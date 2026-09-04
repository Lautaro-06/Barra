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
    disponible: bool


class ProductoIn(BaseModel):
    nombre: str
    precio: float = Field(gt=0)
    stock: int = Field(ge=0, default=0)
    disponible: bool = True


class ProductoPatch(BaseModel):
    """Edición desde el panel de Admin: todos los campos son opcionales,
    solo se pisa lo que venga seteado (ver PATCH /productos/{id})."""
    nombre: str | None = None
    precio: float | None = Field(default=None, gt=0)
    stock: int | None = Field(default=None, ge=0)
    disponible: bool | None = None


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
    mesa_nombre: str | None = None
    detalles: list[DetalleOut]


class EstadoIn(BaseModel):
    estado: str  # "en_preparacion" | "listo" | "entregado"


class MesaOut(BaseModel):
    id: int
    nombre: str
    estado: str  # "libre" | "ocupada"
    cuenta_id: int | None
    total_actual: float


class MesaIn(BaseModel):
    nombre: str


class CuentaOut(BaseModel):
    id: int
    mesa_id: int
    mesa_nombre: str
    fecha_apertura: str
    fecha_cierre: str | None
    estado: str  # "abierta" | "cerrada"
    pedidos: list[PedidoOut]
    total: float


class ConfiguracionOut(BaseModel):
    nombre_local: str


class ConfiguracionIn(BaseModel):
    nombre_local: str = Field(min_length=1)
