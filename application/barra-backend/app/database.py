"""
database.py

Punto clave de la arquitectura: PYTHON ES EL ÚNICO DUEÑO DEL ARCHIVO SQLITE.

- Java (la GUI) NUNCA toca el .db directamente. Ni siquiera sabe que existe.
- Toda lectura/escritura pasa por acá, y de acá sale únicamente por la API HTTP.
- Esto evita el problema clásico de "dos procesos escribiendo el mismo archivo
  SQLite a la vez" -> corrupción / locks eternos.

Nota sobre concurrencia (se profundiza en el punto 3 del proyecto, todavía no
implementado acá): SQLite + hilos requiere cuidado. Por eso:
  - check_same_thread=False (FastAPI corre cada request en un hilo del pool)
  - una única conexión reutilizada, protegida con un Lock a la hora de escribir
Esto es la base mínima; el pool de hilos para pedidos concurrentes y el hilo
de vigilancia de stock se agregan en el siguiente paso del proyecto.
"""

import sqlite3
import threading
from pathlib import Path

DB_PATH = Path(__file__).parent.parent / "barra.db"

# Lock global de escritura: aunque SQLite ya serializa a nivel de archivo,
# este lock evita condiciones de carrera dentro de nuestra propia lógica
# (ej: leer stock, decidir, y recién después descontar).
write_lock = threading.Lock()

# Conexión única compartida (Python es el único dueño del archivo)
_connection: sqlite3.Connection | None = None


def get_connection() -> sqlite3.Connection:
    global _connection
    if _connection is None:
        _connection = sqlite3.connect(DB_PATH, check_same_thread=False)
        _connection.row_factory = sqlite3.Row
        _connection.execute("PRAGMA foreign_keys = ON")
    return _connection


def _agregar_columna_si_falta(conn: sqlite3.Connection, tabla: str, columna: str, definicion: str) -> None:
    """ALTER TABLE ... ADD COLUMN es idempotente a mano: SQLite no soporta
    'IF NOT EXISTS' para columnas, así que se chequea el esquema actual
    primero. Esto permite versionar barra.db sin tener que borrarlo cada
    vez que se agrega un campo nuevo (ej: producto.disponible)."""
    columnas_actuales = [fila["name"] for fila in conn.execute(f"PRAGMA table_info({tabla})")]
    if columna not in columnas_actuales:
        conn.execute(f"ALTER TABLE {tabla} ADD COLUMN {definicion}")


def init_db() -> None:
    """Crea las tablas del DER (sección 16) si no existen todavía."""
    conn = get_connection()
    conn.executescript(
        """
        CREATE TABLE IF NOT EXISTS producto (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            nombre      TEXT NOT NULL,
            precio      REAL NOT NULL,
            stock       INTEGER NOT NULL DEFAULT 0,
            disponible  INTEGER NOT NULL DEFAULT 1
        );

        CREATE TABLE IF NOT EXISTS mesa (
            id      INTEGER PRIMARY KEY AUTOINCREMENT,
            nombre  TEXT NOT NULL,
            estado  TEXT NOT NULL DEFAULT 'libre'
        );

        CREATE TABLE IF NOT EXISTS cuenta (
            id              INTEGER PRIMARY KEY AUTOINCREMENT,
            mesa_id         INTEGER NOT NULL REFERENCES mesa(id),
            fecha_apertura  TEXT NOT NULL,
            fecha_cierre    TEXT,
            estado          TEXT NOT NULL DEFAULT 'abierta'
        );

        CREATE TABLE IF NOT EXISTS pedido (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            fecha      TEXT NOT NULL,
            estado     TEXT NOT NULL DEFAULT 'en_preparacion',
            total      REAL NOT NULL DEFAULT 0,
            nota       TEXT,
            cuenta_id  INTEGER REFERENCES cuenta(id)
        );

        CREATE TABLE IF NOT EXISTS detalle_pedido (
            id           INTEGER PRIMARY KEY AUTOINCREMENT,
            pedido_id    INTEGER NOT NULL REFERENCES pedido(id) ON DELETE CASCADE,
            producto_id  INTEGER NOT NULL REFERENCES producto(id),
            cantidad     INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS configuracion (
            id            INTEGER PRIMARY KEY CHECK (id = 1),
            nombre_local  TEXT NOT NULL DEFAULT 'Mi local'
        );
        """
    )
    conn.commit()

    # Migraciones para bases de datos creadas antes de sumar mesas/cuentas -
    # así no hace falta borrar barra.db para actualizar.
    _agregar_columna_si_falta(conn, "producto", "disponible", "disponible INTEGER NOT NULL DEFAULT 1")
    _agregar_columna_si_falta(conn, "pedido", "cuenta_id", "cuenta_id INTEGER REFERENCES cuenta(id)")
    conn.commit()

    # Semilla de datos para poder probar la GUI Java desde el primer día
    cur = conn.execute("SELECT COUNT(*) FROM producto")
    if cur.fetchone()[0] == 0:
        conn.executemany(
            "INSERT INTO producto (nombre, precio, stock, disponible) VALUES (?, ?, ?, 1)",
            [
                ("Hamburguesa clásica", 4500.0, 20),
                ("Papas fritas", 2200.0, 30),
                ("Gaseosa 500ml", 1800.0, 40),
            ],
        )
        conn.commit()

    cur = conn.execute("SELECT COUNT(*) FROM mesa")
    if cur.fetchone()[0] == 0:
        conn.executemany(
            "INSERT INTO mesa (nombre, estado) VALUES (?, 'libre')",
            [(f"Mesa {i}",) for i in range(1, 7)],
        )
        conn.commit()

    cur = conn.execute("SELECT COUNT(*) FROM configuracion")
    if cur.fetchone()[0] == 0:
        conn.execute("INSERT INTO configuracion (id, nombre_local) VALUES (1, 'Mi local')")
        conn.commit()
