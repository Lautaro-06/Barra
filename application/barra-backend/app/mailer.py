"""
mailer.py

Envío real de emails al dueño (alertas de stock bajo y, más adelante, el
resumen diario), usando la configuración SMTP guardada en /configuracion.

- La contraseña SMTP se guarda cifrada (ver secrets.py): acá se descifra
  solo en el momento de mandar, nunca se guarda descifrada en memoria.
- Puerto 465 -> SMTP_SSL (TLS desde el primer byte). Cualquier otro puerto
  (típicamente 587) -> SMTP + STARTTLS. Si el servidor no ofrece STARTTLS
  no se manda nada: sería mandar la contraseña en texto plano.
- La conexión SMTP puede tardar varios segundos: leer_config_email() se
  llama con write_lock tomado (es la conexión SQLite compartida), pero
  enviar_email() se llama AFUERA del lock, para no frenar los pedidos
  mientras se habla con el servidor de mail.
"""

import logging
import smtplib
import ssl
from dataclasses import dataclass
from email.message import EmailMessage

from .secrets import SecretConfigurationError, decrypt_secret

logger = logging.getLogger("barra.mailer")

SMTP_TIMEOUT_SEGUNDOS = 20


class EmailError(RuntimeError):
    """No se pudo mandar el email (config incompleta, SMTP caído, credenciales, etc.)."""


@dataclass(frozen=True)
class ConfigEmail:
    nombre_local: str
    destino: str
    smtp_host: str
    smtp_port: int
    smtp_usuario: str
    smtp_password: str
    email_habilitado: bool
    resumen_diario_habilitado: bool


def obtener_email_destino_efectivo(conn, email_destino: str | None) -> str | None:
    """A dónde se mandan las alertas y el resumen diario: email_destino de
    /configuracion si está cargado, sino el email del dueño de /admin.
    Así alcanza con completar los datos del dueño una sola vez."""
    if email_destino:
        return email_destino
    fila = conn.execute("SELECT email_dueno FROM admin WHERE id = 1").fetchone()
    return (fila["email_dueno"] or None) if fila else None


def leer_config_email(conn) -> ConfigEmail:
    """Lee la configuración SMTP y descifra la contraseña. Llamar con
    write_lock tomado (usa la conexión SQLite compartida). Lanza
    EmailError si falta algo para poder mandar."""
    row = conn.execute("SELECT * FROM configuracion WHERE id = 1").fetchone()
    if row is None:
        raise EmailError("No existe la fila de configuración")

    destino = obtener_email_destino_efectivo(conn, row["email_destino"])
    faltantes = [
        nombre for nombre, valor in (
            ("smtp_host", row["smtp_host"]),
            ("smtp_usuario", row["smtp_usuario"]),
            ("smtp_password", row["smtp_password_cifrada"]),
            ("email_destino", destino),
        )
        if not valor
    ]
    if faltantes:
        raise EmailError("Configuración de email incompleta, falta: " + ", ".join(faltantes))

    try:
        password = decrypt_secret(row["smtp_password_cifrada"])
    except SecretConfigurationError as exc:
        raise EmailError(f"No se pudo leer la contraseña SMTP: {exc}") from exc

    return ConfigEmail(
        nombre_local=row["nombre_local"],
        destino=destino,
        smtp_host=row["smtp_host"],
        smtp_port=row["smtp_port"],
        smtp_usuario=row["smtp_usuario"],
        smtp_password=password,
        email_habilitado=bool(row["email_habilitado"]),
        resumen_diario_habilitado=bool(row["resumen_diario_habilitado"]),
    )


def _contexto_tls() -> ssl.SSLContext:
    # Función aparte para poder reemplazarla en pruebas con un servidor
    # SMTP local de certificado autofirmado.
    return ssl.create_default_context()


def enviar_email(config: ConfigEmail, asunto: str, cuerpo: str) -> None:
    """Manda un email de texto plano a config.destino. NO llamar con
    write_lock tomado. Lanza EmailError si algo falla."""
    mensaje = EmailMessage()
    mensaje["Subject"] = asunto
    mensaje["From"] = config.smtp_usuario
    mensaje["To"] = config.destino
    mensaje.set_content(cuerpo)

    try:
        if config.smtp_port == 465:
            with smtplib.SMTP_SSL(
                config.smtp_host, config.smtp_port,
                timeout=SMTP_TIMEOUT_SEGUNDOS, context=_contexto_tls(),
            ) as smtp:
                smtp.login(config.smtp_usuario, config.smtp_password)
                smtp.send_message(mensaje)
        else:
            with smtplib.SMTP(config.smtp_host, config.smtp_port, timeout=SMTP_TIMEOUT_SEGUNDOS) as smtp:
                smtp.ehlo()
                if not smtp.has_extn("starttls"):
                    raise EmailError(
                        f"{config.smtp_host}:{config.smtp_port} no soporta STARTTLS - "
                        "no se manda la contraseña sin cifrar (probá con el puerto 465)"
                    )
                smtp.starttls(context=_contexto_tls())
                smtp.ehlo()
                smtp.login(config.smtp_usuario, config.smtp_password)
                smtp.send_message(mensaje)
    except smtplib.SMTPAuthenticationError as exc:
        raise EmailError(
            "El servidor SMTP rechazó el usuario/contraseña "
            "(en Gmail hace falta una 'contraseña de aplicación')"
        ) from exc
    except (smtplib.SMTPException, OSError) as exc:
        raise EmailError(f"No se pudo mandar el email: {exc}") from exc

    logger.info("Email enviado a %s: %s", config.destino, asunto)


def armar_alerta_stock(nombre_local: str, productos: list[dict]) -> tuple[str, str]:
    """Asunto y cuerpo del email de alerta. productos: dicts con nombre,
    stock y umbral (mismo formato que el snapshot de GET /alertas)."""
    if len(productos) == 1:
        asunto = f"[{nombre_local}] Stock bajo: {productos[0]['nombre']}"
    else:
        asunto = f"[{nombre_local}] Stock bajo en {len(productos)} productos"

    lineas = [
        f"Los siguientes productos de {nombre_local} quedaron por debajo del umbral de stock:",
        "",
    ]
    for p in productos:
        lineas.append(f"  - {p['nombre']}: quedan {p['stock']} (umbral: {p['umbral']})")
    lineas += [
        "",
        "No vas a recibir otra alerta por estos productos hasta que se repongan",
        "por encima del umbral y vuelvan a bajar.",
        "",
        "-- Barra",
    ]
    return asunto, "\n".join(lineas)