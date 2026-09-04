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


def init_db() -> None:
    """Crea las tablas del DER (sección 16) si no existen todavía."""
    conn = get_connection()
    conn.executescript(
        """
        CREATE TABLE IF NOT EXISTS producto (
            id      INTEGER PRIMARY KEY AUTOINCREMENT,
            nombre  TEXT NOT NULL,
            precio  REAL NOT NULL,
            stock   INTEGER NOT NULL DEFAULT 0,
            activo  INTEGER NOT NULL DEFAULT 1
        );

        CREATE TABLE IF NOT EXISTS pedido (
            id       INTEGER PRIMARY KEY AUTOINCREMENT,
            fecha    TEXT NOT NULL,
            estado   TEXT NOT NULL DEFAULT 'en_preparacion',
            total    REAL NOT NULL DEFAULT 0,
            nota     TEXT
        );

        CREATE TABLE IF NOT EXISTS detalle_pedido (
            id           INTEGER PRIMARY KEY AUTOINCREMENT,
            pedido_id    INTEGER NOT NULL REFERENCES pedido(id) ON DELETE CASCADE,
            producto_id  INTEGER NOT NULL REFERENCES producto(id),
            cantidad     INTEGER NOT NULL
        );
        """
    )
    conn.commit()

    # Migración liviana: si `barra.db` ya existía de antes (sin la columna
    # `activo`, agregada para la baja lógica de productos), se la agrega.
    # CREATE TABLE IF NOT EXISTS no toca tablas que ya existen, así que la
    # columna nueva hay que sumarla a mano la primera vez que corre este
    # código sobre una base vieja.
    columnas = [fila["name"] for fila in conn.execute("PRAGMA table_info(producto)")]
    if "activo" not in columnas:
        conn.execute("ALTER TABLE producto ADD COLUMN activo INTEGER NOT NULL DEFAULT 1")
        conn.commit()

    # Semilla de datos para poder probar la GUI Java desde el primer día
    cur = conn.execute("SELECT COUNT(*) FROM producto")
    if cur.fetchone()[0] == 0:
        conn.executemany(
            "INSERT INTO producto (nombre, precio, stock) VALUES (?, ?, ?)",
            [
                ("Hamburguesa clásica", 4500.0, 20),
                ("Papas fritas", 2200.0, 30),
                ("Gaseosa 500ml", 1800.0, 40),
            ],
        )
        conn.commit()
