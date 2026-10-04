"""
resumen.py

Resumen de ventas que se le manda al dueño por email una vez por día (ver
el hilo resumen-diario en concurrency.py).

El resumen cubre un PERÍODO, no un día calendario: por defecto las 24hs
anteriores a resumen_diario_hora. Así un local que cierra a las 03:00 y
configura el resumen para esa hora recibe la noche completa en un solo
email, en vez de partida en dos días. Los pedidos se filtran por
pedido.fecha, que se guarda como texto ISO ("2026-09-30T21:15:00"): con
ese formato comparar strings es lo mismo que comparar fechas.
"""

from datetime import datetime


def _iso(momento: datetime) -> str:
    return momento.isoformat(timespec="seconds")


def calcular_resumen(conn, desde: datetime, hasta: datetime) -> dict:
    """Arma el resumen de ventas de [desde, hasta). Llamar con write_lock
    tomado (usa la conexión SQLite compartida)."""
    rango = (_iso(desde), _iso(hasta))

    ventas = conn.execute(
        """
        SELECT COUNT(*) AS pedidos,
               COALESCE(SUM(total), 0) AS total,
               COALESCE(SUM(CASE WHEN cuenta_id IS NULL THEN 1 ELSE 0 END), 0) AS pedidos_mostrador,
               COALESCE(SUM(CASE WHEN cuenta_id IS NULL THEN total ELSE 0 END), 0) AS total_mostrador
        FROM pedido
        WHERE fecha >= ? AND fecha < ?
        """,
        rango,
    ).fetchone()

    cuentas_cerradas = conn.execute(
        "SELECT COUNT(*) FROM cuenta WHERE fecha_cierre >= ? AND fecha_cierre < ?",
        rango,
    ).fetchone()[0]

    productos = conn.execute(
        """
        SELECT p.nombre AS nombre, SUM(dp.cantidad) AS cantidad
        FROM detalle_pedido dp
        JOIN pedido pe ON pe.id = dp.pedido_id
        JOIN producto p ON p.id = dp.producto_id
        WHERE pe.fecha >= ? AND pe.fecha < ?
        GROUP BY p.id
        ORDER BY cantidad DESC, p.nombre
        LIMIT 10
        """,
        rango,
    ).fetchall()

    # Mismo criterio de umbral que el hilo de vigilancia de stock: el
    # propio del producto si tiene, sino el global.
    stock_bajo = conn.execute(
        """
        SELECT p.nombre AS nombre, p.stock AS stock,
               COALESCE(p.umbral_stock, c.umbral_stock_global, 5) AS umbral
        FROM producto p
        LEFT JOIN configuracion c ON c.id = 1
        WHERE p.stock < COALESCE(p.umbral_stock, c.umbral_stock_global, 5)
        ORDER BY p.stock, p.nombre
        """
    ).fetchall()

    pedidos = ventas["pedidos"]
    total = ventas["total"]
    return {
        "desde": _iso(desde),
        "hasta": _iso(hasta),
        "cantidad_pedidos": pedidos,
        "total_vendido": total,
        "ticket_promedio": total / pedidos if pedidos else 0.0,
        "mostrador": {
            "pedidos": ventas["pedidos_mostrador"],
            "total": ventas["total_mostrador"],
        },
        "mesas": {
            "pedidos": pedidos - ventas["pedidos_mostrador"],
            "total": total - ventas["total_mostrador"],
            "cuentas_cerradas": cuentas_cerradas,
        },
        "productos_mas_vendidos": [dict(p) for p in productos],
        "stock_bajo": [dict(p) for p in stock_bajo],
    }


def _moneda(valor: float) -> str:
    """1234.5 -> "$ 1.234,50" (mismo formato que UiTheme.moneda en la GUI)."""
    return "$ " + f"{valor:,.2f}".replace(",", "X").replace(".", ",").replace("X", ".")


def _fecha_corta(iso: str) -> str:
    return datetime.fromisoformat(iso).strftime("%d/%m %H:%M")


def armar_email_resumen(nombre_local: str, resumen: dict) -> tuple[str, str]:
    """Asunto y cuerpo (texto plano) del email de resumen diario."""
    fecha = datetime.fromisoformat(resumen["hasta"]).strftime("%d/%m")
    asunto = f"[{nombre_local}] Resumen de ventas del {fecha}: {_moneda(resumen['total_vendido'])}"

    lineas = [
        f"Resumen de ventas de {nombre_local}",
        f"Del {_fecha_corta(resumen['desde'])} al {_fecha_corta(resumen['hasta'])}",
        "",
    ]

    if resumen["cantidad_pedidos"] == 0:
        lineas.append("No hubo ventas en este período.")
    else:
        mostrador = resumen["mostrador"]
        mesas = resumen["mesas"]
        lineas += [
            f"Total vendido:    {_moneda(resumen['total_vendido'])}",
            f"Pedidos:          {resumen['cantidad_pedidos']}",
            f"Ticket promedio:  {_moneda(resumen['ticket_promedio'])}",
            "",
            f"  Mostrador: {mostrador['pedidos']} pedidos - {_moneda(mostrador['total'])}",
            f"  Mesas:     {mesas['pedidos']} pedidos - {_moneda(mesas['total'])}"
            f" ({mesas['cuentas_cerradas']} cuentas cerradas)",
            "",
            "Productos más vendidos:",
        ]
        for i, p in enumerate(resumen["productos_mas_vendidos"], start=1):
            lineas.append(f"  {i}. {p['nombre']} x {p['cantidad']}")

    lineas += ["", "Stock bajo en este momento:"]
    if resumen["stock_bajo"]:
        for p in resumen["stock_bajo"]:
            lineas.append(f"  - {p['nombre']}: quedan {p['stock']} (umbral: {p['umbral']})")
    else:
        lineas.append("  Ningún producto con stock bajo.")

    lineas += ["", "-- Barra"]
    return asunto, "\n".join(lineas)