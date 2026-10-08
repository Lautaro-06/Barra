# Compilación y empaquetado de la app de escritorio

Cómo correr en desarrollo, compilar y publicar las dos piezas de la app de escritorio:

| Pieza | Carpeta | Se publica como |
|---|---|---|
| Backend local (Python) | `application/barra-backend` | `barra-backend-vX.Y.Z.exe` |
| GUI (Java) | `application/barra-gui` | `barra-gui-vX.Y.Z.jar` |

Los dos archivos se publican en `barraPagina/barraWeb/public/downloads/`, desde donde los sirve la
web de venta.

> Los comandos de esta guía se verificaron el 08/10/2026 (ver CG-12 y CG-13 en el
> [plan de pruebas](../proyecto/plan-de-pruebas.md)). El ejecutable de Windows se genera con los
> mismos comandos en una PC con Windows.

---

## 1. Backend local (Python)

### 1.1 Requisitos

- Python 3.10 o superior (el código usa anotaciones `str | None`). El `.exe` publicado se
  regeneró el 08/10/2026, con la corrección de DEF-01, usando Python 3.12.10 y PyInstaller 6.22 (ver §1.6).
- Dependencias: `requirements.txt` (FastAPI, uvicorn, Pydantic, cryptography).

### 1.2 Correr en desarrollo

```bash
cd application/barra-backend
python -m venv venv
source venv/bin/activate          # Windows: venv\Scripts\activate
pip install -r requirements.txt
python run_backend.py
```

- Queda en `http://127.0.0.1:8000`; la documentación interactiva de la API está en `http://127.0.0.1:8000/docs`.
- `run_backend.py` es el mismo punto de entrada que usa el `.exe`: si no existe la variable
  `BARRA_SECRET_KEY`, genera la clave de cifrado en `barra_secret.key` (junto a `barra.db`) y la reutiliza.

**Con recarga automática** (útil mientras se programa), hay que dar la clave a mano:

```bash
# Generar una clave una sola vez y guardarla
python -c "from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())"

# Linux/macOS
export BARRA_SECRET_KEY="<la clave generada>"
# Windows (PowerShell)
$env:BARRA_SECRET_KEY = "<la clave generada>"

uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

Sin `BARRA_SECRET_KEY`, guardar o leer la contraseña SMTP responde 503. Si se cambia la clave,
hay que volver a escribir la contraseña SMTP desde la GUI.

### 1.3 Variables de entorno

| Variable | Por defecto | Para qué |
|---|---|---|
| `BARRA_SECRET_KEY` | (la genera `run_backend.py`) | Clave Fernet para cifrar la contraseña SMTP |
| `BARRA_STOCK_CHECK_INTERVAL` | `30` | Segundos entre chequeos del hilo de stock |
| `BARRA_STOCK_MINIMO` | `5` | Umbral de respaldo, solo si no existiera la fila de configuración |
| `BARRA_EMAIL_RETRY_SECONDS` | `300` | Espera antes de reintentar un email que falló |
| `BARRA_RESUMEN_CHECK_INTERVAL` | `30` | Segundos entre chequeos del hilo del resumen diario |
| `BARRA_BACKUP_INTERVAL_SECONDS` | `14400` (4 h) | Intervalo entre backups |
| `BARRA_BACKUP_MAX_COPIES` | `5` | Copias que se conservan |
| `BARRA_BACKUP_DIR` | `backups/` junto a `barra.db` | Carpeta de los backups |

### 1.4 Generar el ejecutable (PyInstaller)

En una PC con **Windows 10/11 de 64 bits** (PyInstaller genera ejecutables para el sistema en el
que corre):

```bash
cd application/barra-backend
venv\Scripts\activate
pip install -r requirements.txt pyinstaller
pyinstaller --onefile --name barra-backend run_backend.py
```

- Resultado: `dist\barra-backend.exe` (entre 19 y 23 MB según la versión de Python). No necesita opciones extra: PyInstaller incluye uvicorn y FastAPI solo.
- Es una app de **consola**: al abrirla muestra una ventana negra con el log. Si se quisiera sin
  ventana, se agrega `--windowed`; `run_backend.py` ya contempla ese caso (redirige la salida),
  pero entonces no hay forma visible de cerrarlo.
- Al ejecutarse, crea `barra.db`, `barra_secret.key` y `backups\` **junto al `.exe`** (no en la
  carpeta temporal de PyInstaller), así los datos sobreviven entre ejecuciones.
- PyInstaller deja `build\`, `dist\` y `barra-backend.spec`; los tres están en el `.gitignore` del backend.

### 1.5 Probar el ejecutable

1. Copiarlo a una carpeta vacía y ejecutarlo.
2. Esperar la línea `Uvicorn running on http://127.0.0.1:8000`.
3. Abrir `http://127.0.0.1:8000/health` en el navegador: tiene que responder `{"status":"ok"}`.
4. Verificar que aparecieron `barra.db`, `barra_secret.key` y `backups\`.

### 1.6 Generarlo desde Linux (con Wine)

Así se generó el `.exe` publicado con la corrección de DEF-01. PyInstaller no genera ejecutables
de Windows desde Linux, pero sí desde un Python de Windows corriendo en Wine:

```bash
sudo apt-get install wine wine64
# Python 3.12 para Windows, sin instalador (build independiente que usa uv)
curl -LO "https://github.com/astral-sh/python-build-standalone/releases/download/20250409/cpython-3.12.10%2B20250409-x86_64-pc-windows-msvc-install_only.tar.gz"
tar -xzf cpython-3.12.10+20250409-x86_64-pc-windows-msvc-install_only.tar.gz   # crea python/
wine python/python.exe -m pip install -r application/barra-backend/requirements.txt pyinstaller
cd application/barra-backend
wine ../../python/python.exe -m PyInstaller --onefile --name barra-backend run_backend.py 2>&1 | cat
```

- El `| cat` final no es decorativo: si la salida se redirige directo a un archivo, Python en Wine
  falla con `init_sys_streams: can't initialize sys standard streams`.
