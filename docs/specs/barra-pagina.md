---
titulo: Barra Web (barraPagina) — compra, licencias y panel de admin
fecha: 2026-09-03
estado: aprobado, en implementación
---

# Barra Web (barraPagina)

## Contexto

`barraPagina` es la web pública donde dueños de otros locales compran una licencia
de Barra. Al momento de escribir esto es un esqueleto vacío (`barraWeb` con 6
páginas en 0 líneas, `barraWebBackend` con Express + modelos + rutas nombrados
pero vacíos, sin `package.json`, sin dependencias instaladas). Es 100% greenfield.

## Alcance

**Adentro:** compra (Checkout Pro de Mercado Pago), emisión de licencia, entrega
por mail, recuperación de licencia, panel de admin básico, endpoint de
activación/validación que `barraAplicacion` va a consumir.

**Afuera (explícito):**
- La lógica de expiración/bloqueo del lado de `barraAplicacion` (Python) — acá
  solo se expone el endpoint de estado, no se implementa el chequeo del cliente.
- Resolver la desviación Java de `barraGui` (debería ser Electron+React según el
  plan original; hoy es Java/Maven). Queda pendiente como decisión aparte.
- Habilitar Pro/Max de verdad — el código queda listo pero `disponible=false`.

## Planes (tabla `planes`)

| id | nombre | precio_ars | dias_renovacion | max_activaciones | disponible |
|----|--------|-----------|------------------|-------------------|------------|
| 1 | Gratis | 0 | 10 | 1 | true |
| 2 | Pro | 100 (mínimo de prueba, ajustable) | 30 | 1 | false |
| 3 | Max | 150 (mínimo de prueba, ajustable) | 365 | 1 | false |

El único diferenciador real entre planes es `dias_renovacion` (y precio).
`disponible=false` bloquea la compra en el backend, no solo visualmente.

## Flujo de compra

```
POST /api/compras
body: { plan_id, comprador: { nombre, email } }
```

- `plan.disponible === false` → 409, no se crea nada.
- `plan.precio === 0` (Gratis) → esquiva Mercado Pago: crea `Comprador`, genera
  `Licencia`, registra `Pago` con `monto=0, estado="gratuito"`, manda el mail,
  responde con redirect a `PagoExitoso`.
- `plan.precio > 0` (Pro/Max) → crea preferencia en Mercado Pago (Checkout Pro),
  guarda `Pago` en estado `pendiente`, responde con la URL de MP.

La fuente de verdad del pago es el **webhook**, no el redirect del navegador:

```
POST /api/pagos/webhook
```

Valida la firma de Mercado Pago, consulta el estado real vía su API, y recién
ahí genera `Comprador`+`Licencia`+mail si está aprobado. `PagoExitoso.jsx` /
`PagoFallido.jsx` son solo feedback visual, no disparan lógica de negocio.

## Licencias — seguridad

Cada licencia tiene **código** (identificador visible, ej.
`BARRA-7F3A-9C1D-4E82`) y **secret** (32 bytes de alta entropía). Ambos se
mandan una sola vez por mail. En la base, el secret se guarda **hasheado**
(bcrypt) — un dump de la DB no permite reconstruir licencias válidas.

```
POST /api/licencias/activar
body: { codigo, secret }
→ verifica hash, chequea activaciones_usadas < max_activaciones,
  incrementa contador, fija fecha_activacion/fecha_vencimiento,
  responde { valido: true, fecha_vencimiento }

GET /api/licencias/estado?codigo=...&secret=...
→ para que barraAplicacion re-valide periódicamente

POST /api/licencias/recuperar
body: { email }
→ reenvía código (no el secret) al mail del comprador
```

`fecha_vencimiento` se calcula en la **activación**, no en la compra — si
alguien tarda una semana en instalar, no pierde días de su plan.

## Esquema (MySQL)

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
  comprador_id INT NOT NULL REFERENCES compradores(id),
  plan_id INT NOT NULL REFERENCES planes(id),
  codigo VARCHAR(32) UNIQUE NOT NULL,
  secret_hash VARCHAR(60) NOT NULL,
  dias_renovacion INT NOT NULL,
  activaciones_usadas INT NOT NULL DEFAULT 0,
  max_activaciones INT NOT NULL,
  fecha_activacion DATETIME NULL,
  fecha_vencimiento DATETIME NULL,
  creado_en DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pagos (
  id INT PRIMARY KEY AUTO_INCREMENT,
  licencia_id INT NULL REFERENCES licencias(id),
  mp_payment_id VARCHAR(64) NULL,
  monto DECIMAL(10,2) NOT NULL,
  estado ENUM('pendiente','aprobado','rechazado','gratuito') NOT NULL,
  creado_en DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

## Admin

Login único usuario/contraseña (env vars, sin tabla de usuarios). Panel mínimo:
lista de ventas, lista de licencias con estado, botón para revocar
(`max_activaciones=0` en una licencia existente).

## Diseño

Componentes de 21st.dev, sistema de diseño vía `/ui-ux-pro-max`. Paleta
seria/profesional, sin elementos llamativos.

## Testing

| Capa | Qué | Cant. |
|------|-----|-------|
| Unit | generación código+secret, hash/verify, cálculo fecha_vencimiento | +4 |
| Integration | POST /compras (gratis) → licencia creada; webhook MP → licencia creada; /activar rechaza tras max_activaciones | +3 |
| E2E | comprar plan gratis en el navegador → recibir mail (mock) → activar contra el endpoint | +1 |

## Decisiones registradas durante la interrogación

- Concurrencia con hilos queda enteramente en `barraAplicacion` (Python); no se
  fuerza en `barraWebBackend` (Node es single-threaded, no había un caso real
  que lo justificara).
- Seguridad de licencia: código + secret (no solo código), la opción más
  segura de las dos evaluadas.
- Estructura de carpetas: `barraPagina/` (web+backend de venta) y
  `barraAplicacion/` (desktop), ambas sin guiones, camelCase.
