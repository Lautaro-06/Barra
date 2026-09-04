"""
main.py

Punto clave de la arquitectura: la GUI Java le habla a este backend Python
por HTTP en localhost, como un mozo que pasa el pedido por una ventanita y
espera el plato listo.

Correr con:
    uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload

La GUI Java (ver ApiClient.java) apunta a http://127.0.0.1:8000
"""

import asyncio
from datetime import datetime
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from .concurrency import pedido_executor, shutdown_executor

from .database import get_connection, init_db, write_lock
from .models import (
    ProductoIn,
    ProductoOut,
    PedidoIn,
    PedidoOut,
    DetalleOut,
    EstadoIn,
)

app = FastAPI(title="Barra - Backend de Pedidos")

# CORS abierto solo para desarrollo local: la GUI Java corre embebida
# (jpackage) y le habla siempre a localhost, no hay exposición a internet.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
def on_startup():
    init_db()


@app.on_event("shutdown")
def on_shutdown():
    shutdown_executor()

@app.get("/health")
def health():
    """La GUI Java llama esto al arrancar para confirmar que el backend
    (que ella misma o el instalador ya debería tener corriendo) está vivo."""
    return {"status": "ok"}


# ---------- Productos (catálogo) ----------

@app.get("/productos", response_model=list[ProductoOut])
def listar_productos():
    conn = get_connection()
    rows = conn.execute("SELECT * FROM producto ORDER BY nombre").fetchall()
    return [dict(r) for r in rows]


@app.post("/productos", response_model=ProductoOut, status_code=201)
def crear_producto(producto: ProductoIn):
    conn = get_connection()
    with write_lock:
        cur = conn.execute(
            "INSERT INTO producto (nombre, precio, stock) VALUES (?, ?, ?)",
            (producto.nombre, producto.precio, producto.stock),
        )
        conn.commit()
        nuevo_id = cur.lastrowid
    row = conn.execute("SELECT * FROM producto WHERE id = ?", (nuevo_id,)).fetchone()
    return dict(row)


# ---------- Pedidos ----------

def _pedido_a_dict(conn, pedido_row) -> dict:
    detalles_rows = conn.execute(
        """
        SELECT dp.producto_id, p.nombre AS nombre_producto, dp.cantidad,
               (dp.cantidad * p.precio) AS subtotal
        FROM detalle_pedido dp
        JOIN producto p ON p.id = dp.producto_id
        WHERE dp.pedido_id = ?
        """,
        (pedido_row["id"],),
    ).fetchall()
    return {
        "id": pedido_row["id"],
        "fecha": pedido_row["fecha"],
        "estado": pedido_row["estado"],
        "total": pedido_row["total"],
        "nota": pedido_row["nota"],
        "detalles": [dict(d) for d in detalles_rows],
    }


@app.get("/pedidos", response_model=list[PedidoOut])
def listar_pedidos():
    conn = get_connection()
    rows = conn.execute("SELECT * FROM pedido ORDER BY id DESC").fetchall()
    return [_pedido_a_dict(conn, r) for r in rows]


def _procesar_pedido(pedido: PedidoIn) -> dict:
    """
    Registra un pedido y descuenta stock. Corre dentro de un worker del
    pedido_executor (punto 1: pool de hilos real), nunca en el hilo del
    event loop de FastAPI. La sección crítica sigue protegida por
    write_lock: varios workers pueden estar procesando pedidos distintos
    al mismo tiempo, pero solo uno a la vez toca la base.
    """
    conn = get_connection()

    with write_lock:
        # 1. Validar stock disponible de todos los productos primero
        total = 0.0
        productos_cache = {}
        for det in pedido.detalles:
            row = conn.execute(
                "SELECT * FROM producto WHERE id = ?", (det.producto_id,)
            ).fetchone()
            if row is None:
                raise HTTPException(404, f"Producto {det.producto_id} no existe")
            if row["stock"] < det.cantidad:
                raise HTTPException(
                    400,
                    f"Stock insuficiente para '{row['nombre']}' "
                    f"(pedido: {det.cantidad}, stock: {row['stock']})",
                )
            productos_cache[det.producto_id] = row
            total += row["precio"] * det.cantidad

        # 2. Crear el pedido
        cur = conn.execute(
            "INSERT INTO pedido (fecha, estado, total, nota) VALUES (?, ?, ?, ?)",
            (datetime.now().isoformat(timespec="seconds"), "en_preparacion", total, pedido.nota),
        )
        pedido_id = cur.lastrowid

        # 3. Insertar detalle y descontar stock
        for det in pedido.detalles:
            conn.execute(
                "INSERT INTO detalle_pedido (pedido_id, producto_id, cantidad) VALUES (?, ?, ?)",
                (pedido_id, det.producto_id, det.cantidad),
            )
            conn.execute(
                "UPDATE producto SET stock = stock - ? WHERE id = ?",
                (det.cantidad, det.producto_id),
            )

        conn.commit()

    row = conn.execute("SELECT * FROM pedido WHERE id = ?", (pedido_id,)).fetchone()
    return _pedido_a_dict(conn, row)

@app.post("/pedidos", response_model=PedidoOut, status_code=201)
async def crear_pedido(pedido: PedidoIn):
    """
    Punto de entrada HTTP. No procesa nada acá: delega el trabajo al
    pedido_executor (ThreadPoolExecutor real, ver concurrency.py) y espera
    el resultado sin bloquear el event loop. Si llegan varios pedidos a
    la vez, cada uno se ejecuta en un worker distinto del pool (hasta
    MAX_WORKERS en simultáneo); el resto espera en la cola interna del
    executor.
    """
    loop = asyncio.get_running_loop()
    return await loop.run_in_executor(pedido_executor, _procesar_pedido, pedido)

@app.patch("/pedidos/{pedido_id}/estado", response_model=PedidoOut)
def cambiar_estado(pedido_id: int, body: EstadoIn):
    if body.estado not in ("en_preparacion", "listo", "entregado"):
        raise HTTPException(400, "Estado inválido")

    conn = get_connection()
    with write_lock:
        row = conn.execute("SELECT * FROM pedido WHERE id = ?", (pedido_id,)).fetchone()
        if row is None:
            raise HTTPException(404, "Pedido no encontrado")
        conn.execute("UPDATE pedido SET estado = ? WHERE id = ?", (body.estado, pedido_id))
        conn.commit()

    row = conn.execute("SELECT * FROM pedido WHERE id = ?", (pedido_id,)).fetchone()
    return _pedido_a_dict(conn, row)