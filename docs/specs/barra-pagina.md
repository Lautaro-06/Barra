---
titulo: Barra Web (barraPagina) — compra, licencias y panel de admin
fecha: 2026-09-03
actualizado: 2026-10-08
estado: implementado (v1.0.0)
---

# Barra Web (barraPagina)

## Contexto

`barraPagina` es la web pública donde dueños de otros locales obtienen una licencia de Barra.
Cuando se escribió esta especificación (03/09/2026) era un esqueleto vacío. Se implementó por
completo entre el 24/08 y el 04/09 (commit `790d895`) y se le sumaron las descargas el 08/10.

| Parte | Carpeta | Documentación |
|---|---|---|
| Frontend (React + Vite + Tailwind) | `barraPagina/barraWeb` | [README](../../barraPagina/barraWeb/README.md) |
| Backend (Node + Express + MySQL) | `barraPagina/barraWebBackend` | [README](../../barraPagina/barraWebBackend/README.md) |
| Despliegue | — | [despliegue de la web](../tecnico/despliegue-web.md) |
| Panel de administración | — | [manual del panel](../tecnico/manual-admin-web.md) |

## Alcance

**Adentro:** compra (Checkout Pro de Mercado Pago), emisión de licencia, entrega por mail,
recuperación de licencia, panel de admin básico, endpoints de activación y validación que la app
de escritorio va a consumir, y descarga de los ejecutables.

**Afuera (explícito):**

