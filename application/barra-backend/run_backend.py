import os
import sys
from pathlib import Path


# Los ejecutables --windowed de PyInstaller no tienen consola: sys.stdout
# y sys.stderr quedan en None (no un stream vacío), y uvicorn se rompe al
# intentar loguear porque llama a sys.stdout.isatty(). Se parchea ANTES
# de importar uvicorn, redirigiendo a un sumidero.
if sys.stdout is None:
    sys.stdout = open(os.devnull, "w")
if sys.stderr is None:
    sys.stderr = open(os.devnull, "w")


def _carpeta_base() -> Path:
    if getattr(sys, "frozen", False):
        return Path(sys.executable).parent
    return Path(__file__).parent


def _asegurar_secret_key() -> None:
    if os.environ.get("BARRA_SECRET_KEY"):
        return

    from cryptography.fernet import Fernet

    archivo_clave = _carpeta_base() / "barra_secret.key"
    if archivo_clave.exists():
        clave = archivo_clave.read_text(encoding="ascii").strip()
    else:
        clave = Fernet.generate_key().decode("ascii")
        archivo_clave.write_text(clave, encoding="ascii")

    os.environ["BARRA_SECRET_KEY"] = clave


if __name__ == "__main__":
    _asegurar_secret_key()

    import uvicorn
    from app.main import app

    uvicorn.run(app, host="127.0.0.1", port=8000, reload=False, use_colors=False)