# Registro de decisiones de arquitectura

Decisiones importantes del proyecto, con su contexto, las alternativas que se evaluaron y sus
consecuencias. Las fechas salen del historial del repositorio. Formato: una decisión por
sección (ADR, *Architecture Decision Record*).

| ID | Decisión | Fecha | Estado |
|---|---|---|---|
| ADR-001 | GUI de escritorio en Java Swing | 20/08/2026 | Aceptada |
| ADR-002 | Backend local en Python con FastAPI, comunicado por HTTP local | 20/08/2026 | Aceptada |
| ADR-003 | SQLite con Python como único dueño del archivo | 20/08/2026 | Aceptada |
| ADR-004 | Concurrencia: pool de hilos para pedidos y tres hilos de fondo | 04/09/2026 – 09/09/2026 | Aceptada |
| ADR-005 | Mesas y cuentas dentro del alcance | 04/09/2026 | Aceptada |
| ADR-006 | Sincronización por sondeo cada 4 segundos | 04/09/2026 | Aceptada |
| ADR-007 | Alertas y resumen solo por email | 14/09/2026 | Aceptada |
| ADR-008 | Contraseña SMTP cifrada con Fernet y clave local | 14/09/2026 | Aceptada |
| ADR-009 | Licencias con código + clave secreta hasheada | 03/09/2026 | Aceptada |
| ADR-010 | Venta por planes con período (suscripción) | 03/09/2026 | Aceptada |
| ADR-011 | Distribución en dos archivos hasta tener un instalador | 06/10/2026 | Aceptada, temporal |

---

## ADR-001 — GUI de escritorio en Java Swing

**Contexto.** El plan original mencionaba JavaFX con `jpackage` y, en otras secciones, Electron
con React. Hacía falta una GUI de escritorio táctil, que corriera en PCs modestas y fuera fácil de distribuir.

**Decisión.** Java 17 con **Swing**, compilado con Maven a un único `.jar`, sin dependencias
externas (HTTP con `java.net.http` y un parser JSON propio).

**Alternativas.**

| Opción | A favor | En contra |
|---|---|---|
| JavaFX | Controles modernos | Desde Java 11 se distribuye aparte del JDK: más peso y configuración |
| Electron + React | Mismo stack que la web | Incluye Node y un navegador: instalador de más de 100 MB y más consumo de memoria |
| **Swing** | Viene con el JDK, `.jar` de 100 KB, sin dependencias | Aspecto antiguo por defecto (se resolvió con componentes propios: `RoundedPanel`, `RoundButton`, `Toast`, íconos vectoriales) |

**Consecuencias.** La GUI es liviana y portable, pero hoy requiere tener Java instalado (ver ADR-011).

---

## ADR-002 — Backend local en Python con FastAPI, comunicado por HTTP local

**Contexto.** La materia pide integrar más de un lenguaje y usar concurrencia. La lógica de
negocio tenía que poder probarse sin la GUI.

**Decisión.** Backend en **Python con FastAPI**, escuchando solo en `127.0.0.1:8000`. La GUI le
habla por HTTP con JSON.

**Alternativas.** Flask (más simple, pero sin validación de tipos ni documentación automática);
toda la lógica en Java (perdía la integración entre lenguajes y la separación en capas).

**Consecuencias.**

- Separación clara de capas (RNF06) y API documentada sola en `/docs`.
- Se puede probar todo por HTTP: la campaña de pruebas usa esa API.
- Al escuchar solo en `127.0.0.1`, no hay exposición a la red, pero todas las pantallas tienen que estar en la misma PC.

---

## ADR-003 — SQLite con Python como único dueño del archivo

**Contexto.** Los datos tienen que vivir en el local, sin instalar un motor de base de datos.

**Decisión.** **SQLite** en un único archivo (`barra.db`). Solo `database.py` lo abre; la GUI
nunca lo toca. Una única conexión compartida protegida por `write_lock`. El esquema se crea y
migra solo al arrancar (`ALTER TABLE` idempotentes), así no hace falta borrar la base al actualizar.

**Alternativas.** MySQL o PostgreSQL locales (requieren instalación y servicio); que Java también
escribiera el archivo (riesgo de bloqueos y corrupción con dos procesos).

**Consecuencias.** Cero instalación y backups simples (un archivo). Las escrituras se serializan:
suficiente para el volumen de un local chico (ver CP-31 y CP-32).

---

## ADR-004 — Concurrencia: pool de hilos para pedidos y tres hilos de fondo

**Contexto.** Requisitos de procesar pedidos simultáneos sin perder datos y de vigilar el stock en segundo plano.

**Decisión.**

- `ThreadPoolExecutor` de 4 hilos para los pedidos de mostrador; la sección crítica «validar y
  descontar stock» protegida por `write_lock`.
- Tres hilos daemon independientes: vigilancia de stock (30 s), backup (4 h) y resumen diario
  (30 s), cada uno con un `threading.Event` para apagarse al instante.
- Las conexiones SMTP se hacen fuera del lock para no frenar los pedidos.

