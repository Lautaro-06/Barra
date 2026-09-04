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
- [ ] Hilo separado de vigilancia de stock (detecta cuándo un producto baja
  de un umbral).
- [ ] Hilo de backup automático de `barra.db` (copia periódica, sin depender
  de red).

## Qué falta (próximos puntos del proyecto)

- Persistir configuración de mail/Telegram del dueño.
- Empaquetado con PyInstaller (punto 1, más adelante).
