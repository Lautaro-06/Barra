"""Campaña de pruebas del backend de escritorio de Barra (caja negra, por HTTP).

Trabaja sobre una COPIA TEMPORAL de application/barra-backend: nunca toca
el barra.db real. Levanta un servidor SMTP local con STARTTLS para probar
el email de prueba, las alertas de stock y el resumen diario.

Uso (desde la raíz del repo, con el venv del backend + pruebas/requirements.txt):
    python pruebas/test_escritorio.py [salida.json]
Requiere que el puerto 8000 (backend) y el 8025 (SMTP de prueba) estén libres.
"""
import datetime as dt
import ipaddress
import json
import os
import shutil
import sqlite3
import ssl
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
from pathlib import Path

from aiosmtpd.controller import Controller
from aiosmtpd.smtp import AuthResult, LoginPassword
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID

HERE = Path(__file__).resolve().parent
REPO = HERE.parent
SALIDA = sys.argv[1] if len(sys.argv) > 1 else str(HERE / "resultados" / "escritorio.json")
URL = "http://127.0.0.1:8000"
PY = sys.executable

TMP = Path(tempfile.mkdtemp(prefix="barra-pruebas-"))
BASE = str(TMP / "barra-backend")
shutil.copytree(REPO / "application" / "barra-backend", BASE,
                ignore=shutil.ignore_patterns("venv", ".venv", "barra.db", "barra_secret.key", "backups", "__pycache__"))


def _certificado_autofirmado(carpeta: Path):
    """Certificado para 127.0.0.1, válido 2 días, solo para el SMTP de prueba."""
    clave = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    nombre = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "127.0.0.1")])
    ahora = dt.datetime.now(dt.timezone.utc)
    cert = (x509.CertificateBuilder().subject_name(nombre).issuer_name(nombre)
            .public_key(clave.public_key()).serial_number(x509.random_serial_number())
            .not_valid_before(ahora - dt.timedelta(minutes=5)).not_valid_after(ahora + dt.timedelta(days=2))
            .add_extension(x509.SubjectAlternativeName([x509.IPAddress(ipaddress.ip_address("127.0.0.1"))]), critical=False)
            .add_extension(x509.BasicConstraints(ca=True, path_length=None), critical=True)
            .sign(clave, hashes.SHA256()))
    (carpeta / "smtp.crt").write_bytes(cert.public_bytes(serialization.Encoding.PEM))
    (carpeta / "smtp.key").write_bytes(clave.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.TraditionalOpenSSL, serialization.NoEncryption()))
    return str(carpeta / "smtp.crt"), str(carpeta / "smtp.key")


CERT, KEY = _certificado_autofirmado(TMP)

# ---------- SMTP de prueba (STARTTLS + AUTH) ----------
correos = []


class Handler:
    async def handle_DATA(self, server, session, envelope):
        texto = envelope.content.decode("utf-8", "replace")
        asunto = ""
        for linea in texto.splitlines():
            if linea.lower().startswith("subject:"):
                asunto = linea[8:].strip()
                break
        correos.append({"para": envelope.rcpt_tos, "asunto": asunto, "texto": texto})
        return "250 OK"


def autenticador(server, session, envelope, mechanism, auth_data):
    ok = isinstance(auth_data, LoginPassword) and auth_data.password == b"clave-correcta"
    return AuthResult(success=ok, handled=False)


tls = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)
tls.load_cert_chain(CERT, KEY)
smtp = Controller(
    Handler(), hostname="127.0.0.1", port=8025, tls_context=tls,
    require_starttls=True, authenticator=autenticador, auth_required=True,
)
smtp.start()

