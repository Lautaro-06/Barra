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
- GET  /configuracion
- PUT  /configuracion        (nombre del local - panel de Admin)
- GET  /productos
- POST /productos
- PATCH /productos/{id}      (nombre/precio/stock/disponible - panel de Admin)
- GET  /pedidos
- POST /pedidos              (pedido de mostrador/para llevar, sin mesa)
- PATCH /pedidos/{id}/estado
- GET  /mesas
- POST /mesas                (alta de mesa - panel de Admin)
- DELETE /mesas/{id}         (solo si está libre - panel de Admin)
- POST /mesas/{id}/abrir     (abre la cuenta de la mesa)
- GET  /mesas/{id}/cuenta    (cuenta abierta con todas las rondas)
- POST /mesas/{id}/pedidos   (suma una ronda a la cuenta abierta de la mesa)
- POST /mesas/{id}/cerrar    (cierra la cuenta, la mesa vuelve a libre, devuelve el ticket)

`producto.disponible` es independiente del stock: sirve para pausar un
producto sin perder el conteo (ej. "hoy no hay pescado" aunque quede stock
cargado). Un pedido a un producto no disponible, o sin suficiente stock,
se rechaza con 400.

## Qué falta (próximos puntos del proyecto)

- Punto 3: pool de hilos real para pedidos concurrentes + hilo de alertas
  de stock + hilo de backup. Hoy `write_lock` ya aísla la sección crítica,
  pero el procesamiento sigue siendo secuencial (uvicorn en un solo worker).
- Persistir configuración de mail/Telegram del dueño.
- Empaquetado con PyInstaller (punto 1, más adelante).
