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

## Endpoints (probando)

- GET  /health
- GET  /alertas              (snapshot de productos con stock bajo)
- GET  /productos
- POST /productos
- GET  /pedidos
- POST /pedidos              (descuenta stock, calcula total)
- PATCH /pedidos/{id}/estado

## Concurrencia (rama app/concurrence)

- [x] Pool de hilos real (`concurrent.futures.ThreadPoolExecutor`, ver
  `app/concurrency.py`) para procesar pedidos concurrentes: `POST /pedidos`
  delega el trabajo al pool en vez de correr secuencialmente en el hilo del
  request. `write_lock` sigue protegiendo la sección crítica de stock.
- [x] Hilo separado de vigilancia de stock (`app/concurrency.py`): cada
  `BARRA_STOCK_CHECK_INTERVAL` segundos (default 30) recorre `producto` y
  loguea un warning por cada uno con `stock < BARRA_STOCK_MINIMO` (default
  5). Corre como `threading.Thread` daemon, arranca en el `startup` de la
  app y se apaga prolijamente en el `shutdown`. El snapshot del último
  chequeo queda expuesto en `GET /alertas` (lista en memoria, con su
  propio lock, separado de `write_lock`).
- [x] Hilo de backup automático de `barra.db` (`app/concurrency.py`): cada
  `BARRA_BACKUP_INTERVAL_SECONDS` segundos (default 14400 = 4hs) copia la
  base a `backups/` usando la API de backup nativa de `sqlite3`
  (`Connection.backup`), sin depender de red. Cada copia genera un archivo
  `barra_backup_AAAAMMDD_HHMMSS.db` y una línea en `backups/backups.log`.
  Retiene como máximo `BARRA_BACKUP_MAX_COPIES` copias (default 5),
  borrando las más viejas. La carpeta `backups/` se crea sola y está en
  `.gitignore` (son archivos de runtime, no código fuente).

## Qué falta (próximos puntos del proyecto)

- Persistir configuración de mail/Telegram del dueño.
- Empaquetado con PyInstaller (punto 1, más adelante).