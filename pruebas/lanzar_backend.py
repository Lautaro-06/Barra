"""Levanta una copia del backend de escritorio para la campaña de pruebas.

Lo usa test_escritorio.py. Igual que run_backend.py (genera la clave de
cifrado si falta), pero hace que el envío de emails confíe en el
certificado autofirmado del SMTP de prueba: mailer._contexto_tls está
pensado para reemplazarse así en pruebas.

Uso: python lanzar_backend.py <carpeta_backend> <certificado.pem>
"""
import os
import ssl
import sys

base, cert = sys.argv[1], sys.argv[2]
os.chdir(base)
sys.path.insert(0, base)

import run_backend  # noqa: E402

run_backend._asegurar_secret_key()

from app import mailer  # noqa: E402


def _contexto_de_prueba():
    return ssl.create_default_context(cafile=cert)


mailer._contexto_tls = _contexto_de_prueba

import uvicorn  # noqa: E402
from app.main import app  # noqa: E402

uvicorn.run(app, host="127.0.0.1", port=8000, log_level="warning")
