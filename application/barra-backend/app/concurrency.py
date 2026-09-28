"""
concurrency.py

Infraestructura de concurrencia del backend: acá viven el pool de hilos
para pedidos (punto 1) y el hilo de vigilancia de stock (punto 2).

Punto 1 - Pool de hilos real para procesar pedidos concurrentes
-----------------------------------------------------------------
FastAPI ya corre cada función sync en un threadpool propio (vía anyio),
pero eso queda oculto y sin control nuestro. Acá creamos y administramos
explícitamente un concurrent.futures.ThreadPoolExecutor: los pedidos que
llegan se delegan a este pool, con una cantidad fija de workers ("mozos"
de la barra). Si llegan más pedidos que mozos disponibles, se encolan
solos -> ThreadPoolExecutor maneja la cola internamente, no hace falta
programarla a mano.

La sección crítica (leer/descontar stock) sigue protegida por write_lock
en database.py, así que aunque varios workers procesen pedidos "al mismo
tiempo", el acceso a barra.db sigue siendo seguro.

Punto 2 - Hilo de vigilancia de stock
-----------------------------------------------------------------
Un threading.Thread daemon aparte, independiente del pool de arriba, que
cada INTERVALO_VIGILANCIA_SEGUNDOS recorre la tabla producto.

Umbral efectivo: cada producto puede tener su propio producto.umbral_stock
(nullable). Si lo tiene, se usa ese; si no, se usa configuracion.umbral_stock_global.
STOCK_MINIMO (variable de entorno) queda solo como último respaldo, por si
la fila de configuracion no existiera todavía por algún motivo.

Detección de cruce real: se guarda en memoria (_ultimo_estado_bajo) si cada
producto estaba por debajo del umbral en el chequeo anterior. Solo se
LOGUEA (warning) el momento exacto en que un producto pasa de "ok" a
"bajo" - no en cada chequeo mientras se mantiene bajo. 

Email de alerta: si configuracion.email_habilitado está activo, se manda
UN email al dueño por cada producto que queda bajo el umbral, no uno cada
INTERVALO_VIGILANCIA_SEGUNDOS mientras no se repone. Para eso se usa la
columna producto.alerta_stock_enviada (en la base, no en memoria, así un
reinicio del backend no vuelve a mandar las alertas ya enviadas):
  - bajo y alerta_stock_enviada = 0  -> se incluye en el email; si el
    envío sale bien se marca en 1.
  - ya no está bajo y estaba en 1    -> se vuelve a 0 (se repuso), así la
    próxima vez que baje se avisa de nuevo.
Si en el mismo chequeo bajan varios productos, van todos en un solo email.
Si el envío falla (SMTP caído, contraseña mal, etc.) se loguea el error y
se reintenta recién después de REINTENTO_EMAIL_SEGUNDOS, para no martillar
el servidor SMTP ni llenar el log cada 30 segundos. Si el email está
deshabilitado no se marca nada: al habilitarlo, llega un email con todo lo
que esté bajo en ese momento.
La conexión SMTP (que puede tardar segundos) se hace AFUERA de write_lock,
para no frenar los pedidos mientras se habla con el servidor de mail.

El snapshot de GET /alertas, en cambio, siempre refleja el estado actual
completo (todo lo que esté bajo en este momento), no solo los cruces - así
la GUI puede mostrar en cualquier momento "qué está bajo ahora", sin tener
que engancharse justo en el instante del cruce.

Usa la MISMA conexión SQLite compartida que el resto del backend
(get_connection() en database.py) -> por eso, aunque el hilo solo lee,
también toma write_lock antes de consultar: no es seguro que dos hilos
usen a la vez un mismo objeto sqlite3.Connection sin coordinarse, así
que ese lock en la práctica protege todo acceso concurrente a la
conexión, no solo las escrituras.

En vez de time.sleep(INTERVALO_VIGILANCIA_SEGUNDOS) entre chequeo y
chequeo (lo que dejaría al hilo "sordo" ante un pedido de apagado hasta
que termine de dormir), se usa threading.Event().wait(timeout=...): al
apagar la app se llama a stop_stock_watcher(), que hace event.set() y
corta la espera al instante.

Además del log, cada chequeo deja un snapshot de las alertas activas en
_alertas_activas (una lista en memoria del proceso, no en la base), que
GET /alertas expone tal cual. Es un snapshot, no un historial: si un
producto se repone, desaparece de la lista en el chequeo siguiente. Esa
lista tiene su propio lock (_alertas_lock), separado de write_lock,
porque protege una estructura de Python en memoria, no la conexión
SQLite -> así una request a GET /alertas nunca compite por el lock que
usan los pedidos.

Punto 3 - Hilo de backup automático de barra.db
-----------------------------------------------------------------
Otro threading.Thread daemon, con el mismo patrón de Event que los
anteriores. Cada BACKUP_INTERVAL_SEGUNDOS (default 4 horas) hace una
copia de barra.db a la carpeta backups/, sin depender de red: todo
local, con el propio filesystem del server.

La copia NO se hace con un shutil.copy del archivo crudo. Se usa la API
de backup nativa de sqlite3 (Connection.backup(destino)), que es la
forma correcta de copiar una base que puede estar en uso: SQLite arma
la copia página por página de forma consistente, en vez de arriesgarse
a fotografiar el archivo a mitad de una escritura (lo que podría dejar
una copia corrupta). Aun así, se toma write_lock durante la copia,
mismo criterio que el resto: es la misma conexión compartida, y solo un
hilo a la vez puede usarla.

Cada backup deja dos cosas en backups/:
  - barra_backup_AAAAMMDD_HHMMSS.db  -> copia real y restaurable.
  - backups.log                      -> una línea de texto por evento
    (fecha, archivo generado, resultado). Nunca se rota ni se borra.

Después de cada backup exitoso se aplica retención: si hay más de
BACKUP_MAX_COPIAS archivos barra_backup_*.db, se borran los más viejos
(el nombre ya ordena cronológicamente).
"""

