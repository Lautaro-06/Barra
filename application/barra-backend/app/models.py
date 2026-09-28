"""
models.py

Modelos Pydantic: definen el "contrato" JSON entre Java y Python.
Java arma estos mismos campos como objetos/records al serializar/deserializar.
"""

import re

from pydantic import BaseModel, Field, field_validator

# Compartidos con main.py para validar emails y la hora del resumen diario.
EMAIL_RE = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")
HORA_RE = r"^([01]\d|2[0-3]):[0-5]\d$"  # HH:MM, 00:00 a 23:59

class ProductoOut(BaseModel):
    id: int
    nombre: str
    precio: float
    stock: int
    disponible: bool
    umbral_stock: int | None = None


class ProductoIn(BaseModel):
    nombre: str
    precio: float = Field(gt=0)
    stock: int = Field(ge=0, default=0)
    disponible: bool = True
    umbral_stock: int | None = Field(default=None, ge=0)


class ProductoPatch(BaseModel):
    """Edición desde el panel de Admin: todos los campos son opcionales,
    solo se pisa lo que venga seteado (ver PATCH /productos/{id}).

    umbral_stock es un caso especial: a diferencia de los demás campos,
    null es un valor válido y con significado propio ("usar el umbral
    global de configuracion"), no solo "no lo mandé". Por eso el endpoint
    no puede usar el mismo truco de "if cambios.X is not None" que usa
    para el resto - necesita mirar model_fields_set para distinguir
    "no vino en el body" de "vino explícitamente en null"."""
    nombre: str | None = None
    precio: float | None = Field(default=None, gt=0)
    stock: int | None = Field(default=None, ge=0)
    disponible: bool | None = None
    umbral_stock: int | None = Field(default=None, ge=0)


class AdminOut(BaseModel):
    nombre_dueno: str
    email_dueno: str
    telefono: str | None


class AdminIn(BaseModel):
    nombre_dueno: str = Field(min_length=1)
    email_dueno: str = Field(pattern=EMAIL_RE.pattern)
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
    # email_destino si está cargado, sino el email del dueño (/admin). Es
    # a donde se van a mandar realmente las alertas y el resumen diario.
    email_destino_efectivo: str | None
    smtp_host: str | None
    smtp_port: int
    smtp_usuario: str | None
    smtp_password_configurada: bool
    resumen_diario_habilitado: bool
    resumen_diario_hora: str


class ConfiguracionIn(BaseModel):
    """Todos los campos son opcionales: POST/PUT /configuracion solo pisa
    lo que venga en el body (ver model_fields_set en main.py), así se
    puede mandar, por ejemplo, solo {"email_destino": "..."}.

    Los campos que en la base son NOT NULL (nombre_local, los bool, el
    umbral, el puerto y la hora) no aceptan null explícito: omitirlos es
    "no lo cambio", mandarlos en null es un error (422)."""
    nombre_local: str | None = Field(default=None, min_length=1)
    umbral_stock_global: int | None = Field(default=None, ge=0)
    email_habilitado: bool | None = None
    email_destino: str | None = None
    smtp_host: str | None = None
    smtp_port: int | None = Field(default=None, ge=1, le=65535)
    smtp_usuario: str | None = None
    smtp_password: str | None = None
    resumen_diario_habilitado: bool | None = None
    resumen_diario_hora: str | None = Field(default=None, pattern=HORA_RE)

    @field_validator(
        "nombre_local", "umbral_stock_global", "email_habilitado",
        "smtp_port", "resumen_diario_habilitado", "resumen_diario_hora",
    )
    @classmethod
    def _no_null(cls, valor):
        if valor is None:
            raise ValueError("no puede ser null (omitilo si no querés cambiarlo)")
        return valor

    @field_validator("email_destino", "smtp_host", "smtp_usuario", mode="before")
    @classmethod
    def _vacio_a_none(cls, valor):
        # La GUI manda "" cuando el campo queda en blanco: se guarda como
        # NULL, igual que si nunca se hubiera cargado.
        if isinstance(valor, str):
            valor = valor.strip()
            return valor or None
        return valor

    @field_validator("email_destino")
    @classmethod
    def _email_valido(cls, valor):
        if valor is not None and not EMAIL_RE.match(valor):
            raise ValueError("no tiene un formato de email válido")
        return valor


class AlertaOut(BaseModel):
    producto_id: int
    nombre: str
    stock: int
    umbral: int