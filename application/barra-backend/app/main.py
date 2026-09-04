"""
main.py

Punto clave de la arquitectura: la GUI Java le habla a este backend Python
por HTTP en localhost, como un mozo que pasa el pedido por una ventanita y
espera el plato listo.

Correr con:
    uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload

La GUI Java (ver ApiClient.java) apunta a http://127.0.0.1:8000
"""

from datetime import datetime
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from .database import get_connection, init_db, write_lock
from .models import (
    ProductoIn,
    ProductoOut,
    ProductoPatch,
    PedidoIn,
    PedidoOut,
    DetalleOut,
    EstadoIn,
    MesaIn,
    MesaOut,
    CuentaOut,
    ConfiguracionIn,
    ConfiguracionOut,
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


@app.get("/health")
def health():
    """La GUI Java llama esto al arrancar para confirmar que el backend
    (que ella misma o el instalador ya debería tener corriendo) está vivo."""
    return {"status": "ok"}


# ---------- Configuración (panel de Admin) ----------

@app.get("/configuracion", response_model=ConfiguracionOut)
def obtener_configuracion():
    conn = get_connection()
    row = conn.execute("SELECT * FROM configuracion WHERE id = 1").fetchone()
    return dict(row)


@app.put("/configuracion", response_model=ConfiguracionOut)
def actualizar_configuracion(config: ConfiguracionIn):
    conn = get_connection()
    with write_lock:
        conn.execute("UPDATE configuracion SET nombre_local = ? WHERE id = 1", (config.nombre_local,))
        conn.commit()
    row = conn.execute("SELECT * FROM configuracion WHERE id = 1").fetchone()
    return dict(row)


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
            "INSERT INTO producto (nombre, precio, stock, disponible) VALUES (?, ?, ?, ?)",
            (producto.nombre, producto.precio, producto.stock, int(producto.disponible)),
        )
        conn.commit()
        nuevo_id = cur.lastrowid
    row = conn.execute("SELECT * FROM producto WHERE id = ?", (nuevo_id,)).fetchone()
    return dict(row)


@app.patch("/productos/{producto_id}", response_model=ProductoOut)
def editar_producto(producto_id: int, cambios: ProductoPatch):
    """Alta/edición desde el panel de Admin: nombre, precio, stock y si el
    producto está disponible para vender (independiente del stock - sirve
    para pausar un producto sin perder el conteo, ej. 'hoy no hay pescado')."""
    conn = get_connection()
    with write_lock:
        row = conn.execute("SELECT * FROM producto WHERE id = ?", (producto_id,)).fetchone()
        if row is None:
            raise HTTPException(404, "Producto no encontrado")
        nombre = cambios.nombre if cambios.nombre is not None else row["nombre"]
        precio = cambios.precio if cambios.precio is not None else row["precio"]
        stock = cambios.stock if cambios.stock is not None else row["stock"]
        disponible = cambios.disponible if cambios.disponible is not None else bool(row["disponible"])
        conn.execute(
            "UPDATE producto SET nombre = ?, precio = ?, stock = ?, disponible = ? WHERE id = ?",
            (nombre, precio, stock, int(disponible), producto_id),
        )
        conn.commit()
    row = conn.execute("SELECT * FROM producto WHERE id = ?", (producto_id,)).fetchone()
    return dict(row)


# ---------- Mesas y cuentas (salón) ----------

def _mesa_a_dict(conn, mesa_row) -> dict:
    cuenta_row = conn.execute(
        "SELECT * FROM cuenta WHERE mesa_id = ? AND estado = 'abierta'", (mesa_row["id"],)
    ).fetchone()
    cuenta_id = None
    total_actual = 0.0
    if cuenta_row is not None:
        cuenta_id = cuenta_row["id"]
        total_actual = conn.execute(
            "SELECT COALESCE(SUM(total), 0) AS t FROM pedido WHERE cuenta_id = ?", (cuenta_id,)
        ).fetchone()["t"]
    return {
        "id": mesa_row["id"],
        "nombre": mesa_row["nombre"],
        "estado": mesa_row["estado"],
        "cuenta_id": cuenta_id,
        "total_actual": total_actual,
    }


@app.get("/mesas", response_model=list[MesaOut])
def listar_mesas():
    conn = get_connection()
    rows = conn.execute("SELECT * FROM mesa ORDER BY id").fetchall()
    return [_mesa_a_dict(conn, r) for r in rows]


