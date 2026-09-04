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
    ProductoUpdate,
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


@app.get("/health")
def health():
    """La GUI Java llama esto al arrancar para confirmar que el backend
    (que ella misma o el instalador ya debería tener corriendo) está vivo."""
    return {"status": "ok"}


# ---------- Productos (catálogo) ----------

@app.get("/productos", response_model=list[ProductoOut])
def listar_productos(incluir_inactivos: bool = False):
    """Por defecto solo trae productos activos (los que están "de alta"
    en el catálogo). El cajero/mozo arma pedidos contra este listado, así
    que un producto dado de baja no debería ni aparecer como opción.
    `incluir_inactivos=true` es para pantallas de administración del
    catálogo, donde sí interesa ver (y poder reactivar) lo dado de baja."""
    conn = get_connection()
    query = "SELECT * FROM producto"
    if not incluir_inactivos:
        query += " WHERE activo = 1"
    query += " ORDER BY nombre"
    rows = conn.execute(query).fetchall()
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


@app.put("/productos/{producto_id}", response_model=ProductoOut)
@app.patch("/productos/{producto_id}", response_model=ProductoOut)
def modificar_producto(producto_id: int, cambios: ProductoUpdate):
    """
    Modificación de producto. Se registran ambos verbos (PUT y PATCH)
    contra el mismo handler: la semántica que termina importando acá es
    la de PATCH (actualización parcial, `exclude_unset` -> solo se tocan
    los campos que vinieron en el body), pero como el enunciado deja
    PUT/PATCH como sinónimos y el JSON que manda la GUI ya es parcial por
    naturaleza (un formulario de edición no siempre repite todo), se
    acepta con cualquiera de los dos verbos para no atarse a esa decisión
    del lado del cliente.
    """
    conn = get_connection()
    with write_lock:
        row = conn.execute("SELECT * FROM producto WHERE id = ?", (producto_id,)).fetchone()
        if row is None:
            raise HTTPException(404, "Producto no encontrado")

        campos = cambios.model_dump(exclude_unset=True)
        if not campos:
            raise HTTPException(400, "No se envió ningún campo para modificar")

        nombre = campos.get("nombre", row["nombre"])
        precio = campos.get("precio", row["precio"])
        stock = campos.get("stock", row["stock"])
        activo = campos.get("activo", bool(row["activo"]))

        conn.execute(
            "UPDATE producto SET nombre = ?, precio = ?, stock = ?, activo = ? WHERE id = ?",
            (nombre, precio, stock, int(activo), producto_id),
        )
        conn.commit()

    row = conn.execute("SELECT * FROM producto WHERE id = ?", (producto_id,)).fetchone()
    return dict(row)


@app.delete("/productos/{producto_id}", response_model=ProductoOut)
def eliminar_producto(producto_id: int):
    """
    Baja de producto. Es una baja LÓGICA (activo = 0), no un DELETE de
    SQL: `detalle_pedido.producto_id` referencia esta tabla sin CASCADE,
    así que un DELETE físico de un producto que ya apareció en algún
    pedido rompería la foreign key (o, peor, si tuviera cascade, borraría
    silenciosamente el detalle de pedidos históricos). Con baja lógica el
    producto desaparece de `GET /productos` (ver más arriba) pero el
    historial de pedidos que lo mencionan queda intacto, y se puede dar
    de alta de nuevo con PATCH {"activo": true}.
    """
    conn = get_connection()
    with write_lock:
        row = conn.execute("SELECT * FROM producto WHERE id = ?", (producto_id,)).fetchone()
        if row is None:
            raise HTTPException(404, "Producto no encontrado")
        conn.execute("UPDATE producto SET activo = 0 WHERE id = ?", (producto_id,))
        conn.commit()

    row = conn.execute("SELECT * FROM producto WHERE id = ?", (producto_id,)).fetchone()
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


def _restaurar_stock(conn, pedido_id: int) -> None:
    """Le devuelve al stock de cada producto la cantidad que este pedido
    tenía descontada. Se usa tanto al modificar un pedido (se devuelve
    todo y se vuelve a descontar con las líneas nuevas) como al
    cancelarlo (se devuelve y no se vuelve a descontar nada). No hace
    commit: queda dentro de la transacción del caller."""
    detalles = conn.execute(
        "SELECT producto_id, cantidad FROM detalle_pedido WHERE pedido_id = ?",
        (pedido_id,),
    ).fetchall()
    for det in detalles:
        conn.execute(
            "UPDATE producto SET stock = stock + ? WHERE id = ?",
            (det["cantidad"], det["producto_id"]),
        )


@app.get("/pedidos", response_model=list[PedidoOut])
def listar_pedidos():
    conn = get_connection()
    rows = conn.execute("SELECT * FROM pedido ORDER BY id DESC").fetchall()
    return [_pedido_a_dict(conn, r) for r in rows]


