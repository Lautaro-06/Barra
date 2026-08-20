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

## Endpoints (probados y funcionando)

- GET  /health
- GET  /productos
- POST /productos
- GET  /pedidos
- POST /pedidos              (descuenta stock, calcula total)
- PATCH /pedidos/{id}/estado

## Qué falta (próximos puntos del proyecto)

- Punto 3: pool de hilos real para pedidos concurrentes + hilo de alertas
  de stock + hilo de backup. Hoy `write_lock` ya aísla la sección crítica,
  pero el procesamiento sigue siendo secuencial (uvicorn en un solo worker).
- Persistir configuración de mail/Telegram del dueño.
- Empaquetado con PyInstaller (punto 1, más adelante).
