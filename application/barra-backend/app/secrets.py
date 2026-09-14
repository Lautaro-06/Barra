"""Cifrado de secretos persistidos por el backend.

La clave nunca se guarda en SQLite: debe entregarse mediante BARRA_SECRET_KEY.
"""

import os

from cryptography.fernet import Fernet, InvalidToken


class SecretConfigurationError(RuntimeError):
    """La clave externa de cifrado no existe o no es válida."""


def _fernet() -> Fernet:
    key = os.environ.get("BARRA_SECRET_KEY")
    if not key:
        raise SecretConfigurationError(
            "BARRA_SECRET_KEY es obligatoria para guardar o leer credenciales"
        )
    try:
        return Fernet(key.encode("ascii"))
    except (ValueError, UnicodeEncodeError) as exc:
        raise SecretConfigurationError(
            "BARRA_SECRET_KEY debe ser una clave Fernet válida"
        ) from exc


def encrypt_secret(value: str) -> str:
    """Cifra un secreto y devuelve un valor apto para guardar en SQLite."""
    if not value:
        raise ValueError("No se puede cifrar un secreto vacío")
    return _fernet().encrypt(value.encode("utf-8")).decode("ascii")


def decrypt_secret(value: str) -> str:
    """Descifra un valor almacenado en SQLite."""
    try:
        return _fernet().decrypt(value.encode("ascii")).decode("utf-8")
    except (InvalidToken, UnicodeDecodeError, UnicodeEncodeError) as exc:
        raise SecretConfigurationError("No se pudo descifrar el secreto") from exc