# ---------- helpers ----------
def req(metodo, ruta, cuerpo=None):
    data = json.dumps(cuerpo).encode() if cuerpo is not None else None
    r = urllib.request.Request(URL + ruta, data=data, method=metodo,
                               headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(r, timeout=30) as resp:
            txt = resp.read().decode()
            return resp.status, (json.loads(txt) if txt else None)
    except urllib.error.HTTPError as e:
        txt = e.read().decode()
        try:
            return e.code, json.loads(txt)
        except ValueError:
            return e.code, txt


proc = None


def arrancar():
    global proc
    env = dict(os.environ, BARRA_STOCK_CHECK_INTERVAL="1", BARRA_RESUMEN_CHECK_INTERVAL="1",
               BARRA_EMAIL_RETRY_SECONDS="3")
    env.pop("BARRA_SECRET_KEY", None)
    proc = subprocess.Popen([PY, str(HERE / "lanzar_backend.py"), BASE, CERT], env=env,
                            stdout=open(TMP / "backend.log", "a"), stderr=subprocess.STDOUT)
    for _ in range(60):
        try:
            if req("GET", "/health")[0] == 200:
                return
        except Exception:
            pass
        time.sleep(0.25)
    raise RuntimeError("el backend no arrancó")


def detener():
    proc.terminate()
    proc.wait(timeout=15)


def esperar(cond, segundos=10):
    fin = time.time() + segundos
    while time.time() < fin:
        if cond():
            return True
        time.sleep(0.2)
    return False


resultados = []


def caso(cid, req_id, nombre, esperado, fn):
    try:
        ok, obtenido = fn()
    except Exception as exc:  # el caso falla, no la campaña
        ok, obtenido = False, f"Excepción: {exc!r}"
    resultados.append({"id": cid, "req": req_id, "nombre": nombre, "esperado": esperado,
                       "obtenido": obtenido, "resultado": "Pasó" if ok else "Falló"})
    print(f"{cid:7} {'OK ' if ok else 'XX '} {nombre} -> {obtenido}")


# ---------- campaña ----------
arrancar()

# Datos semilla
caso("CP-01", "RNF03", "Primer arranque crea la base con datos de ejemplo",
     "3 productos, 6 mesas, local 'Mi local'",
     lambda: (lambda p, m, c: (len(p) == 3 and len(m) == 6 and c["nombre_local"] == "Mi local",
                               f"{len(p)} productos, {len(m)} mesas, local '{c['nombre_local']}'"))(
         req("GET", "/productos")[1], req("GET", "/mesas")[1], req("GET", "/configuracion")[1]))

caso("CP-02", "RF13", "Backup automático al arrancar",
     "Un archivo barra_backup_*.db y una línea OK en backups.log",
     lambda: (lambda archivos, log: (len(archivos) >= 1 and "| OK |" in log,
                                     f"{len(archivos)} backup(s); log: {log.strip().splitlines()[-1] if log else '(vacío)'}"))(
         list(Path(BASE, "backups").glob("barra_backup_*.db")),
         Path(BASE, "backups", "backups.log").read_text() if Path(BASE, "backups", "backups.log").exists() else ""))

# Productos (ABM)
def cp_alta():
    s, p = req("POST", "/productos", {"nombre": "Empanada", "precio": 1500, "stock": 20})
    return s == 201 and p["disponible"] is True and p["umbral_stock"] is None, f"HTTP {s}, id {p.get('id')}, disponible={p.get('disponible')}"
caso("CP-03", "RF08", "Alta de producto", "HTTP 201, disponible por defecto, sin umbral propio", cp_alta)

caso("CP-04", "RF08", "Alta con precio 0 se rechaza", "HTTP 422",
     lambda: (lambda r: (r[0] == 422, f"HTTP {r[0]}"))(req("POST", "/productos", {"nombre": "X", "precio": 0})))

def cp_mod():
    pid = [p for p in req("GET", "/productos")[1] if p["nombre"] == "Empanada"][0]["id"]
    s, p = req("PATCH", f"/productos/{pid}", {"precio": 1600, "umbral_stock": 3})
    s2, p2 = req("PATCH", f"/productos/{pid}", {"umbral_stock": None})
    return (s == 200 and p["precio"] == 1600 and p["umbral_stock"] == 3 and s2 == 200 and p2["umbral_stock"] is None and p2["precio"] == 1600,
            f"precio {p['precio']}, umbral 3 -> {p2['umbral_stock']} (vuelve al global), resto sin cambios")
caso("CP-05", "RF08", "Modificación parcial de producto (precio y umbral)", "Cambia solo lo enviado; umbral null = usar el global", cp_mod)

def cp_baja():
    pid = [p for p in req("GET", "/productos")[1] if p["nombre"] == "Empanada"][0]["id"]
    req("PATCH", f"/productos/{pid}", {"disponible": False})
    s, r = req("POST", "/pedidos", {"detalles": [{"producto_id": pid, "cantidad": 1}]})
    req("PATCH", f"/productos/{pid}", {"disponible": True})
    return s == 400 and "no está disponible" in r["detail"], f"HTTP {s}: {r['detail']}"
caso("CP-06", "RF08", "Baja lógica: producto no disponible no se puede vender", "HTTP 400 «no está disponible»", cp_baja)

caso("CP-07", "RF08", "Producto inexistente", "HTTP 404",
     lambda: (lambda r: (r[0] == 404, f"HTTP {r[0]}"))(req("PATCH", "/productos/9999", {"precio": 10})))

# Pedidos
def cp_pedido():
    prods = {p["nombre"]: p for p in req("GET", "/productos")[1]}
    h, g = prods["Hamburguesa clásica"], prods["Gaseosa 500ml"]
    s, p = req("POST", "/pedidos", {"nota": "sin cebolla", "detalles": [
        {"producto_id": h["id"], "cantidad": 2}, {"producto_id": g["id"], "cantidad": 1}]})
    esperado = h["precio"] * 2 + g["precio"]
    prods2 = {x["nombre"]: x for x in req("GET", "/productos")[1]}
    ok = (s == 201 and p["estado"] == "en_preparacion" and p["total"] == esperado and p["nota"] == "sin cebolla"
          and prods2["Hamburguesa clásica"]["stock"] == h["stock"] - 2 and prods2["Gaseosa 500ml"]["stock"] == g["stock"] - 1)
    return ok, (f"HTTP {s}, total {p['total']} (esperado {esperado}), estado {p['estado']}, "
                f"stock hamburguesa {h['stock']}→{prods2['Hamburguesa clásica']['stock']}, gaseosa {g['stock']}→{prods2['Gaseosa 500ml']['stock']}")
caso("CP-08", "RF01, RF02, RF05", "Registrar pedido de mostrador con nota", "HTTP 201, total = Σ precio×cantidad, estado en_preparacion, stock descontado", cp_pedido)

def cp_sin_stock():
    pid = [p for p in req("GET", "/productos")[1] if p["nombre"] == "Papas fritas"][0]["id"]
    s, r = req("POST", "/pedidos", {"detalles": [{"producto_id": pid, "cantidad": 999}]})
    return s == 400 and "Stock insuficiente" in r["detail"], f"HTTP {s}: {r['detail']}"
caso("CP-09", "RF05", "Pedido con más cantidad que el stock", "HTTP 400 «Stock insuficiente…» y no se registra nada", cp_sin_stock)

caso("CP-10", "RF01", "Pedido con cantidad 0", "HTTP 422",
     lambda: (lambda r: (r[0] == 422, f"HTTP {r[0]}"))(req("POST", "/pedidos", {"detalles": [{"producto_id": 1, "cantidad": 0}]})))

def cp_estados():
    pid = req("GET", "/pedidos")[1][0]["id"]
    a = req("PATCH", f"/pedidos/{pid}/estado", {"estado": "listo"})
    b = req("PATCH", f"/pedidos/{pid}/estado", {"estado": "entregado"})
    c = req("PATCH", f"/pedidos/{pid}/estado", {"estado": "cancelado"})
    return (a[0] == 200 and a[1]["estado"] == "listo" and b[1]["estado"] == "entregado" and c[0] == 400,
            f"listo HTTP {a[0]}, entregado HTTP {b[0]}, 'cancelado' HTTP {c[0]}")
caso("CP-11", "RF03", "Cambio de estado del pedido", "en_preparacion→listo→entregado OK; estado inválido HTTP 400", cp_estados)

caso("CP-12", "RF04", "Listado de pedidos para cajero y cocina", "GET /pedidos devuelve todos con estado, detalles y total",
     lambda: (lambda r: (r[0] == 200 and all({"estado", "detalles", "total"} <= set(p) for p in r[1]), f"HTTP {r[0]}, {len(r[1])} pedido(s) con estado/detalles/total"))(req("GET", "/pedidos")))

# Mesas
def cp_mesa_ciclo():
    prods = {p["nombre"]: p for p in req("GET", "/productos")[1]}
    mid = req("GET", "/mesas")[1][0]["id"]
    a = req("POST", f"/mesas/{mid}/abrir")
    r1 = req("POST", f"/mesas/{mid}/pedidos", {"detalles": [{"producto_id": prods["Papas fritas"]["id"], "cantidad": 2}]})
    r2 = req("POST", f"/mesas/{mid}/pedidos", {"nota": "postre", "detalles": [{"producto_id": prods["Gaseosa 500ml"]["id"], "cantidad": 1}]})
    m = [x for x in req("GET", "/mesas")[1] if x["id"] == mid][0]
    cuenta = req("GET", f"/mesas/{mid}/cuenta")[1]
    cierre = req("POST", f"/mesas/{mid}/cerrar")
    m2 = [x for x in req("GET", "/mesas")[1] if x["id"] == mid][0]
    esperado = prods["Papas fritas"]["precio"] * 2 + prods["Gaseosa 500ml"]["precio"]
    ok = (a[1]["estado"] == "ocupada" and r1[0] == 201 and r2[0] == 201 and r1[1]["mesa_nombre"] == m["nombre"]
          and m["total_actual"] == esperado and len(cuenta["pedidos"]) == 2 and cierre[0] == 200
          and cierre[1]["estado"] == "cerrada" and cierre[1]["total"] == esperado and cierre[1]["fecha_cierre"] and m2["estado"] == "libre")
    return ok, (f"abrir→{a[1]['estado']}, 2 rondas, total mesa {m['total_actual']} (esperado {esperado}), "
                f"cierre HTTP {cierre[0]} estado {cierre[1]['estado']}, mesa vuelve a {m2['estado']}")
caso("CP-13", "RF09", "Ciclo completo de una mesa: abrir, 2 rondas, cerrar", "Mesa ocupada→libre, cuenta con 2 rondas y total correcto, ticket con fecha de cierre", cp_mesa_ciclo)

caso("CP-14", "RF09", "Ronda en mesa sin cuenta abierta", "HTTP 400 «abrila primero»",
     lambda: (lambda r: (r[0] == 400 and "abrila primero" in r[1]["detail"], f"HTTP {r[0]}: {r[1]['detail']}"))(
         req("POST", f"/mesas/{req('GET', '/mesas')[1][1]['id']}/pedidos", {"detalles": [{"producto_id": 1, "cantidad": 1}]})))

def cp_borrar_ocupada():
    mid = req("GET", "/mesas")[1][2]["id"]
    req("POST", f"/mesas/{mid}/abrir")
    s, r = req("DELETE", f"/mesas/{mid}")
    req("POST", f"/mesas/{mid}/cerrar")
    return s == 400, f"HTTP {s}: {r['detail'] if isinstance(r, dict) else r}"
caso("CP-15", "RF09", "Eliminar mesa ocupada", "HTTP 400 «No se puede borrar una mesa con la cuenta abierta»", cp_borrar_ocupada)

def cp_alta_baja_mesa():
    s, m = req("POST", "/mesas", {"nombre": "Vereda 1"})
    s2, _ = req("DELETE", f"/mesas/{m['id']}")
    return s == 201 and s2 == 204, f"alta HTTP {s}, baja HTTP {s2}"
caso("CP-16", "RF09", "Alta y baja de una mesa nunca usada", "HTTP 201 y HTTP 204", cp_alta_baja_mesa)

def cp_borrar_usada():
    mid = req("GET", "/mesas")[1][0]["id"]  # la mesa de CP-13 (ya tuvo una cuenta)
    s, r = req("DELETE", f"/mesas/{mid}")
    return s in (204, 400), f"HTTP {s}: {r.get('detail') if isinstance(r, dict) else r}"
caso("CP-17", "RF09", "Eliminar mesa libre que ya tuvo cuentas", "HTTP 204 (o 400 con un motivo claro)", cp_borrar_usada)

# Configuración
caso("CP-18", "RF10", "Datos del dueño con email inválido", "HTTP 422",
     lambda: (lambda r: (r[0] == 422, f"HTTP {r[0]}"))(req("PUT", "/admin", {"nombre_dueno": "Ana", "email_dueno": "no-es-email"})))

def cp_destino():
    req("PUT", "/admin", {"nombre_dueno": "Ana", "email_dueno": "duena@local.test", "telefono": None})
    c = req("GET", "/configuracion")[1]
    return c["email_destino"] is None and c["email_destino_efectivo"] == "duena@local.test", f"email_destino {c['email_destino']}, efectivo {c['email_destino_efectivo']}"
caso("CP-19", "RF10", "Sin email de destino, los mails van al dueño", "email_destino_efectivo = email del dueño", cp_destino)

caso("CP-20", "RF11", "Habilitar email sin SMTP completo", "HTTP 400 con los campos que faltan",
     lambda: (lambda r: (r[0] == 400 and "smtp_host" in r[1]["detail"], f"HTTP {r[0]}: {r[1]['detail']}"))(req("PUT", "/configuracion", {"email_habilitado": True})))

caso("CP-21", "RF11", "Hora de resumen inválida", "HTTP 422",
     lambda: (lambda r: (r[0] == 422, f"HTTP {r[0]}"))(req("PUT", "/configuracion", {"resumen_diario_hora": "25:00"})))

def cp_smtp_mal():
    req("PUT", "/configuracion", {"smtp_host": "127.0.0.1", "smtp_port": 8025, "smtp_usuario": "barra@local.test", "smtp_password": "mala"})
    s, r = req("POST", "/configuracion/probar-email")
    return s == 400 and "rechazó el usuario/contraseña" in r["detail"], f"HTTP {s}: {r['detail']}"
caso("CP-22", "RF11", "Email de prueba con contraseña incorrecta", "HTTP 400 con el aviso de contraseña (y la pista de Gmail)", cp_smtp_mal)

def cp_smtp_ok():
    n = len(correos)
    s0, c = req("PUT", "/configuracion", {"smtp_password": "clave-correcta"})
    s, r = req("POST", "/configuracion/probar-email")
    ok = s == 200 and r["enviado_a"] == "duena@local.test" and esperar(lambda: len(correos) > n, 5) and "Email de prueba" in correos[-1]["asunto"]
    return ok, f"HTTP {s}, enviado a {r.get('enviado_a')}, asunto «{correos[-1]['asunto'] if len(correos) > n else '-'}»"
caso("CP-23", "RF11", "Email de prueba con STARTTLS y credenciales correctas", "HTTP 200 y el mail llega al servidor SMTP", cp_smtp_ok)

def cp_cifrado():
    con = sqlite3.connect(Path(BASE, "barra.db"))
    v = con.execute("SELECT smtp_password_cifrada FROM configuracion").fetchone()[0]
    con.close()
    c = req("GET", "/configuracion")[1]
    return (v != "clave-correcta" and v.startswith("gAAAA") and "smtp_password" not in c and c["smtp_password_configurada"] is True,
            f"en la base: {v[:12]}… (Fernet); la API solo devuelve smtp_password_configurada={c['smtp_password_configurada']}")
caso("CP-24", "RNF07", "La contraseña SMTP se guarda cifrada y nunca se devuelve", "Valor Fernet en la base; la API no la expone", cp_cifrado)

# Alertas de stock
def cp_alerta():
    s, p = req("POST", "/productos", {"nombre": "Milanesa", "precio": 8900, "stock": 6})
    req("PUT", "/configuracion", {"email_habilitado": True, "umbral_stock_global": 5})
    time.sleep(2.5)
    n = len(correos)
    req("POST", "/pedidos", {"detalles": [{"producto_id": p["id"], "cantidad": 2}]})  # 6 -> 4
    llego = esperar(lambda: any("Stock bajo: Milanesa" in c["asunto"] for c in correos[n:]), 8)
    alertas = req("GET", "/alertas")[1]
    return (llego and any(a["nombre"] == "Milanesa" and a["stock"] == 4 for a in alertas),
            f"mail «{[c['asunto'] for c in correos[n:]]}»; /alertas incluye Milanesa con stock 4")
caso("CP-25", "RF06", "Alerta por email al bajar del umbral", "Un mail «[local] Stock bajo: Milanesa» en menos de 30 s y la alerta en /alertas", cp_alerta)

def cp_no_repite():
    pid = [p for p in req("GET", "/productos")[1] if p["nombre"] == "Milanesa"][0]["id"]
    n = len(correos)
    req("POST", "/pedidos", {"detalles": [{"producto_id": pid, "cantidad": 1}]})  # 4 -> 3
    time.sleep(4)
    repetidos = [c["asunto"] for c in correos[n:] if "Milanesa" in c["asunto"]]
    return not repetidos, f"{len(repetidos)} mails nuevos por Milanesa"
caso("CP-26", "RF06", "La alerta no se repite mientras sigue bajo", "0 mails nuevos", cp_no_repite)

def cp_rearma():
    pid = [p for p in req("GET", "/productos")[1] if p["nombre"] == "Milanesa"][0]["id"]
    req("PATCH", f"/productos/{pid}", {"stock": 10})
    time.sleep(3)
    n = len(correos)
    req("POST", "/pedidos", {"detalles": [{"producto_id": pid, "cantidad": 7}]})  # 10 -> 3
    llego = esperar(lambda: any("Milanesa" in c["asunto"] for c in correos[n:]), 8)
    return llego, "volvió a llegar la alerta después de reponer" if llego else "no llegó"
caso("CP-27", "RF06", "Después de reponer, la alerta vuelve a funcionar", "Llega un nuevo mail al volver a bajar", cp_rearma)

# Resumen diario
caso("CP-28", "RF07", "Resumen de ventas (consulta)", "Total, pedidos, mostrador/mesas y productos más vendidos",
     lambda: (lambda r: (r[0] == 200 and r[1]["cantidad_pedidos"] > 0 and r[1]["productos_mas_vendidos"] and r[1]["mesas"]["cuentas_cerradas"] >= 1,
                         f"{r[1]['cantidad_pedidos']} pedidos, total {r[1]['total_vendido']}, más vendido: {r[1]['productos_mas_vendidos'][0]['nombre']}, cuentas cerradas {r[1]['mesas']['cuentas_cerradas']}"))(req("GET", "/resumen-diario")))

def cp_resumen_ahora():
    n = len(correos)
    s, r = req("POST", "/resumen-diario/enviar")
    llego = esperar(lambda: any("Resumen de ventas" in c["asunto"] for c in correos[n:]), 5)
    return s == 200 and llego, f"HTTP {s}, mail «{correos[-1]['asunto'] if llego else '-'}»"
caso("CP-29", "RF07", "Enviar resumen ahora", "HTTP 200 y llega el mail con el resumen", cp_resumen_ahora)

def cp_resumen_auto():
    n = len(correos)
    hora = datetime.now().strftime("%H:%M")
    req("PUT", "/configuracion", {"resumen_diario_habilitado": True, "resumen_diario_hora": hora})
    llego = esperar(lambda: sum("Resumen de ventas" in c["asunto"] for c in correos[n:]) >= 1, 8)
    time.sleep(4)
    cantidad = sum("Resumen de ventas" in c["asunto"] for c in correos[n:])
    return llego and cantidad == 1, f"programado a las {hora}: llegaron {cantidad} resumen(es) en 12 s"
caso("CP-30", "RF12", "Resumen diario automático a la hora programada", "Llega exactamente 1 resumen", cp_resumen_auto)

# No funcionales
def cp_latencia():
    tiempos = []
    for _ in range(100):
        t = time.perf_counter(); req("GET", "/productos"); tiempos.append(time.perf_counter() - t)
    pid = [p for p in req("GET", "/productos")[1] if p["nombre"] == "Café"]
    if not pid:
        pid = [req("POST", "/productos", {"nombre": "Café", "precio": 2000, "stock": 1000})[1]]
    tp = []
    for _ in range(50):
        t = time.perf_counter(); req("POST", "/pedidos", {"detalles": [{"producto_id": pid[0]["id"], "cantidad": 1}]}); tp.append(time.perf_counter() - t)
    p95g = sorted(tiempos)[94] * 1000
    p95p = sorted(tp)[47] * 1000
    return max(tiempos + tp) < 1, f"GET /productos p95 {p95g:.1f} ms; POST /pedidos p95 {p95p:.1f} ms; máximo {max(tiempos + tp) * 1000:.1f} ms"
caso("CP-31", "RNF01", "Tiempo de respuesta", "Todas las respuestas en menos de 1 s", cp_latencia)

def cp_concurrencia():
    s, p = req("POST", "/productos", {"nombre": "Flan", "precio": 3000, "stock": 10})
    antes = len(req("GET", "/pedidos")[1])
    with ThreadPoolExecutor(30) as ex:
        codigos = list(ex.map(lambda _: req("POST", "/pedidos", {"detalles": [{"producto_id": p["id"], "cantidad": 1}]})[0], range(30)))
    stock = [x for x in req("GET", "/productos")[1] if x["id"] == p["id"]][0]["stock"]
    nuevos = len(req("GET", "/pedidos")[1]) - antes
    ok = codigos.count(201) == 10 and codigos.count(400) == 20 and stock == 0 and nuevos == 10
    return ok, f"30 pedidos simultáneos de 1 unidad con stock 10: {codigos.count(201)} aceptados, {codigos.count(400)} rechazados, stock final {stock}, pedidos nuevos {nuevos}"
caso("CP-32", "RNF05", "Pedidos concurrentes sin pérdida ni duplicación", "10 aceptados, 20 rechazados, stock 0, 10 pedidos", cp_concurrencia)

def cp_offline():
    req("PUT", "/configuracion", {"smtp_port": 1})  # puerto cerrado = sin conexión con el servidor de mail
    s, r = req("POST", "/configuracion/probar-email")
    pid = [p for p in req("GET", "/productos")[1] if p["nombre"] == "Café"][0]["id"]
    s2, _ = req("POST", "/pedidos", {"detalles": [{"producto_id": pid, "cantidad": 1}]})
    vivo = req("GET", "/health")[0]
    req("PUT", "/configuracion", {"smtp_port": 8025})
    return s == 400 and s2 == 201 and vivo == 200, f"email: HTTP {s} ({r['detail'][:60]}…); pedido: HTTP {s2}; backend sigue respondiendo"
caso("CP-33", "RNF02", "Sin conexión con el servidor de mail se sigue vendiendo", "El email falla con aviso; los pedidos funcionan", cp_offline)

def cp_persistencia():
    antes = len(req("GET", "/pedidos")[1])
    detener(); arrancar()
    despues = len(req("GET", "/pedidos")[1])
    nombre = req("GET", "/admin")[1]["nombre_dueno"]
    return antes == despues and nombre == "Ana", f"{antes} pedidos antes y {despues} después de reiniciar; dueño «{nombre}»"
caso("CP-34", "RNF03", "Los datos persisten al reiniciar", "Mismos pedidos y configuración", cp_persistencia)

detener()
smtp.stop()
Path(SALIDA).parent.mkdir(parents=True, exist_ok=True)
json.dump(resultados, open(SALIDA, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
shutil.rmtree(TMP, ignore_errors=True)
print(f"\n{sum(r['resultado'] == 'Pasó' for r in resultados)}/{len(resultados)} casos pasaron; "
      f"{len(correos)} mails recibidos. Resultados en {SALIDA}")