@app.post("/pedidos", response_model=PedidoOut, status_code=201)
def crear_pedido(pedido: PedidoIn):
    """
    Registra un pedido y descuenta stock. Todo protegido por write_lock:
    esto es la semilla del punto 3 (pool de hilos + lock sobre stock
    compartido), que todavía no está implementado con concurrencia real,
    pero la sección crítica ya queda aislada acá adentro.
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


@app.put("/pedidos/{pedido_id}", response_model=PedidoOut)
def modificar_pedido(pedido_id: int, pedido: PedidoIn):
    """
    Caso de uso del cajero: se equivocó al cargar el pedido (falta un
    producto, sobra otro, cambió la cantidad) y lo corrige antes de que
    se empiece a preparar.

    Solo se puede modificar mientras esté "en_preparacion". Una vez que
    pasa a "listo" o "entregado" ya se cocinó/sirvió, así que reescribir
    el detalle ahí sería mentir sobre qué se preparó realmente; para ese
    caso lo que corresponde es cancelar (DELETE) y cargar un pedido nuevo.

    La estrategia reutiliza la misma lógica que crear_pedido: se devuelve
    todo el stock de las líneas viejas (_restaurar_stock) y se vuelve a
    "armar" el pedido con las líneas nuevas, validando stock disponible
    de nuevo antes de tocar nada. Si algo no entra (stock insuficiente,
    producto inexistente), se hace rollback de la restauración de stock
    para no dejar la conexión compartida con una transacción a medio
    aplicar.
    """
    conn = get_connection()
    with write_lock:
        row = conn.execute("SELECT * FROM pedido WHERE id = ?", (pedido_id,)).fetchone()
        if row is None:
            raise HTTPException(404, "Pedido no encontrado")
        if row["estado"] != "en_preparacion":
            raise HTTPException(
                409,
                f"No se puede modificar un pedido en estado '{row['estado']}'",
            )

        # 1. Devolver el stock de las líneas actuales
        _restaurar_stock(conn, pedido_id)

        # 2. Validar que las líneas nuevas entren con el stock ya restaurado
        total = 0.0
        for det in pedido.detalles:
            prod = conn.execute(
                "SELECT * FROM producto WHERE id = ?", (det.producto_id,)
            ).fetchone()
            if prod is None:
                conn.rollback()
                raise HTTPException(404, f"Producto {det.producto_id} no existe")
            if prod["stock"] < det.cantidad:
                conn.rollback()
                raise HTTPException(
                    400,
                    f"Stock insuficiente para '{prod['nombre']}' "
                    f"(pedido: {det.cantidad}, stock: {prod['stock']})",
                )
            total += prod["precio"] * det.cantidad

        # 3. Reemplazar el detalle y descontar stock de las líneas nuevas
        conn.execute("DELETE FROM detalle_pedido WHERE pedido_id = ?", (pedido_id,))
        for det in pedido.detalles:
            conn.execute(
                "INSERT INTO detalle_pedido (pedido_id, producto_id, cantidad) VALUES (?, ?, ?)",
                (pedido_id, det.producto_id, det.cantidad),
            )
            conn.execute(
                "UPDATE producto SET stock = stock - ? WHERE id = ?",
                (det.cantidad, det.producto_id),
            )

        conn.execute(
            "UPDATE pedido SET total = ?, nota = ? WHERE id = ?",
            (total, pedido.nota, pedido_id),
        )
        conn.commit()

    row = conn.execute("SELECT * FROM pedido WHERE id = ?", (pedido_id,)).fetchone()
    return _pedido_a_dict(conn, row)


@app.delete("/pedidos/{pedido_id}", response_model=PedidoOut)
def cancelar_pedido(pedido_id: int):
    """
    Caso de uso del cajero: cancelar un pedido (el cliente se arrepintió,
    se cargó por error, etc).

    No se borra la fila de `pedido`: queda como registro para el
    arqueo/historial del día, solo que con estado "cancelado". Lo que sí
    se hace es devolver el stock que ese pedido tenía descontado
    (_restaurar_stock), porque esos productos vuelven a estar
    disponibles para vender.

    No se puede cancelar un pedido ya entregado (ya se le sirvió al
    cliente, no hay stock que devolver ni sentido en "deshacerlo"), ni
    cancelar dos veces el mismo pedido.
    """
    conn = get_connection()
    with write_lock:
        row = conn.execute("SELECT * FROM pedido WHERE id = ?", (pedido_id,)).fetchone()
        if row is None:
            raise HTTPException(404, "Pedido no encontrado")
        if row["estado"] == "cancelado":
            raise HTTPException(409, "El pedido ya estaba cancelado")
        if row["estado"] == "entregado":
            raise HTTPException(409, "No se puede cancelar un pedido ya entregado")

        _restaurar_stock(conn, pedido_id)
        conn.execute("UPDATE pedido SET estado = ? WHERE id = ?", ("cancelado", pedido_id))
        conn.commit()

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