import logging
import os
import sqlite3
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
from pathlib import Path

from .database import DB_PATH, get_connection, write_lock
from .mailer import EmailError, armar_alerta_stock, enviar_email, leer_config_email
logger = logging.getLogger("barra.concurrency")

# ---------- Punto 1: pool de hilos para pedidos ----------

# Cantidad de pedidos que se pueden procesar en simultáneo.
MAX_WORKERS = 4

pedido_executor = ThreadPoolExecutor( # Crea un pool con 4 hilos fijos (mozos)
    max_workers=MAX_WORKERS,
    thread_name_prefix="pedido-worker",
)


def shutdown_executor(wait: bool = True) -> None:
    """Se llama al apagar la app (evento shutdown de FastAPI) para que los
    hilos del pool no queden colgados."""
    pedido_executor.shutdown(wait=wait)


# ---------- Punto 2: hilo de vigilancia de stock ----------

# STOCK_MINIMO ahora es solo un respaldo (ver arriba): el umbral real es
# producto.umbral_stock si existe, sino configuracion.umbral_stock_global.
STOCK_MINIMO = int(os.environ.get("BARRA_STOCK_MINIMO", "5"))
INTERVALO_VIGILANCIA_SEGUNDOS = int(
    os.environ.get("BARRA_STOCK_CHECK_INTERVAL", "30")
)
# Después de un envío fallido, cuánto esperar antes de volver a intentar.
REINTENTO_EMAIL_SEGUNDOS = int(os.environ.get("BARRA_EMAIL_RETRY_SECONDS", "300"))

_stock_watch_stop_event = threading.Event()
_stock_watch_thread: threading.Thread | None = None

# Snapshot de las alertas activas según el último chequeo, para GET
# /alertas. Lock propio: es una lista en memoria, no la conexión SQLite.
_alertas_lock = threading.Lock()
_alertas_activas: list[dict] = []

# producto_id -> bool: si estaba por debajo del umbral efectivo en el
# chequeo anterior. Se usa solo para loguear en el momento del cruce real,
# no en cada chequeo mientras el producto sigue bajo. (Los emails usan
# producto.alerta_stock_enviada, que sobrevive a un reinicio.)
_ultimo_estado_bajo: dict[int, bool] = {}

# time.monotonic() a partir del cual se puede volver a intentar mandar un
# email de alerta después de un fallo. Solo lo toca el hilo stock-watcher.
_proximo_intento_email = 0.0


def get_alertas_stock() -> list[dict]:
    """Devuelve una copia del snapshot actual de alertas de stock bajo.
    Se llama desde el endpoint GET /alertas."""
    with _alertas_lock:
        return list(_alertas_activas)


