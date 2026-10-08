"""Campaña de pruebas del backend de la web de venta (Express + MySQL/MariaDB).

Usa una base PROPIA, `barra_web_pruebas`, que borra y vuelve a crear en cada
corrida: nunca toca `barra_web`. Los mails se leen de la consola del backend
(sin SMTP_HOST, el backend los imprime en vez de mandarlos).

Requisitos:
  - `npm ci` hecho en barraPagina/barraWebBackend
  - cliente `mysql` en el PATH y un usuario con permiso para crear y borrar
    `barra_web_pruebas`, indicado con las variables PRUEBAS_DB_USER y
    PRUEBAS_DB_PASSWORD (por defecto: barra / barra-dev)
  - el puerto 4000 libre

Uso (desde la raíz del repo): python pruebas/test_web.py [salida.json]
"""
import base64
import hashlib
import hmac
import json
import os
import re
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path

HERE = Path(__file__).resolve().parent
BASE = str(HERE.parent / "barraPagina" / "barraWebBackend")
SALIDA = sys.argv[1] if len(sys.argv) > 1 else str(HERE / "resultados" / "web.json")
URL = "http://127.0.0.1:4000/api"
Path(SALIDA).parent.mkdir(parents=True, exist_ok=True)
LOG = Path(tempfile.gettempdir()) / "barra-pruebas-web.log"  # contiene los mails de prueba: no se versiona

DB = "barra_web_pruebas"
DB_USER = os.environ.get("PRUEBAS_DB_USER", "barra")
DB_PASSWORD = os.environ.get("PRUEBAS_DB_PASSWORD", "barra-dev")
MYSQL = ["mysql", "-h127.0.0.1", f"-u{DB_USER}", f"-p{DB_PASSWORD}"]

subprocess.run(MYSQL + ["-e", f"DROP DATABASE IF EXISTS {DB}; CREATE DATABASE {DB} CHARACTER SET utf8mb4;"], check=True)

env = dict(os.environ, PORT="4000", PUBLIC_URL="http://localhost:5173", DB_HOST="127.0.0.1", DB_PORT="3306",
           DB_USER=DB_USER, DB_PASSWORD=DB_PASSWORD, DB_NAME=DB, MP_ACCESS_TOKEN="", MP_WEBHOOK_SECRET="secreto-webhook",
           SMTP_HOST="", DOWNLOAD_URL="http://localhost:5173/downloads", ADMIN_USER="admin", ADMIN_PASSWORD="clave-admin")
log = open(LOG, "w", encoding="utf-8")
proc = subprocess.Popen(["node", "src/app.js"], cwd=BASE, env=env, stdout=log, stderr=subprocess.STDOUT)


def req(metodo, ruta, cuerpo=None, headers=None):
    data = json.dumps(cuerpo).encode() if cuerpo is not None else None
    h = {"Content-Type": "application/json", **(headers or {})}
    r = urllib.request.Request(URL + ruta, data=data, method=metodo, headers=h)
    try:
        with urllib.request.urlopen(r, timeout=20) as resp:
            txt = resp.read().decode()
            try:
                return resp.status, json.loads(txt) if txt else None
            except ValueError:
                return resp.status, txt
    except urllib.error.HTTPError as e:
        txt = e.read().decode()
        try:
            return e.code, json.loads(txt)
        except ValueError:
            return e.code, txt


for _ in range(80):
    try:
        if req("GET", "/health")[0] == 200:
            break
    except Exception:
        time.sleep(0.25)


def consola():
    log.flush()
    return LOG.read_text()


ADMIN = {"Authorization": "Basic " + base64.b64encode(b"admin:clave-admin").decode()}
resultados = []
ctx = {}


def caso(cid, req_id, nombre, esperado, fn):
    try:
        ok, obtenido = fn()
    except Exception as exc:
        ok, obtenido = False, f"Excepción: {exc!r}"
    resultados.append({"id": cid, "req": req_id, "nombre": nombre, "esperado": esperado,
                       "obtenido": obtenido, "resultado": "Pasó" if ok else "Falló"})
    print(f"{cid:7} {'OK ' if ok else 'XX '} {nombre} -> {obtenido}")


def cp_planes():
    s, p = req("GET", "/planes")
    nombres = [(x["nombre"], x["precioArs"], x["diasRenovacion"], x["disponible"]) for x in p]
    return s == 200 and nombres == [("Gratis", 0, 10, True), ("Pro", 100, 30, False), ("Max", 150, 365, False)], f"HTTP {s}: {nombres}"
