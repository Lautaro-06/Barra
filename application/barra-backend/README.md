# Barra - Backend Python

Backend HTTP local. Python es el único dueño del archivo `barra.db` (SQLite).

## Cómo correrlo (Visual Studio Code o terminal)

```bash
python3 -m venv venv
source venv/bin/activate        # en Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

Queda escuchando en http://127.0.0.1:8000
Documentación interactiva automática en http://127.0.0.1:8000/docs

Las credenciales SMTP se guardan cifradas en SQLite. Para eso hace falta
la variable de entorno `BARRA_SECRET_KEY` (una clave Fernet válida) antes
de arrancar el servidor - sin ella, guardar o leer la contraseña SMTP
responde 503.

## Endpoints

- GET  /health
- GET  /configuracion
- POST /configuracion, PUT /configuracion   (nombre del local, umbral global,
  email/SMTP, resumen diario - panel de Admin. Ver validación más abajo)
- GET  /admin
- PUT  /admin                 (nombre, email y teléfono del dueño - panel de Admin)
- GET  /alertas                (snapshot de productos con stock bajo)
- GET  /productos
- POST /productos
- PATCH /productos/{id}       (nombre/precio/stock/disponible - panel de Admin)
- GET  /pedidos
- POST /pedidos                (pedido de mostrador/para llevar, sin mesa)
- PATCH /pedidos/{id}/estado
- GET  /mesas
- POST /mesas                  (alta de mesa - panel de Admin)
- DELETE /mesas/{id}           (solo si está libre - panel de Admin)
- POST /mesas/{id}/abrir       (abre la cuenta de la mesa)
- GET  /mesas/{id}/cuenta      (cuenta abierta con todas las rondas)
- POST /mesas/{id}/pedidos     (suma una ronda a la cuenta abierta de la mesa)
- POST /mesas/{id}/cerrar      (cierra la cuenta, la mesa vuelve a libre, devuelve el ticket)

`producto.disponible` es independiente del stock: sirve para pausar un
producto sin perder el conteo (ej. "hoy no hay pescado" aunque quede stock
cargado). Un pedido a un producto no disponible, o sin suficiente stock,
se rechaza con 400.

## Configuración de email (validación)

`POST/PUT /configuracion` acepta actualizaciones parciales (solo se pisa lo
que venga en el body), pero valida el estado **final** después de mergear
con lo que ya había guardado. Si `email_habilitado` o
`resumen_diario_habilitado` quedan en `true`, tienen que estar completos:
`smtp_host`, `smtp_usuario`, `smtp_password` (o ya tenerla guardada de un
request anterior) y `email_destino` con formato de email válido. Si falta
algo, responde 400 con el detalle de qué campos faltan. La contraseña SMTP
nunca se devuelve: la API solo informa `smtp_password_configurada` (bool).

## Concurrencia (`app/concurrency.py`)

- [x] Pool de hilos real (`concurrent.futures.ThreadPoolExecutor`) para
  procesar pedidos concurrentes: `POST /pedidos` delega el trabajo al pool
  en vez de correr secuencialmente en el hilo del request. `write_lock`
  sigue protegiendo la sección crítica de stock.
- [x] Hilo separado de vigilancia de stock: cada `BARRA_STOCK_CHECK_INTERVAL`
  segundos (default 30) recorre `producto` y loguea un warning por cada uno
  con `stock < BARRA_STOCK_MINIMO` (default 5). Corre como `threading.Thread`
  daemon, arranca en el `startup` de la app y se apaga prolijamente en el
  `shutdown`. El snapshot del último chequeo queda expuesto en `GET /alertas`.
- [x] Hilo de backup automático de `barra.db`: cada
  `BARRA_BACKUP_INTERVAL_SECONDS` segundos (default 14400 = 4hs) copia la
  base a `backups/` usando la API de backup nativa de `sqlite3`
  (`Connection.backup`). Retiene como máximo `BARRA_BACKUP_MAX_COPIES`
  copias (default 5), borrando las más viejas.

## Qué falta (próximos puntos del proyecto)

- Umbral de stock por producto (`producto.umbral_stock` ya existe en la
  tabla, falta exponerlo en `ProductoIn/Out/Patch` y usarlo en el watcher
  con prioridad: umbral del producto si existe, sino el global).
- El watcher de stock todavía no distingue el momento exacto en que un
  producto cruza el umbral (hoy solo mira "¿está bajo ahora?" en cada
  chequeo) - falta esa lógica para no repetir la misma alerta sin parar.
- Servicio de envío de emails (usando la config SMTP ya validada), armado
  del resumen diario de ventas, y un scheduler que lo dispare a la hora
  configurada en `resumen_diario_hora`.
- Actualizar la GUI Java: sección de alertas/SMTP/resumen diario en el
  panel de Admin y botón "Probar email".
- Tests de backend y confirmar que el proyecto Java sigue compilando con
  Maven después de estos cambios.
- Empaquetado con PyInstaller.