def _chequear_stock() -> None:
    """Un chequeo completo: actualiza el snapshot de GET /alertas, loguea
    los cruces de umbral y manda el email de alerta si corresponde."""
    global _proximo_intento_email

    conn = get_connection()
    config_email = None
    pendientes_email: list[dict] = []

    with write_lock:
        rows = conn.execute(
            """
            SELECT p.id, p.nombre, p.stock, p.umbral_stock, p.alerta_stock_enviada,
                   c.umbral_stock_global AS umbral_global,
                   c.email_habilitado AS email_habilitado
            FROM producto p
            LEFT JOIN configuracion c ON c.id = 1
            """
        ).fetchall()

        nuevas_alertas = []
        repuestos_ids = []
        email_habilitado = False
        for row in rows:
            email_habilitado = bool(row["email_habilitado"])
            umbral_global = row["umbral_global"] if row["umbral_global"] is not None else STOCK_MINIMO
            umbral_efectivo = row["umbral_stock"] if row["umbral_stock"] is not None else umbral_global
            esta_bajo = row["stock"] < umbral_efectivo
            estaba_bajo = _ultimo_estado_bajo.get(row["id"], False)

            if esta_bajo:
                alerta = {
                    "producto_id": row["id"],
                    "nombre": row["nombre"],
                    "stock": row["stock"],
                    "umbral": umbral_efectivo,
                }
                nuevas_alertas.append(alerta)
                if not row["alerta_stock_enviada"]:
                    pendientes_email.append(alerta)
                if not estaba_bajo:
                    # Cruce real (recién ahora quedó por debajo): único
                    # momento en que se loguea, no en cada chequeo
                    # mientras sigue bajo.
                    logger.warning(
                        "Stock bajo: '%s' cruzó el umbral (%s unidades, umbral: %s)",
                        row["nombre"],
                        row["stock"],
                        umbral_efectivo,
                    )
            else:
                if row["alerta_stock_enviada"]:
                    repuestos_ids.append(row["id"])
                if estaba_bajo:
                    # Cruce inverso: se repuso. También se loguea una sola vez.
                    logger.info(
                        "Stock recuperado: '%s' volvió a estar por encima del umbral (%s unidades, umbral: %s)",
                        row["nombre"],
                        row["stock"],
                        umbral_efectivo,
                    )

            _ultimo_estado_bajo[row["id"]] = esta_bajo

        if repuestos_ids:
            # Se repusieron: la próxima vez que bajen se vuelve a avisar.
            conn.executemany(
                "UPDATE producto SET alerta_stock_enviada = 0 WHERE id = ?",
                [(pid,) for pid in repuestos_ids],
            )
            conn.commit()

        if pendientes_email and email_habilitado and time.monotonic() >= _proximo_intento_email:
            try:
                config_email = leer_config_email(conn)
            except EmailError as exc:
                logger.error("No se puede mandar la alerta de stock por email: %s", exc)
                _proximo_intento_email = time.monotonic() + REINTENTO_EMAIL_SEGUNDOS

    with _alertas_lock:
        _alertas_activas[:] = nuevas_alertas

    if config_email is None:
        return

    # Afuera de write_lock: hablar con el servidor SMTP puede tardar.
    asunto, cuerpo = armar_alerta_stock(config_email.nombre_local, pendientes_email)
    try:
        enviar_email(config_email, asunto, cuerpo)
    except EmailError as exc:
        logger.error(
            "Falló el email de alerta de stock (se reintenta en %ss): %s",
            REINTENTO_EMAIL_SEGUNDOS,
            exc,
        )
        _proximo_intento_email = time.monotonic() + REINTENTO_EMAIL_SEGUNDOS
        return

    _proximo_intento_email = 0.0
    with write_lock:
        conn.executemany(
            "UPDATE producto SET alerta_stock_enviada = 1 WHERE id = ?",
            [(p["producto_id"],) for p in pendientes_email],
        )
        conn.commit()


def _vigilar_stock() -> None:
    logger.info(
        "Hilo de vigilancia de stock arrancado (intervalo=%ss, umbral de respaldo=%s)",
        INTERVALO_VIGILANCIA_SEGUNDOS,
        STOCK_MINIMO,
    )
    while not _stock_watch_stop_event.is_set():
        try:
            _chequear_stock()
        except Exception:
            # Un error inesperado no tiene que matar el hilo: se loguea y
            # se sigue vigilando en el próximo intervalo.
            logger.exception("Error inesperado en el chequeo de stock")

        # Espera interrumpible: si stop_stock_watcher() llama a .set(),
        # esta espera corta ahí mismo en vez de completar el intervalo.
        _stock_watch_stop_event.wait(timeout=INTERVALO_VIGILANCIA_SEGUNDOS)


