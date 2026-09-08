"""
concurrency.py

Infraestructura de concurrencia del backend: acá viven el pool de hilos
para pedidos (punto 1) y el hilo de vigilancia de stock (punto 2).

Punto 1 - Pool de hilos real para procesar pedidos concurrentes
-----------------------------------------------------------------
FastAPI ya corre cada función sync en un threadpool propio (vía anyio),
pero eso queda oculto y sin control nuestro. Acá creamos y administramos
explícitamente un concurrent.futures.ThreadPoolExecutor: los pedidos que
La sección crítica (leer/descontar stock) sigue protegida por write_lock
en database.py, así que aunque varios workers procesen pedidos "al mismo
tiempo", el acceso a barra.db sigue siendo seguro.

Punto 2 - Hilo de vigilancia de stock
-----------------------------------------------------------------
Un threading.Thread daemon aparte, independiente del pool de arriba, que
cada INTERVALO_VIGILANCIA_SEGUNDOS recorre la tabla producto y loguea un
warning por cada producto cuyo stock esté por debajo de STOCK_MINIMO.

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
"""

import logging
import os
import threading
from concurrent.futures import ThreadPoolExecutor

from .database import get_connection, write_lock

logger = logging.getLogger("barra.concurrency")

# ---------- Punto 1: pool de hilos para pedidos ----------

# Cantidad de pedidos que se pueden procesar en simultáneo.
MAX_WORKERS = 4

"""
Se llama al apagar la app (evento shutdown de FastAPI) para que los
hilos del pool no queden colgados.
"""

pedido_executor = ThreadPoolExecutor( # Crea un pool con 4 hilos fijos (mozos)
    max_workers=MAX_WORKERS,
    thread_name_prefix="pedido-worker",
)


def shutdown_executor(wait: bool = True) -> None:
    """Se llama al apagar la app (evento shutdown de FastAPI) para que los
    hilos del pool no queden colgados."""
    pedido_executor.shutdown(wait=wait)


# ---------- Punto 2: hilo de vigilancia de stock ----------

# Umbral y frecuencia configurables por variable de entorno, sin tocar
# código ni el esquema de la tabla producto (umbral global, no por ítem).
STOCK_MINIMO = int(os.environ.get("BARRA_STOCK_MINIMO", "5"))
INTERVALO_VIGILANCIA_SEGUNDOS = int(
    os.environ.get("BARRA_STOCK_CHECK_INTERVAL", "30")
)

_stock_watch_stop_event = threading.Event()
_stock_watch_thread: threading.Thread | None = None


def _vigilar_stock() -> None:
    logger.info(
        "Hilo de vigilancia de stock arrancado (umbral=%s, intervalo=%ss)",
        STOCK_MINIMO,
        INTERVALO_VIGILANCIA_SEGUNDOS,
    )
    while not _stock_watch_stop_event.is_set():
        conn = get_connection()
        with write_lock:
            rows = conn.execute(
                "SELECT nombre, stock FROM producto WHERE stock < ? ORDER BY stock",
                (STOCK_MINIMO,),
            ).fetchall()

        for row in rows:
            logger.warning(
                "Stock bajo: '%s' tiene %s unidades (umbral: %s)",
                row["nombre"],
                row["stock"],
                STOCK_MINIMO,
            )

        # Espera interrumpible: si stop_stock_watcher() llama a .set(),
        # esta espera corta ahí mismo en vez de completar el intervalo.
        _stock_watch_stop_event.wait(timeout=INTERVALO_VIGILANCIA_SEGUNDOS)


def start_stock_watcher() -> None:
    """Arranca el hilo de vigilancia. Se llama en el startup de la app."""
    global _stock_watch_thread
    _stock_watch_stop_event.clear()
    _stock_watch_thread = threading.Thread(
        target=_vigilar_stock, name="stock-watcher", daemon=True
    )
    _stock_watch_thread.start()


def stop_stock_watcher() -> None:
    """Corta el hilo de vigilancia prolijamente. Se llama en el shutdown."""
    _stock_watch_stop_event.set()
    if _stock_watch_thread is not None:
        _stock_watch_thread.join(timeout=5)