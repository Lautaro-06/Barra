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


class ProductoIn(BaseModel):
    nombre: str
    precio: float = Field(gt=0)
    stock: int = Field(ge=0, default=0)


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