def start_stock_watcher() -> None:
    """Arranca el hilo de vigilancia. Se llama en el startup de la app."""
    global _stock_watch_thread, _proximo_intento_email
    _stock_watch_stop_event.clear()
    _ultimo_estado_bajo.clear()
    _proximo_intento_email = 0.0
    _stock_watch_thread = threading.Thread(
        target=_vigilar_stock, name="stock-watcher", daemon=True
    )
    _stock_watch_thread.start()


def stop_stock_watcher() -> None:
    """Corta el hilo de vigilancia prolijamente. Se llama en el shutdown."""
    _stock_watch_stop_event.set()
    if _stock_watch_thread is not None:
        _stock_watch_thread.join(timeout=5)


# ---------- Punto 3: hilo de backup automático de barra.db ----------

BACKUP_DIR = Path(os.environ.get("BARRA_BACKUP_DIR", DB_PATH.parent / "backups"))
BACKUP_INTERVAL_SEGUNDOS = int(
    os.environ.get("BARRA_BACKUP_INTERVAL_SECONDS", str(4 * 60 * 60))  # 4 horas
)
BACKUP_MAX_COPIAS = int(os.environ.get("BARRA_BACKUP_MAX_COPIES", "5"))

BACKUP_LOG_PATH = BACKUP_DIR / "backups.log"
_BACKUP_FILENAME_FMT = "barra_backup_%Y%m%d_%H%M%S.db"

_backup_stop_event = threading.Event()
_backup_thread: threading.Thread | None = None


def _registrar_evento_backup(mensaje: str) -> None:
    """Agrega una línea a backups/backups.log. Nunca se rota ni se borra:
    es el historial de texto de la actividad del hilo, separado de las
    copias .db en sí."""
    timestamp = datetime.now().isoformat(timespec="seconds")
    with open(BACKUP_LOG_PATH, "a", encoding="utf-8") as f:
        f.write(f"{timestamp} | {mensaje}\n")


def _aplicar_retencion() -> None:
    """Si hay más de BACKUP_MAX_COPIAS archivos de backup, borra los más
    viejos. El nombre timestamped ya ordena cronológicamente."""
    backups = sorted(BACKUP_DIR.glob("barra_backup_*.db"))
    de_mas = max(0, len(backups) - BACKUP_MAX_COPIAS)
    for viejo in backups[:de_mas]:
        viejo.unlink()
        logger.info("Backup viejo eliminado por retención: %s", viejo.name)


def _hacer_backup() -> None:
    BACKUP_DIR.mkdir(parents=True, exist_ok=True)
    nombre = datetime.now().strftime(_BACKUP_FILENAME_FMT)
    destino = BACKUP_DIR / nombre

    try:
        # API de backup nativa de sqlite3: copia página por página de
        # forma consistente, a diferencia de copiar el archivo crudo
        # mientras puede estar siendo escrito.
        conn = get_connection()
        with write_lock:
            destino_conn = sqlite3.connect(destino)
            try:
                conn.backup(destino_conn)
            finally:
                destino_conn.close()
    except Exception as exc:
        logger.error("Falló el backup de barra.db: %s", exc)
        _registrar_evento_backup(f"ERROR | {exc}")
        return

    tamano = destino.stat().st_size
    logger.info("Backup de barra.db creado: %s (%s bytes)", nombre, tamano)
    _registrar_evento_backup(f"OK | {nombre} | {tamano} bytes")
    _aplicar_retencion()


def _vigilar_backup() -> None:
    logger.info(
        "Hilo de backup arrancado (intervalo=%ss, retención=%s copias, dir=%s)",
        BACKUP_INTERVAL_SEGUNDOS,
        BACKUP_MAX_COPIAS,
        BACKUP_DIR,
    )
    while not _backup_stop_event.is_set():
        _hacer_backup()
        # Espera interrumpible, mismo criterio que el hilo de stock.
        _backup_stop_event.wait(timeout=BACKUP_INTERVAL_SEGUNDOS)


def start_backup_thread() -> None:
    """Arranca el hilo de backup. Se llama en el startup de la app. Hace
    un backup inmediato al arrancar, sin esperar el primer intervalo."""
    global _backup_thread
    _backup_stop_event.clear()
    _backup_thread = threading.Thread(
        target=_vigilar_backup, name="db-backup", daemon=True
    )
    _backup_thread.start()


def stop_backup_thread() -> None:
    """Corta el hilo de backup prolijamente. Se llama en el shutdown."""
    _backup_stop_event.set()
    if _backup_thread is not None:
        _backup_thread.join(timeout=5)