caso("CW-01", "RF14", "La base se crea sola con los 3 planes", "Gratis (10 días, disponible), Pro y Max no disponibles", cp_planes)

caso("CW-02", "RF14", "Compra sin email", "HTTP 400",
     lambda: (lambda r: (r[0] == 400, f"HTTP {r[0]}: {r[1]['error']}"))(req("POST", "/compras", {"plan_id": 1, "comprador": {"nombre": "Ana"}})))

caso("CW-03", "RF14", "Compra de un plan no disponible", "HTTP 409 y no se crea nada",
     lambda: (lambda r: (r[0] == 409, f"HTTP {r[0]}: {r[1]['error']}"))(req("POST", "/compras", {"plan_id": 2, "comprador": {"nombre": "Ana", "email": "ana@local.test"}})))


def cp_compra_gratis():
    s, r = req("POST", "/compras", {"plan_id": 1, "comprador": {"nombre": "Ana Pérez", "email": "ana@local.test"}})
    time.sleep(0.5)
    txt = consola()
    codigo = re.findall(r"Código de licencia: (BARRA-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4})", txt)
    secret = re.findall(r"Clave secreta: ([0-9a-f]{64})", txt)
    ctx["codigo"], ctx["secret"] = codigo[-1], secret[-1]
    return (s == 201 and r["redirect"] == "/pago-exitoso" and codigo and secret and "Tu licencia de Barra" in txt,
            f"HTTP {s} → {r['redirect']}; mail «Tu licencia de Barra» con código {codigo[-1]} y clave de 64 caracteres")
caso("CW-04", "RF14", "Compra del plan Gratis", "HTTP 201, redirige a /pago-exitoso, mail con código BARRA-XXXX-XXXX-XXXX y clave", cp_compra_gratis)


def cp_secret_hash():
    out = subprocess.run(MYSQL + ["-N", "-e", f"SELECT secret_hash, codigo FROM {DB}.licencias WHERE codigo='{ctx['codigo']}'"],
                         capture_output=True, text=True).stdout.split()
    return out[0].startswith("$2") and ctx["secret"] not in out[0], f"secret_hash = {out[0][:7]}… (bcrypt), la clave no se guarda en claro"
caso("CW-05", "RNF07", "La clave secreta se guarda hasheada", "Hash bcrypt en la base", cp_secret_hash)

caso("CW-06", "RF15", "Activar con clave incorrecta", "HTTP 401",
     lambda: (lambda r: (r[0] == 401, f"HTTP {r[0]}: {r[1]['error']}"))(req("POST", "/licencias/activar", {"codigo": ctx["codigo"], "secret": "0" * 64})))

caso("CW-07", "RF15", "Activar con código inexistente", "HTTP 404",
     lambda: (lambda r: (r[0] == 404, f"HTTP {r[0]}: {r[1]['error']}"))(req("POST", "/licencias/activar", {"codigo": "BARRA-AAAA-BBBB-CCCC", "secret": ctx["secret"]})))


def cp_activar():
    s, r = req("POST", "/licencias/activar", {"codigo": ctx["codigo"], "secret": ctx["secret"]})
    vence = datetime.fromisoformat(r["fecha_vencimiento"].replace("Z", "+00:00"))
    dias = (vence - datetime.now(vence.tzinfo)).days
    return s == 200 and r["valido"] and 9 <= dias <= 10, f"HTTP {s}, valido={r['valido']}, vence en {dias + 1} días"
caso("CW-08", "RF15", "Activar con código y clave correctos", "HTTP 200, valido=true, vence en 10 días (plan Gratis)", cp_activar)

caso("CW-09", "RF15", "Segunda activación (máximo 1)", "HTTP 403",
     lambda: (lambda r: (r[0] == 403, f"HTTP {r[0]}: {r[1]['error']}"))(req("POST", "/licencias/activar", {"codigo": ctx["codigo"], "secret": ctx["secret"]})))

caso("CW-10", "RF16", "Consultar estado de la licencia", "HTTP 200, valido=true",
     lambda: (lambda r: (r[0] == 200 and r[1]["valido"] is True, f"HTTP {r[0]}, valido={r[1]['valido']}"))(
         req("GET", f"/licencias/estado?codigo={ctx['codigo']}&secret={ctx['secret']}")))


