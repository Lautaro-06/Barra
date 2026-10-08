# Barra — Backend local (Python)

Backend HTTP local de la app de escritorio. Tiene la lógica de negocio, los hilos de
concurrencia y el envío de emails. **Python es el único dueño del archivo `barra.db` (SQLite):**
la GUI Java nunca lo toca, todo pasa por esta API.

- Escucha solo en `http://127.0.0.1:8000` (no queda expuesto a la red).
- Documentación interactiva automática en `http://127.0.0.1:8000/docs`.

## Cómo correrlo

Requiere Python 3.10 o superior.

```bash
python -m venv venv
source venv/bin/activate        # en Windows: venv\Scripts\activate
pip install -r requirements.txt
python run_backend.py
```

`run_backend.py` es el mismo punto de entrada que usa el `.exe`. Si no existe la variable
`BARRA_SECRET_KEY`, genera la clave de cifrado en `barra_secret.key` y la reutiliza.

**Con recarga automática** (mientras se programa) hay que definir la clave a mano, porque
uvicorn no pasa por `run_backend.py`:

```bash
python -c "from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())"
export BARRA_SECRET_KEY="<la clave>"           # Windows PowerShell: $env:BARRA_SECRET_KEY = "<la clave>"
uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

Sin `BARRA_SECRET_KEY`, guardar o leer la contraseña SMTP responde 503.

Para generar el ejecutable: [compilación y empaquetado](../../docs/tecnico/compilacion-y-empaquetado.md).

## Archivos que crea

Junto a `run_backend.py` (o junto al `.exe`):

| Archivo | Qué es |
|---|---|
| `barra.db` | La base SQLite. Se crea con datos de ejemplo (3 productos, 6 mesas) y se migra sola al agregar columnas |
| `barra_secret.key` | Clave Fernet para la contraseña SMTP (solo si no se usa `BARRA_SECRET_KEY`) |
| `backups/` | Copias `barra_backup_AAAAMMDD_HHMMSS.db` y el registro `backups.log` |

**Restaurar un backup:** detener el backend, renombrar `barra.db`, copiar el backup elegido como
`barra.db` y volver a arrancar.

## Estructura

| Archivo | Responsabilidad |
|---|---|
| `app/main.py` | Endpoints HTTP |
| `app/models.py` | Contrato JSON con la GUI (Pydantic) |
| `app/database.py` | Conexión única, `write_lock`, esquema y migraciones |
| `app/concurrency.py` | Pool de pedidos e hilos de stock, backup y resumen |
| `app/mailer.py` | Envío de emails por SMTP (STARTTLS o SSL) |
| `app/resumen.py` | Cálculo y texto del resumen de ventas |
| `app/secrets.py` | Cifrado Fernet de la contraseña SMTP |
| `run_backend.py` | Punto de entrada del ejecutable |

## Endpoints

| Método | Ruta | Para qué |
|---|---|---|
| GET | `/health` | La GUI comprueba que el backend está vivo |
| GET | `/productos` | Catálogo |
| POST | `/productos` | Alta de producto (`nombre`, `precio` > 0, `stock`, `disponible`, `umbral_stock`) |
| PATCH | `/productos/{id}` | Edición parcial. `umbral_stock: null` = usar el umbral global |
| GET | `/pedidos` | Todos los pedidos, el más nuevo primero |
| POST | `/pedidos` | Pedido de mostrador (sin mesa). Se procesa en el pool de hilos |
| PATCH | `/pedidos/{id}/estado` | `en_preparacion`, `listo` o `entregado` |
| GET | `/mesas` | Mesas con su estado y el total de la cuenta abierta |
| POST | `/mesas` | Alta de mesa |
| DELETE | `/mesas/{id}` | Baja lógica de mesa (solo libre; conserva sus cuentas cerradas en el historial) |
| POST | `/mesas/{id}/abrir` | Abre la cuenta de la mesa |
| GET | `/mesas/{id}/cuenta` | Cuenta abierta con todas sus rondas |
| POST | `/mesas/{id}/pedidos` | Suma una ronda a la cuenta abierta |
| POST | `/mesas/{id}/cerrar` | Cierra la cuenta, libera la mesa y devuelve el ticket |
| GET | `/configuracion` | Nombre del local, umbral global, email, resumen diario |
| POST, PUT | `/configuracion` | Actualización parcial (ver validación abajo) |
| POST | `/configuracion/probar-email` | Manda un email de prueba con la configuración guardada |
| GET | `/admin` | Datos del dueño |
| PUT | `/admin` | Guarda nombre, email y teléfono del dueño |
| GET | `/alertas` | Productos con stock bajo según el último chequeo |
| GET | `/resumen-diario?horas=24` | Resumen de ventas de las últimas N horas (1 a 744), sin mandarlo |
| POST | `/resumen-diario/enviar` | Manda ya el resumen de las últimas 24 h |

**Reglas de negocio:**

- `producto.disponible` es independiente del stock: sirve para pausar un producto sin perder el
  conteo («hoy no hay pescado»). Un pedido a un producto no disponible, o sin stock suficiente,
  se rechaza con 400 y no se registra nada.
- El stock se valida y se descuenta dentro de `write_lock`: dos pedidos simultáneos nunca venden la misma unidad.

## Configuración de email (validación)

`POST/PUT /configuracion` acepta actualizaciones parciales (solo se pisa lo que venga en el body),
pero valida el estado **final** después de combinarlo con lo guardado. Si `email_habilitado` o
`resumen_diario_habilitado` quedan en `true`, tienen que estar completos `smtp_host`,
`smtp_usuario`, `smtp_password` (o ya tenerla guardada) y un destino: `email_destino` o, si está
vacío, el email del dueño (`/admin`). Si falta algo, responde 400 con los campos que faltan.

- La contraseña SMTP se guarda cifrada y nunca se devuelve: la API solo informa `smtp_password_configurada`.
- Puerto 465 → SSL directo. Otro puerto (normalmente 587) → STARTTLS; si el servidor no lo ofrece, no se manda la contraseña.

## Concurrencia (`app/concurrency.py`)

| Hilo | Qué hace |
|---|---|
| `pedido-worker` (×4, `ThreadPoolExecutor`) | Procesa los `POST /pedidos` simultáneos; el resto espera en la cola |
| `stock-watcher` | Cada `BARRA_STOCK_CHECK_INTERVAL` s compara el stock con el umbral efectivo (el del producto o el global), actualiza `GET /alertas` y, si el email está habilitado, manda **un** mail por cada producto que cruza el umbral (`producto.alerta_stock_enviada` evita repetirlo; se rearma al reponer) |
| `db-backup` | Al arrancar y cada `BARRA_BACKUP_INTERVAL_SECONDS` s copia la base con `Connection.backup` y conserva `BARRA_BACKUP_MAX_COPIES` copias |
| `resumen-diario` | Cada `BARRA_RESUMEN_CHECK_INTERVAL` s revisa si llegó `resumen_diario_hora` y, si ese día no se mandó, envía el resumen |

- `write_lock` protege la conexión SQLite compartida; `_alertas_lock` protege la lista de alertas en memoria.
- Los hilos de fondo se apagan con un `threading.Event` en el `shutdown` de la app.
- Las conexiones SMTP se hacen fuera de `write_lock`. Si un envío falla, se reintenta a los `BARRA_EMAIL_RETRY_SECONDS` s.

## Variables de entorno

| Variable | Por defecto | Para qué |
|---|---|---|
| `BARRA_SECRET_KEY` | (la genera `run_backend.py`) | Clave Fernet de la contraseña SMTP |
| `BARRA_STOCK_CHECK_INTERVAL` | `30` | Segundos entre chequeos de stock |
| `BARRA_STOCK_MINIMO` | `5` | Umbral de respaldo si no existiera la fila de configuración |
| `BARRA_EMAIL_RETRY_SECONDS` | `300` | Espera antes de reintentar un email fallido |
| `BARRA_RESUMEN_CHECK_INTERVAL` | `30` | Segundos entre chequeos del resumen diario |
| `BARRA_BACKUP_INTERVAL_SECONDS` | `14400` | Intervalo entre backups (4 h) |
| `BARRA_BACKUP_MAX_COPIES` | `5` | Copias de backup que se conservan |
| `BARRA_BACKUP_DIR` | `backups/` junto a `barra.db` | Carpeta de backups |

## Pruebas

La campaña automatizada (34 casos: pedidos, stock, mesas, emails con un SMTP de prueba,
concurrencia, persistencia) está en [`pruebas/`](../../pruebas/README.md):

```bash
pip install -r ../../pruebas/requirements.txt
python ../../pruebas/test_escritorio.py
```

Resultados en el [plan de pruebas](../../docs/proyecto/plan-de-pruebas.md).

## Estado

**Implementado:** todo lo listado arriba, incluido el empaquetado con PyInstaller
(`barra-backend-v1.0.0.exe`).

**Pendiente:**

- Validación de la licencia contra la web de venta (RF20).
- Modificar o cancelar pedidos (RF21) y endpoints de historial (RF22).
