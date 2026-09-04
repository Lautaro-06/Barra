"""
models.py

Modelos Pydantic: definen el "contrato" JSON entre Java y Python.
Java arma estos mismos campos como objetos/records al serializar/deserializar.
"""

from pydantic import BaseModel, Field


class ProductoOut(BaseModel):
    id: int
    nombre: str
    precio: float
    stock: int
    activo: bool


class ProductoIn(BaseModel):
    nombre: str
    precio: float = Field(gt=0)
    stock: int = Field(ge=0, default=0)


class ProductoUpdate(BaseModel):
    """Para PUT/PATCH /productos/{id}. Todos los campos son opcionales:
    solo se actualiza lo que venga en el body (permite mandar nomás el
    precio, o nomás el stock, sin repetir el resto)."""

    nombre: str | None = None
    precio: float | None = Field(default=None, gt=0)
    stock: int | None = Field(default=None, ge=0)
    activo: bool | None = None


class DetalleIn(BaseModel):
    producto_id: int
    cantidad: int = Field(gt=0)


class PedidoIn(BaseModel):
    nota: str | None = None
    detalles: list[DetalleIn]


class DetalleOut(BaseModel):
    producto_id: int
    nombre_producto: str
    cantidad: int
    subtotal: float


class PedidoOut(BaseModel):
    id: int
    fecha: str
    estado: str
    total: float
    nota: str | None
    detalles: list[DetalleOut]


class EstadoIn(BaseModel):
    estado: str  # "en_preparacion" | "listo" | "entregado"