- Se puede probar en el mismo Linux con `wine dist/barra-backend.exe` (pasos de §1.5). Igual
  conviene abrirlo una vez en una PC con Windows antes de publicarlo.

---

## 2. GUI (Java)

### 2.1 Requisitos

- JDK 17 o superior y Maven 3.8 o superior.
- Sin dependencias externas: HTTP con `java.net.http` y un parser JSON propio (`Json.java`).

### 2.2 Correr en desarrollo

- **Eclipse:** File → Import → Maven → Existing Maven Projects → elegir `application/barra-gui` → Run As → Java Application sobre `Main.java`.
- **IntelliJ IDEA:** Open → elegir `application/barra-gui/pom.xml` → Run sobre `Main`.
- **Terminal:** `mvn package` y después `java -jar target/barra-gui-1.0.0.jar`.

El backend tiene que estar corriendo en `127.0.0.1:8000` (la URL está fija en `ApiClient.java`).

### 2.3 Generar el `.jar`

```bash
cd application/barra-gui
mvn package
```

- Resultado: `target/barra-gui-1.0.0.jar` (≈ 100 KB), con `Main-Class: com.barra.gui.Main` en el manifiesto.
- Compilado para Java 17 (`maven.compiler.source` y `maven.compiler.target` en 17): corre en Java 17, 21 o superior.
- Se abre con doble clic (si Java está instalado) o con `java -jar barra-gui-1.0.0.jar`.

---

## 3. Publicar una versión

1. Actualizar el número de versión:
   - `application/barra-gui/pom.xml` → `<version>`.
   - Los nombres de los archivos publicados (`-vX.Y.Z`).
2. Generar el `.exe` (§1.4) y el `.jar` (§2.3).
3. Renombrarlos y copiarlos a la web:

   ```text
   barraPagina/barraWeb/public/downloads/barra-backend-vX.Y.Z.exe
   barraPagina/barraWeb/public/downloads/barra-gui-vX.Y.Z.jar
   ```

4. Borrar los de la versión anterior de esa carpeta (o dejarlos si se quiere ofrecer la descarga de versiones viejas).
5. Correr la campaña de pruebas (`pruebas/README.md`).
6. Registrar los cambios en `CHANGELOG.md`.
7. Desplegar la web (ver [despliegue de la web](despliegue-web.md)) y, si cambió el link, actualizar `DOWNLOAD_URL`.

---

## 4. Siguiente paso: instalador único (pendiente)

Objetivo de la próxima versión (RNF04): un instalador que no pida instalar Java y una GUI que
abra el backend sola.

1. **Que la GUI lance el backend:** al iniciar, `Main.java` busca `barra-backend.exe` en su misma
   carpeta, lo ejecuta con `ProcessBuilder` si `/health` no responde y lo cierra al salir.
2. **Instalador con `jpackage`** (incluido en el JDK 17+; en Windows requiere WiX Toolset):

   ```bash
   # Carpeta "entrada" con barra-gui-X.Y.Z.jar y barra-backend.exe
   jpackage --type exe --name Barra --app-version X.Y.Z ^
            --input entrada --main-jar barra-gui-X.Y.Z.jar --main-class com.barra.gui.Main ^
            --win-shortcut --win-menu --win-dir-chooser
   ```

   Genera un instalador `.exe` que incluye el runtime de Java, crea accesos directos y deja el
   backend al lado de la GUI.

> Este paso es una propuesta: todavía no se implementó ni se probó.