**Consecuencias.** 30 pedidos simultáneos sobre 10 unidades: exactamente 10 aceptados (CP-32).
Los usos de concurrencia son distintos entre sí, como pedía la consigna.

---

## ADR-005 — Mesas y cuentas dentro del alcance

**Contexto.** El plan original solo contemplaba pedidos de mostrador. Al diseñar la GUI como
punto de venta real se vio que muchos locales atienden mesas y cobran al final.

**Decisión.** Agregar `mesa` y `cuenta`. Cada ronda de una mesa es un `pedido` con `cuenta_id`;
un pedido de mostrador tiene `cuenta_id` en NULL. Así la cocina, el stock y el resumen tratan
igual a los dos tipos de pedido.

**Consecuencias.** Más valor para el local y un caso de uso más (CU-02, CU-03). Sumó alcance no
planificado, una de las causas del desvío del Gantt. Como las cuentas cerradas referencian a su
mesa, una mesa usada no se puede borrar de la base: la baja es lógica (`mesa.activa = 0`), así
sale del salón y su historial se conserva (corrigió DEF-01).

---

## ADR-006 — Sincronización por sondeo cada 4 segundos

**Contexto.** Vender, Mesas y Cocina tienen que verse sincronizadas.

**Decisión.** La GUI pide el estado completo cada 4 segundos y además se refresca al instante
después de cada acción propia.

**Alternativas.** WebSockets o Server-Sent Events (actualización instantánea, pero más complejidad
en ambos lados).

**Consecuencias.** Simple y robusto: si el backend se cae, la GUI se reconecta sola (CG-11).
Con una sola PC, la demora máxima de 4 segundos no se nota.

---

## ADR-007 — Alertas y resumen solo por email

**Contexto.** El plan proponía email o Telegram.

**Decisión.** Solo **email** por SMTP, con la cuenta que elija el dueño (Gmail u otra), con
STARTTLS en el puerto 587 o SSL en el 465. Si el servidor no ofrece cifrado, no se manda la contraseña.

**Alternativas.** Telegram (requiere crear un bot, guardar su token y que el dueño tenga
Telegram); WhatsApp (API paga y con aprobación).

**Consecuencias.** Un solo canal, sin servicios externos propios. Telegram queda como RF23 (no en
esta versión); las encuestas incluyen la pregunta del canal preferido para revisar esta decisión.

---

## ADR-008 — Contraseña SMTP cifrada con Fernet y clave local

**Contexto.** La contraseña del email del dueño se guarda en la base local.

**Decisión.** Cifrarla con **Fernet** (biblioteca `cryptography`). La clave no se guarda en la
base: viene de `BARRA_SECRET_KEY` o, en el ejecutable, de `barra_secret.key`, que `run_backend.py`
genera la primera vez. La API nunca devuelve la contraseña, solo si está configurada.

**Consecuencias.** Una copia de `barra.db` sola no expone la contraseña. Si se pierde
`barra_secret.key`, hay que volver a escribir la contraseña en la GUI (documentado en el manual).

---

## ADR-009 — Licencias con código + clave secreta hasheada

**Contexto.** Hay que entregar una licencia por mail y validarla después desde la app.

**Decisión.** Cada licencia tiene un **código** visible (`BARRA-XXXX-XXXX-XXXX`, sin caracteres
ambiguos como 0/O o 1/I) y una **clave secreta** de 256 bits. La clave se guarda con **bcrypt**,
se manda una sola vez y nunca se reenvía. El vencimiento se calcula al activar, no al comprar.

**Alternativas.** Solo un código (cualquiera que lo viera podría activarlo).

**Consecuencias.** Un volcado de la base no permite reconstruir licencias válidas (CW-05). Si el
cliente pierde la clave, no se puede recuperar (ver el [manual del panel](manual-admin-web.md)).

---

## ADR-010 — Venta por planes con período (suscripción)

**Contexto.** El presupuesto planteaba pago único o suscripción.

**Decisión.** Planes con `dias_renovacion`: Gratis (10 días, prueba), Pro (30 días) y Max
(365 días). Los planes pagos se cobran con Mercado Pago Checkout Pro; la fuente de verdad del
pago es el webhook firmado, no la página de vuelta.

**Consecuencias.** Ingreso recurrente y prueba gratis para bajar la barrera de entrada (ver
[presupuesto](../proyecto/presupuesto.md)). La lógica de vencimiento en la app queda pendiente (RF20).

---

## ADR-011 — Distribución en dos archivos hasta tener un instalador

**Contexto.** Para la v1.0.0 había que poder entregar el sistema funcionando.

**Decisión.** Publicar el backend como `.exe` (PyInstaller, no requiere Python) y la GUI como
`.jar` (requiere Java 17+), por separado, en `barraWeb/public/downloads/`.

**Consecuencias.** Funciona, pero no cumple RNF04 (instalación sin conocimientos técnicos): hay
que instalar Java y abrir dos programas en orden. Reemplazo previsto: instalador con `jpackage` y
GUI que lance el backend (ver [compilación y empaquetado](compilacion-y-empaquetado.md#4-siguiente-paso-instalador-único-pendiente)).