@app.post("/mesas", response_model=MesaOut, status_code=201)
def crear_mesa(mesa: MesaIn):
    conn = get_connection()
    with write_lock:
        cur = conn.execute("INSERT INTO mesa (nombre, estado) VALUES (?, 'libre')", (mesa.nombre,))
        conn.commit()
        nueva_id = cur.lastrowid
    row = conn.execute("SELECT * FROM mesa WHERE id = ?", (nueva_id,)).fetchone()
    return _mesa_a_dict(conn, row)


@app.delete("/mesas/{mesa_id}", status_code=204)
def eliminar_mesa(mesa_id: int):
    conn = get_connection()
    with write_lock:
        row = conn.execute("SELECT * FROM mesa WHERE id = ?", (mesa_id,)).fetchone()
        if row is None:
            raise HTTPException(404, "Mesa no encontrada")
        if row["estado"] != "libre":
            raise HTTPException(400, "No se puede borrar una mesa con la cuenta abierta")
        conn.execute("DELETE FROM mesa WHERE id = ?", (mesa_id,))
        conn.commit()


@app.post("/mesas/{mesa_id}/abrir", response_model=MesaOut)
def abrir_mesa(mesa_id: int):
    """El mozo toca la mesa libre: se abre una cuenta nueva que va a ir
    acumulando pedidos hasta que se cierre (ver /mesas/{id}/cerrar)."""
    conn = get_connection()
    with write_lock:
        row = conn.execute("SELECT * FROM mesa WHERE id = ?", (mesa_id,)).fetchone()
        if row is None:
            raise HTTPException(404, "Mesa no encontrada")
        cuenta_abierta = conn.execute(
            "SELECT * FROM cuenta WHERE mesa_id = ? AND estado = 'abierta'", (mesa_id,)
        ).fetchone()
        if cuenta_abierta is None:
            conn.execute(
                "INSERT INTO cuenta (mesa_id, fecha_apertura, estado) VALUES (?, ?, 'abierta')",
                (mesa_id, datetime.now().isoformat(timespec="seconds")),
            )
            conn.execute("UPDATE mesa SET estado = 'ocupada' WHERE id = ?", (mesa_id,))
            conn.commit()
    row = conn.execute("SELECT * FROM mesa WHERE id = ?", (mesa_id,)).fetchone()
    return _mesa_a_dict(conn, row)


def _cuenta_a_dict(conn, cuenta_row) -> dict:
    mesa_row = conn.execute("SELECT * FROM mesa WHERE id = ?", (cuenta_row["mesa_id"],)).fetchone()
    pedidos_rows = conn.execute(
        "SELECT * FROM pedido WHERE cuenta_id = ? ORDER BY id", (cuenta_row["id"],)
    ).fetchall()
    pedidos = [_pedido_a_dict(conn, r) for r in pedidos_rows]
    total = sum(p["total"] for p in pedidos)
    return {
        "id": cuenta_row["id"],
        "mesa_id": cuenta_row["mesa_id"],
        "mesa_nombre": mesa_row["nombre"] if mesa_row else "",
        "fecha_apertura": cuenta_row["fecha_apertura"],
        "fecha_cierre": cuenta_row["fecha_cierre"],
        "estado": cuenta_row["estado"],
        "pedidos": pedidos,
        "total": total,
    }


@app.get("/mesas/{mesa_id}/cuenta", response_model=CuentaOut)
def obtener_cuenta_mesa(mesa_id: int):
    conn = get_connection()
    cuenta_row = conn.execute(
        "SELECT * FROM cuenta WHERE mesa_id = ? AND estado = 'abierta'", (mesa_id,)
    ).fetchone()
    if cuenta_row is None:
        raise HTTPException(404, "La mesa no tiene una cuenta abierta")
    return _cuenta_a_dict(conn, cuenta_row)


@app.post("/mesas/{mesa_id}/pedidos", response_model=PedidoOut, status_code=201)
def crear_pedido_mesa(mesa_id: int, pedido: PedidoIn):
    """Suma una ronda de pedido a la cuenta abierta de la mesa (el comensal
    puede seguir pidiendo mientras la cuenta siga abierta)."""
    conn = get_connection()
    with write_lock:
        cuenta_row = conn.execute(
            "SELECT * FROM cuenta WHERE mesa_id = ? AND estado = 'abierta'", (mesa_id,)
        ).fetchone()
        if cuenta_row is None:
            raise HTTPException(400, "La mesa no tiene una cuenta abierta - abrila primero")
        pedido_id = _insertar_pedido(conn, pedido, cuenta_id=cuenta_row["id"])
    row = conn.execute("SELECT * FROM pedido WHERE id = ?", (pedido_id,)).fetchone()
    return _pedido_a_dict(conn, row)