- La lógica de expiración y bloqueo del lado de la app de escritorio (`application/barra-backend`):
  acá solo se exponen los endpoints. Quedó registrada como **RF20** en el
  [documento del proyecto](../proyecto/documento-del-proyecto.md#91-requisitos-funcionales).
- Habilitar Pro/Max de verdad: el código está listo pero con `disponible=false` y precios de prueba.

La tecnología de la GUI de escritorio (Java Swing en lugar de Electron + React) ya se resolvió:
ver [ADR-001](../tecnico/decisiones.md#adr-001--gui-de-escritorio-en-java-swing).

## Planes (tabla `planes`)

| id | nombre | precio_ars | dias_renovacion | max_activaciones | disponible |
|----|--------|-----------|------------------|-------------------|------------|
| 1 | Gratis | 0 | 10 | 1 | true |
| 2 | Pro | 100 (precio de prueba; propuesto: 12.000) | 30 | 1 | false |
| 3 | Max | 150 (precio de prueba; propuesto: 120.000) | 365 | 1 | false |

El único diferenciador real entre planes es `dias_renovacion` (y el precio).
`disponible=false` bloquea la compra en el backend (409), no solo visualmente. Los precios
propuestos salen del [presupuesto](../proyecto/presupuesto.md).

## Flujo de compra

```text
POST /api/compras
body: { plan_id, comprador: { nombre, email } }
```

- `plan.disponible === false` → 409, no se crea nada.
- Siempre registra el `Comprador`.
- `plan.precio === 0` (Gratis) → esquiva Mercado Pago: genera la `Licencia`, registra un `Pago`
  con `monto=0, estado="gratuito"`, manda el mail y responde `{ redirect: "/pago-exitoso" }`.
- `plan.precio > 0` (Pro/Max) → guarda un `Pago` en estado `pendiente`, crea la preferencia en
  Mercado Pago (Checkout Pro, `external_reference` = id del pago) y responde con la URL de MP.

La fuente de verdad del pago es el **webhook**, no el redirect del navegador:

```text
POST /api/pagos/webhook
```

Valida la firma `x-signature` de Mercado Pago (HMAC-SHA256 con `MP_WEBHOOK_SECRET`), consulta el
estado real del pago en la API de MP y recién ahí, si está aprobado, genera la `Licencia`, marca
el pago `aprobado` y manda el mail. Si está rechazado, lo marca `rechazado`. Siempre responde 200
para que MP no reintente. `PagoExitoso.jsx` y `PagoFallido.jsx` son solo feedback visual.

## Licencias — seguridad

Cada licencia tiene **código** (identificador visible, ej. `BARRA-7F3A-9C1D-4E82`, sin caracteres
ambiguos como 0/O o 1/I) y **secret** (32 bytes, 64 caracteres hexadecimales). Ambos se mandan una
sola vez por mail. En la base, el secret se guarda **hasheado** (bcrypt): un volcado de la base no
permite reconstruir licencias válidas.

```text
POST /api/licencias/activar
body: { codigo, secret }
→ 404 si el código no existe, 401 si el secret no coincide, 403 si no quedan activaciones.
  Si es válida: incrementa activaciones_usadas, fija fecha_activacion y fecha_vencimiento
  y responde { valido: true, fecha_vencimiento }

GET /api/licencias/estado?codigo=...&secret=...
→ { valido, fecha_vencimiento }, para que la app re-valide periódicamente

POST /api/licencias/recuperar
body: { email }
→ reenvía el código (no el secret) al mail del comprador. Misma respuesta exista o no el email
```

`fecha_vencimiento` se calcula en la **activación**, no en la compra: si alguien tarda una semana
en instalar, no pierde días de su plan.

## Esquema (MySQL)

Las tablas se crean solas al arrancar el backend (`src/db.js`).

```sql
CREATE TABLE planes (
  id INT PRIMARY KEY AUTO_INCREMENT,
  nombre VARCHAR(20) NOT NULL,
  precio_ars DECIMAL(10,2) NOT NULL DEFAULT 0,
  dias_renovacion INT NOT NULL,
  max_activaciones INT NOT NULL DEFAULT 1,
  disponible BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE compradores (
  id INT PRIMARY KEY AUTO_INCREMENT,
  nombre VARCHAR(120) NOT NULL,
  email VARCHAR(160) NOT NULL,
  creado_en DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE licencias (
  id INT PRIMARY KEY AUTO_INCREMENT,
  comprador_id INT NOT NULL,
  plan_id INT NOT NULL,
  codigo VARCHAR(32) UNIQUE NOT NULL,
  secret_hash VARCHAR(60) NOT NULL,
  dias_renovacion INT NOT NULL,
  activaciones_usadas INT NOT NULL DEFAULT 0,
  max_activaciones INT NOT NULL,
  fecha_activacion DATETIME NULL,
  fecha_vencimiento DATETIME NULL,
  creado_en DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (comprador_id) REFERENCES compradores(id),
  FOREIGN KEY (plan_id) REFERENCES planes(id)
);

CREATE TABLE pagos (
  id INT PRIMARY KEY AUTO_INCREMENT,
  licencia_id INT NULL,
  plan_id INT NULL,
  comprador_id INT NULL,
  mp_payment_id VARCHAR(64) NULL,
  monto DECIMAL(10,2) NOT NULL,
  estado ENUM('pendiente','aprobado','rechazado','gratuito') NOT NULL,
  creado_en DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (licencia_id) REFERENCES licencias(id),
  FOREIGN KEY (plan_id) REFERENCES planes(id),
  FOREIGN KEY (comprador_id) REFERENCES compradores(id)
);
```

`pagos.plan_id` y `pagos.comprador_id` se agregaron en la implementación: un pago pendiente
todavía no tiene licencia, y el webhook necesita saber de qué plan y de qué comprador es para
emitirla.

## Admin

Login único usuario/contraseña (`ADMIN_USER` y `ADMIN_PASSWORD`, autenticación Basic, sin tabla
de usuarios). Panel mínimo: lista de ventas, lista de licencias con activaciones y vencimiento,
y botón para revocar (`max_activaciones=0` en una licencia existente).

## Descargas

Los ejecutables de la app de escritorio se publican en `barraWeb/public/downloads/`
(`barra-backend-vX.Y.Z.exe` y `barra-gui-vX.Y.Z.jar`) y se sirven en `/downloads/`. El mail de
licencia lleva el link de `DOWNLOAD_URL`. Proceso de publicación en
[compilación y empaquetado](../tecnico/compilacion-y-empaquetado.md#3-publicar-una-versión).

## Diseño

Paleta seria y profesional (azul de marca y grises), tipografía sans-serif, sin elementos
llamativos. Componentes propios en `src/components/` (`button`, `card`, `input`, `table`,
`PlanCard`, `header`, `footer`) con Tailwind CSS.

## Testing

Se reemplazó el plan original de tests unitarios por una campaña de integración automatizada
contra MySQL real ([`pruebas/test_web.py`](../../pruebas/test_web.py)). **17 de 17 casos pasan**
(08/10/2026):

| Área | Casos | Qué cubren |
|---|---|---|
| Planes y compra | CW-01 a CW-04 | Planes iniciales, validaciones, plan no disponible, compra gratis con mail |
| Seguridad | CW-05 | La clave se guarda con bcrypt |
| Licencias | CW-06 a CW-11 | Activación (clave incorrecta, código inexistente, correcta, máximo de activaciones), estado y recuperación |
| Admin | CW-12 a CW-15 | Autenticación, listados y revocación |
| Webhook | CW-16, CW-17 | Firma inválida ignorada; firma válida aceptada |

Pendiente: probar el cobro real con credenciales de prueba de Mercado Pago. Detalle en el
[plan de pruebas](../proyecto/plan-de-pruebas.md).

## Decisiones registradas

- Concurrencia con hilos queda enteramente en el backend de escritorio (Python); no se fuerza en
  `barraWebBackend` (Node es single-threaded y no había un caso real que lo justificara).
- Seguridad de licencia: código + secret (no solo código), la opción más segura de las dos evaluadas
  ([ADR-009](../tecnico/decisiones.md#adr-009--licencias-con-código--clave-secreta-hasheada)).
- Estructura de carpetas: `barraPagina/` (web + backend de venta) y `application/` (escritorio:
  `barra-backend` y `barra-gui`).

## Limitaciones conocidas

- Un mismo email puede obtener el plan Gratis varias veces.
- Un pago pendiente redirige a «Pago fallido» (`back_urls.pending`).
- No hay botón de descarga en la web ni páginas de términos y privacidad.