def cp_recuperar():
    antes = consola().count("Tu código de licencia de Barra")
    s, r = req("POST", "/licencias/recuperar", {"email": "ana@local.test"})
    s2, r2 = req("POST", "/licencias/recuperar", {"email": "nadie@local.test"})
    time.sleep(0.5)
    txt = consola()
    despues = txt.count("Tu código de licencia de Barra")
    ultimo = txt[txt.rfind("Tu código de licencia de Barra"):]
    return (s == 200 and s2 == 200 and despues == antes + 1 and ctx["codigo"] in ultimo and ctx["secret"] not in ultimo,
            f"email registrado: HTTP {s} y 1 mail con el código (sin la clave); email desconocido: HTTP {s2} sin mail")
caso("CW-11", "RF17", "Recuperar licencia", "Mail solo con el código; misma respuesta si el email no existe", cp_recuperar)

caso("CW-12", "RF18", "Panel admin sin credenciales", "HTTP 401",
     lambda: (lambda r: (r[0] == 401, f"HTTP {r[0]}"))(req("GET", "/admin/ventas")))

caso("CW-13", "RF18", "Panel admin con contraseña incorrecta", "HTTP 401",
     lambda: (lambda r: (r[0] == 401, f"HTTP {r[0]}"))(req("GET", "/admin/ventas", headers={"Authorization": "Basic " + base64.b64encode(b"admin:x").decode()})))


def cp_admin():
    s, v = req("GET", "/admin/ventas", headers=ADMIN)
    s2, l = req("GET", "/admin/licencias", headers=ADMIN)
    return (s == 200 and s2 == 200 and v[0]["estado"] == "gratuito" and v[0]["comprador_email"] == "ana@local.test" and l[0]["codigo"] == ctx["codigo"],
            f"ventas: {len(v)} ({v[0]['estado']}, {v[0]['plan_nombre']}); licencias: {len(l)} ({l[0]['activaciones_usadas']}/{l[0]['max_activaciones']} activaciones)")
caso("CW-14", "RF18", "Panel admin: ventas y licencias", "Lista la venta gratuita y la licencia emitida", cp_admin)


def cp_revocar():
    lid = req("GET", "/admin/licencias", headers=ADMIN)[1][0]["id"]
    s, _ = req("POST", f"/admin/licencias/{lid}/revocar", headers=ADMIN)
    s2, e = req("GET", f"/licencias/estado?codigo={ctx['codigo']}&secret={ctx['secret']}")
    return s == 200 and e["valido"] is False, f"revocar HTTP {s}; estado después: valido={e['valido']}"
caso("CW-15", "RF18", "Revocar una licencia", "La licencia deja de ser válida", cp_revocar)


def cp_webhook_firma():
    pagos_antes = req("GET", "/admin/ventas", headers=ADMIN)[1]
    s, _ = req("POST", "/pagos/webhook?data.id=123", {"data": {"id": "123"}},
               headers={"x-signature": "ts=1,v1=" + "0" * 64, "x-request-id": "abc"})
    pagos_despues = req("GET", "/admin/ventas", headers=ADMIN)[1]
    return s == 200 and pagos_antes == pagos_despues and "firma inválida" in consola(), f"HTTP {s}, sin cambios en pagos, se registra «firma inválida»"
caso("CW-16", "RF19", "Webhook de Mercado Pago con firma inválida", "HTTP 200 (para que MP no reintente) y se ignora", cp_webhook_firma)


def cp_webhook_firma_ok():
    ts = str(int(time.time()))
    manifest = f"id:999;request-id:req-1;ts:{ts};"
    v1 = hmac.new(b"secreto-webhook", manifest.encode(), hashlib.sha256).hexdigest()
    s, _ = req("POST", "/pagos/webhook?data.id=999", {"data": {"id": "999"}}, headers={"x-signature": f"ts={ts},v1={v1}", "x-request-id": "req-1"})
    txt = consola()
    return s == 200 and "Error procesando webhook" in txt, f"HTTP {s}; la firma se acepta y se consulta el pago a MP (falla por no tener MP_ACCESS_TOKEN, se registra y responde 200)"
caso("CW-17", "RF19", "Webhook con firma válida", "Firma aceptada; se consulta el pago a Mercado Pago", cp_webhook_firma_ok)

proc.terminate()
proc.wait(timeout=10)
json.dump(resultados, open(SALIDA, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
print(f"\n{sum(r['resultado'] == 'Pasó' for r in resultados)}/{len(resultados)} casos pasaron. Resultados en {SALIDA}")