@app.post("/mesas/{mesa_id}/cerrar", response_model=CuentaOut)
def cerrar_mesa(mesa_id: int):
    """Cierra la cuenta de la mesa y la deja libre otra vez. Devuelve la
    cuenta completa (todas las rondas + total) para armar el ticket."""
    conn = get_connection()
    with write_lock:
        cuenta_row = conn.execute(
            "SELECT * FROM cuenta WHERE mesa_id = ? AND estado = 'abierta'", (mesa_id,)
        ).fetchone()
        if cuenta_row is None:
            raise HTTPException(400, "La mesa no tiene una cuenta abierta")
        conn.execute(
            "UPDATE cuenta SET estado = 'cerrada', fecha_cierre = ? WHERE id = ?",
            (datetime.now().isoformat(timespec="seconds"), cuenta_row["id"]),
        )
        conn.execute("UPDATE mesa SET estado = 'libre' WHERE id = ?", (mesa_id,))
        conn.commit()
        cuenta_id = cuenta_row["id"]
    row = conn.execute("SELECT * FROM cuenta WHERE id = ?", (cuenta_id,)).fetchone()
    return _cuenta_a_dict(conn, row)


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

    mesa_nombre = None
    if pedido_row["cuenta_id"] is not None:
        fila = conn.execute(
            """
            SELECT mesa.nombre AS nombre
            FROM cuenta JOIN mesa ON mesa.id = cuenta.mesa_id
            WHERE cuenta.id = ?
            """,
            (pedido_row["cuenta_id"],),
        ).fetchone()
        mesa_nombre = fila["nombre"] if fila else None

    return {
        "id": pedido_row["id"],
        "fecha": pedido_row["fecha"],
        "estado": pedido_row["estado"],
        "total": pedido_row["total"],
        "nota": pedido_row["nota"],
        "mesa_nombre": mesa_nombre,
        "detalles": [dict(d) for d in detalles_rows],
    }


def _insertar_pedido(conn, pedido: PedidoIn, cuenta_id: int | None) -> int:
    """
    Registra un pedido y descuenta stock. Todo protegido por write_lock:
    esto es la semilla del punto 3 (pool de hilos + lock sobre stock
    compartido), que todavía no está implementado con concurrencia real,
    pero la sección crítica ya queda aislada acá adentro.

    Compartida entre el mostrador (POST /pedidos, cuenta_id=None) y las
    mesas (POST /mesas/{id}/pedidos, cuenta_id=la cuenta abierta) para no
    duplicar la validación de stock/disponibilidad.
    """
    total = 0.0
    for det in pedido.detalles:
        row = conn.execute("SELECT * FROM producto WHERE id = ?", (det.producto_id,)).fetchone()
        if row is None:
            raise HTTPException(404, f"Producto {det.producto_id} no existe")
        if not row["disponible"]:
            raise HTTPException(400, f"'{row['nombre']}' no está disponible")
        if row["stock"] < det.cantidad:
            raise HTTPException(
                400,
                f"Stock insuficiente para '{row['nombre']}' "
                f"(pedido: {det.cantidad}, stock: {row['stock']})",
            )
        total += row["precio"] * det.cantidad

    cur = conn.execute(
        "INSERT INTO pedido (fecha, estado, total, nota, cuenta_id) VALUES (?, ?, ?, ?, ?)",
        (datetime.now().isoformat(timespec="seconds"), "en_preparacion", total, pedido.nota, cuenta_id),
    )
    pedido_id = cur.lastrowid

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
    return pedido_id


@app.get("/pedidos", response_model=list[PedidoOut])
def listar_pedidos():
    conn = get_connection()
    rows = conn.execute("SELECT * FROM pedido ORDER BY id DESC").fetchall()
    return [_pedido_a_dict(conn, r) for r in rows]


@app.post("/pedidos", response_model=PedidoOut, status_code=201)
def crear_pedido(pedido: PedidoIn):
    """Pedido de mostrador/para llevar, sin mesa asociada."""
    conn = get_connection()
    with write_lock:
        pedido_id = _insertar_pedido(conn, pedido, cuenta_id=None)
    row = conn.execute("SELECT * FROM pedido WHERE id = ?", (pedido_id,)).fetchone()
    return _pedido_a_dict(conn, row)


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
