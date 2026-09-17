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


class AdminOut(BaseModel):
    nombre_dueno: str
    email_dueno: str
    telefono: str | None


class AdminIn(BaseModel):
    nombre_dueno: str = Field(min_length=1)
    email_dueno: str = Field(min_length=1)
    telefono: str | None = None


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
    umbral_stock_global: int
    email_habilitado: bool
    email_destino: str | None
    smtp_host: str | None
    smtp_port: int
    smtp_usuario: str | None
    smtp_password_configurada: bool
    telegram_habilitado: bool
    telegram_chat_id: str | None
    telegram_token_configurado: bool
    resumen_diario_habilitado: bool
    resumen_diario_hora: str


class ConfiguracionIn(BaseModel):
    nombre_local: str = Field(min_length=1)
    umbral_stock_global: int = Field(ge=0, default=5)
    email_habilitado: bool = False
    email_destino: str | None = None
    smtp_host: str | None = None
    smtp_port: int = Field(default=587, ge=1, le=65535)
    smtp_usuario: str | None = None
    smtp_password: str | None = None
    telegram_habilitado: bool = False
    telegram_chat_id: str | None = None
    telegram_token: str | None = None
    resumen_diario_habilitado: bool = False
    resumen_diario_hora: str = "23:00"


class AlertaOut(BaseModel):
    producto_id: int
    nombre: str
    stock: int
    umbral: int