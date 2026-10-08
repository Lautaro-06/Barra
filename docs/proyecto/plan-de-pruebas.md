# Plan de pruebas y resultados — Barra

| | |
|---|---|
| Versión probada | 1.0.0 (rama `development`) |
| Fecha de ejecución | 8 de octubre de 2026 |
| Tarea del Gantt | M – Pruebas integrales |
| Resultado global | **64 de 65 casos pasaron.** 1 defecto abierto (CP-17) |

---

## 1. Objetivo

Verificar que el sistema cumple los requisitos funcionales y no funcionales del
[documento del proyecto](documento-del-proyecto.md#9-requisitos-funcionales-y-no-funcionales)
y que se puede compilar y empaquetar con los pasos documentados.

## 2. Alcance

| Se prueba | No se prueba en esta campaña (y por qué) |
|---|---|
| Backend local: pedidos, stock, mesas, configuración, emails, hilos, concurrencia, persistencia | Cobro real con Mercado Pago: requiere credenciales de prueba de una cuenta de Mercado Pago |
| GUI de escritorio: todas las pantallas, con datos reales del backend | Impresión física del ticket: requiere una impresora (se probó el diálogo del sistema) |
| Backend de la web: compra gratis, licencias, recuperación, panel admin, webhook | El `.exe` en Windows: el empaquetado se verificó generando el ejecutable equivalente en Linux |
| Web de venta: páginas de compra y recuperación | Envío SMTP por el puerto 465 (SSL directo): se probó el 587 con STARTTLS |
| Compilación: Maven, Vite y PyInstaller | Activación de licencia desde la app, cancelación de pedidos, historial: no están implementados |

## 3. Entorno

| Componente | Versión usada en las pruebas |
|---|---|
| Sistema operativo | Linux x86_64 (contenedor), pantalla virtual Xvfb 1280×800 para la GUI |
| Python | 3.13 con las dependencias de `requirements.txt` (FastAPI 0.143, uvicorn 0.53, Pydantic 2.13, cryptography 50) |
| Java | OpenJDK 21 (la GUI se compila para Java 17) |
| Maven | 3.x |
| Node.js | 22.22 |
| Base de la web | MariaDB 10.11 (compatible con MySQL) |
| Servidor de email de prueba | `aiosmtpd` 1.4.6 con STARTTLS (certificado autofirmado) y autenticación |
| Navegador | Chromium (Playwright) |

## 4. Estrategia

| Nivel | Cómo | Dónde está |
|---|---|---|
| Pruebas de API, caja negra (CP y CW) | Scripts que levantan el sistema desde cero, ejecutan cada caso por HTTP y comparan con el resultado esperado | [`pruebas/test_escritorio.py`](../../pruebas/test_escritorio.py), [`pruebas/test_web.py`](../../pruebas/test_web.py) |
| Pruebas de interfaz (CG) | La GUI y la web se abrieron con datos reales y se verificó cada pantalla con capturas | Capturas en [`docs/img/manual/`](../img/manual/) |
| Pruebas de construcción (CG) | Compilación y empaquetado con los comandos de la [guía de compilación](../tecnico/compilacion-y-empaquetado.md) | — |

**Datos de prueba:** los scripts parten de una base vacía. El de escritorio trabaja sobre una
copia temporal del backend (nunca toca un `barra.db` real) y usa los productos y mesas de
ejemplo más los que crea cada caso. El de la web usa una base propia, `barra_web_pruebas`.

**Para acelerar las pruebas** de los hilos se acortaron los intervalos por variables de entorno:
vigilancia de stock y resumen cada 1 s (normalmente 30 s), reintento de email a los 3 s
(normalmente 300 s). La lógica es la misma.

**Criterio de aceptación:** pasan todos los casos de prioridad M (debe). Un defecto en un caso de
prioridad S se acepta si tiene una solución temporal documentada en el manual.

---

## 5. Resultados — backend de escritorio (CP)

**33 de 34 pasaron.**

| ID | Req. | Caso | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|
| CP-01 | RNF03 | Primer arranque crea la base con datos de ejemplo | 3 productos, 6 mesas, local 'Mi local' | 3 productos, 6 mesas, local 'Mi local' | ✅ Pasó |
| CP-02 | RF13 | Backup automático al arrancar | Un archivo barra_backup_*.db y una línea OK en backups.log | 1 backup(s); log: 2026-10-08T18:49:28 \| OK \| barra_backup_20261008_184928.db \| 36864 bytes | ✅ Pasó |
| CP-03 | RF08 | Alta de producto | HTTP 201, disponible por defecto, sin umbral propio | HTTP 201, id 4, disponible=True | ✅ Pasó |
| CP-04 | RF08 | Alta con precio 0 se rechaza | HTTP 422 | HTTP 422 | ✅ Pasó |
| CP-05 | RF08 | Modificación parcial de producto (precio y umbral) | Cambia solo lo enviado; umbral null = usar el global | precio 1600.0, umbral 3 -> None (vuelve al global), resto sin cambios | ✅ Pasó |
| CP-06 | RF08 | Baja lógica: producto no disponible no se puede vender | HTTP 400 «no está disponible» | HTTP 400: 'Empanada' no está disponible | ✅ Pasó |
| CP-07 | RF08 | Producto inexistente | HTTP 404 | HTTP 404 | ✅ Pasó |
| CP-08 | RF01, RF02, RF05 | Registrar pedido de mostrador con nota | HTTP 201, total = Σ precio×cantidad, estado en_preparacion, stock descontado | HTTP 201, total 10800.0 (esperado 10800.0), estado en_preparacion, stock hamburguesa 20→18, gaseosa 40→39 | ✅ Pasó |
| CP-09 | RF05 | Pedido con más cantidad que el stock | HTTP 400 «Stock insuficiente…» y no se registra nada | HTTP 400: Stock insuficiente para 'Papas fritas' (pedido: 999, stock: 30) | ✅ Pasó |
| CP-10 | RF01 | Pedido con cantidad 0 | HTTP 422 | HTTP 422 | ✅ Pasó |
| CP-11 | RF03 | Cambio de estado del pedido | en_preparacion→listo→entregado OK; estado inválido HTTP 400 | listo HTTP 200, entregado HTTP 200, 'cancelado' HTTP 400 | ✅ Pasó |
| CP-12 | RF04 | Listado de pedidos para cajero y cocina | GET /pedidos devuelve todos con estado, detalles y total | HTTP 200, 1 pedido(s) con estado/detalles/total | ✅ Pasó |
| CP-13 | RF09 | Ciclo completo de una mesa: abrir, 2 rondas, cerrar | Mesa ocupada→libre, cuenta con 2 rondas y total correcto, ticket con fecha de cierre | abrir→ocupada, 2 rondas, total mesa 6200.0 (esperado 6200.0), cierre HTTP 200 estado cerrada, mesa vuelve a libre | ✅ Pasó |
| CP-14 | RF09 | Ronda en mesa sin cuenta abierta | HTTP 400 «abrila primero» | HTTP 400: La mesa no tiene una cuenta abierta - abrila primero | ✅ Pasó |
| CP-15 | RF09 | Eliminar mesa ocupada | HTTP 400 «No se puede borrar una mesa con la cuenta abierta» | HTTP 400: No se puede borrar una mesa con la cuenta abierta | ✅ Pasó |
| CP-16 | RF09 | Alta y baja de una mesa nunca usada | HTTP 201 y HTTP 204 | alta HTTP 201, baja HTTP 204 | ✅ Pasó |
| CP-17 | RF09 | Eliminar mesa libre que ya tuvo cuentas | HTTP 204 (o 400 con un motivo claro) | HTTP 500: Internal Server Error | ❌ Falló |
| CP-18 | RF10 | Datos del dueño con email inválido | HTTP 422 | HTTP 422 | ✅ Pasó |
| CP-19 | RF10 | Sin email de destino, los mails van al dueño | email_destino_efectivo = email del dueño | email_destino None, efectivo duena@local.test | ✅ Pasó |
| CP-20 | RF11 | Habilitar email sin SMTP completo | HTTP 400 con los campos que faltan | HTTP 400: Para habilitar el email hacen falta: smtp_host, smtp_usuario, smtp_password | ✅ Pasó |
| CP-21 | RF11 | Hora de resumen inválida | HTTP 422 | HTTP 422 | ✅ Pasó |
| CP-22 | RF11 | Email de prueba con contraseña incorrecta | HTTP 400 con el aviso de contraseña (y la pista de Gmail) | HTTP 400: El servidor SMTP rechazó el usuario/contraseña (en Gmail hace falta una 'contraseña de aplicación') | ✅ Pasó |
| CP-23 | RF11 | Email de prueba con STARTTLS y credenciales correctas | HTTP 200 y el mail llega al servidor SMTP | HTTP 200, enviado a duena@local.test, asunto «[Mi local] Email de prueba» | ✅ Pasó |
| CP-24 | RNF07 | La contraseña SMTP se guarda cifrada y nunca se devuelve | Valor Fernet en la base; la API no la expone | en la base: gAAAAABqx-W4… (Fernet); la API solo devuelve smtp_password_configurada=True | ✅ Pasó |
| CP-25 | RF06 | Alerta por email al bajar del umbral | Un mail «[local] Stock bajo: Milanesa» en menos de 30 s y la alerta en /alertas | mail «['[Mi local] Stock bajo: Milanesa']»; /alertas incluye Milanesa con stock 4 | ✅ Pasó |
| CP-26 | RF06 | La alerta no se repite mientras sigue bajo | 0 mails nuevos | 0 mails nuevos por Milanesa | ✅ Pasó |
| CP-27 | RF06 | Después de reponer, la alerta vuelve a funcionar | Llega un nuevo mail al volver a bajar | volvió a llegar la alerta después de reponer | ✅ Pasó |
| CP-28 | RF07 | Resumen de ventas (consulta) | Total, pedidos, mostrador/mesas y productos más vendidos | 6 pedidos, total 106000.0, más vendido: Milanesa, cuentas cerradas 2 | ✅ Pasó |
| CP-29 | RF07 | Enviar resumen ahora | HTTP 200 y llega el mail con el resumen | HTTP 200, mail «[Mi local] Resumen de ventas del 08/10: $ 106.000,00» | ✅ Pasó |
| CP-30 | RF12 | Resumen diario automático a la hora programada | Llega exactamente 1 resumen | programado a las 18:49: llegaron 1 resumen(es) en 12 s | ✅ Pasó |
| CP-31 | RNF01 | Tiempo de respuesta | Todas las respuestas en menos de 1 s | GET /productos p95 1.7 ms; POST /pedidos p95 5.2 ms; máximo 7.5 ms | ✅ Pasó |
| CP-32 | RNF05 | Pedidos concurrentes sin pérdida ni duplicación | 10 aceptados, 20 rechazados, stock 0, 10 pedidos | 30 pedidos simultáneos de 1 unidad con stock 10: 10 aceptados, 20 rechazados, stock final 0, pedidos nuevos 10 | ✅ Pasó |
| CP-33 | RNF02 | Sin conexión con el servidor de mail se sigue vendiendo | El email falla con aviso; los pedidos funcionan | email: HTTP 400 (No se pudo mandar el email: [Errno 111] Connection refused…); pedido: HTTP 201; backend sigue respondiendo | ✅ Pasó |
| CP-34 | RNF03 | Los datos persisten al reiniciar | Mismos pedidos y configuración | 67 pedidos antes y 67 después de reiniciar; dueño «Ana» | ✅ Pasó |

---

## 6. Resultados — backend de la web de venta (CW)

**17 de 17 pasaron.**

| ID | Req. | Caso | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|
| CW-01 | RF14 | La base se crea sola con los 3 planes | Gratis (10 días, disponible), Pro y Max no disponibles | HTTP 200: [('Gratis', 0, 10, True), ('Pro', 100, 30, False), ('Max', 150, 365, False)] | ✅ Pasó |
| CW-02 | RF14 | Compra sin email | HTTP 400 | HTTP 400: plan_id, comprador.nombre y comprador.email son requeridos | ✅ Pasó |
| CW-03 | RF14 | Compra de un plan no disponible | HTTP 409 y no se crea nada | HTTP 409: Plan no disponible | ✅ Pasó |
| CW-04 | RF14 | Compra del plan Gratis | HTTP 201, redirige a /pago-exitoso, mail con código BARRA-XXXX-XXXX-XXXX y clave | HTTP 201 → /pago-exitoso; mail «Tu licencia de Barra» con código BARRA-VDKX-NTYV-RNJ3 y clave de 64 caracteres | ✅ Pasó |
| CW-05 | RNF07 | La clave secreta se guarda hasheada | Hash bcrypt en la base | secret_hash = $2a$10$… (bcrypt), la clave no se guarda en claro | ✅ Pasó |
| CW-06 | RF15 | Activar con clave incorrecta | HTTP 401 | HTTP 401: Credenciales inválidas | ✅ Pasó |
| CW-07 | RF15 | Activar con código inexistente | HTTP 404 | HTTP 404: Licencia inexistente | ✅ Pasó |
| CW-08 | RF15 | Activar con código y clave correctos | HTTP 200, valido=true, vence en 10 días (plan Gratis) | HTTP 200, valido=True, vence en 10 días | ✅ Pasó |
| CW-09 | RF15 | Segunda activación (máximo 1) | HTTP 403 | HTTP 403: Licencia sin activaciones disponibles | ✅ Pasó |
| CW-10 | RF16 | Consultar estado de la licencia | HTTP 200, valido=true | HTTP 200, valido=True | ✅ Pasó |
| CW-11 | RF17 | Recuperar licencia | Mail solo con el código; misma respuesta si el email no existe | email registrado: HTTP 200 y 1 mail con el código (sin la clave); email desconocido: HTTP 200 sin mail | ✅ Pasó |
| CW-12 | RF18 | Panel admin sin credenciales | HTTP 401 | HTTP 401 | ✅ Pasó |
| CW-13 | RF18 | Panel admin con contraseña incorrecta | HTTP 401 | HTTP 401 | ✅ Pasó |
| CW-14 | RF18 | Panel admin: ventas y licencias | Lista la venta gratuita y la licencia emitida | ventas: 1 (gratuito, Gratis); licencias: 1 (1/1 activaciones) | ✅ Pasó |
| CW-15 | RF18 | Revocar una licencia | La licencia deja de ser válida | revocar HTTP 200; estado después: valido=False | ✅ Pasó |
| CW-16 | RF19 | Webhook de Mercado Pago con firma inválida | HTTP 200 (para que MP no reintente) y se ignora | HTTP 200, sin cambios en pagos, se registra «firma inválida» | ✅ Pasó |
| CW-17 | RF19 | Webhook con firma válida | Firma aceptada; se consulta el pago a Mercado Pago | HTTP 200; la firma se acepta y se consulta el pago a MP (falla por no tener MP_ACCESS_TOKEN, se registra y responde 200) | ✅ Pasó |

---

## 7. Resultados — interfaz y construcción (CG)

**14 de 14 pasaron.** Las pruebas de interfaz se hicieron con la GUI conectada al backend real
y datos de ejemplo; las capturas son la evidencia.

| ID | Req. | Caso | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|
| CG-01 | RF01, RF02 | Vender: armar un pedido y confirmarlo | Total calculado; aviso «Pedido #N enviado a cocina»; carrito vacío | Total $ 13.000,00; aviso «Pedido #9 enviado a cocina» ([captura](../img/manual/02-vender-confirmado.png)) | ✅ Pasó |
| CG-02 | RF05, RF08 | Productos sin stock o pausados | Tarjeta gris «Sin stock» / «No disponible», no se puede tocar | Agua mineral «Sin stock» y Flan «No disponible» en gris ([captura](../img/manual/01-vender-pedido.png)) | ✅ Pasó |
| CG-03 | RF09 | Grilla de mesas | Libres en verde; ocupadas en naranja con el total | Mesa 2 y Mesa 5 ocupadas con su total ([captura](../img/manual/03-mesas.png)) | ✅ Pasó |
| CG-04 | RF09 | Cuenta de una mesa | Rondas anteriores, ronda actual, total de ronda y de mesa | 2 rondas, ronda actual $ 4.800,00, total $ 19.400,00 ([captura](../img/manual/04-cuenta-mesa.png)) | ✅ Pasó |
| CG-05 | RF03, RF04 | Tablero de cocina | Tres columnas con botones Marcar listo / Entregar; mesas como «Mesa N» | Correcto ([captura](../img/manual/06-cocina.png)) | ✅ Pasó |
| CG-06 | RF09 | Ticket de cierre | Detalle por producto, total, botón Imprimir | Ticket de Mesa 5 por $ 23.300,00 ([captura](../img/manual/05-ticket.png)) | ✅ Pasó |
| CG-07 | RF08 | Admin → Productos | Tabla con stock en rojo (0) o naranja (bajo umbral); «Global (5)» | Correcto ([captura](../img/manual/07-admin-productos.png)) | ✅ Pasó |
| CG-08 | RF08 | Formulario de producto | Campos con los datos actuales; umbral vacío = global | Correcto ([captura](../img/manual/08-producto-form.png)) | ✅ Pasó |
| CG-09 | RF09 | Admin → Mesas | Botón Eliminar deshabilitado en mesas ocupadas | Correcto ([captura](../img/manual/09-admin-mesas.png)) | ✅ Pasó |
| CG-10 | RF10, RF11 | Admin → Configuración | Muestra a qué email llegan los mails y si hay contraseña guardada | «Los mails van a llegar a: dueno@ejemplo.com» ([captura](../img/manual/11-admin-email.png)) | ✅ Pasó |
| CG-11 | RNF02 | GUI sin backend | Indicador rojo «Backend caído»; se reconecta sola | Correcto ([captura](../img/manual/13-backend-caido.png)); al levantar el backend volvió a «Backend conectado» a los 2 s | ✅ Pasó |
| CG-12 | RNF09 | Compilar la GUI con Maven | `mvn package` genera `target/barra-gui-1.0.0.jar` | Jar de 104 KB en 15 s; `java -jar` abre la ventana sin errores | ✅ Pasó |
| CG-13 | RNF09 | Empaquetar el backend con PyInstaller | `pyinstaller --onefile` genera un ejecutable que arranca y crea `barra.db`, `barra_secret.key` y `backups/` junto a él | Ejecutable de 27 MB; `/health` responde OK y crea los tres archivos | ✅ Pasó |
| CG-14 | RF14, RF17 | Web de venta: planes, datos, «¡Listo!», recuperar licencia | Flujo completo navegable; build de producción sin errores | Correcto ([capturas](../img/manual/20-web-planes.png)); `npm run build` sin errores (175 KB de JS) | ✅ Pasó |

---

## 8. Defectos encontrados

| ID | Caso | Severidad | Descripción | Causa | Solución temporal | Propuesta de corrección |
|---|---|---|---|---|---|---|
| DEF-01 | CP-17 | Media | Eliminar una mesa que alguna vez tuvo una cuenta responde **HTTP 500** («Error del backend (500)» en la GUI) | La tabla `cuenta` referencia a la mesa y SQLite rechaza el borrado (`FOREIGN KEY constraint failed`); el endpoint no lo contempla | Documentada en el manual: armar las mesas antes de usarlas | Que `DELETE /mesas/{id}` responda 400 con un motivo claro, o agregar una baja lógica (`mesa.activa = 0`) que conserve el historial |

**Observaciones** (no son fallas de requisitos, pero afectan la experiencia; detalle en
[`docs/revision-documentacion.md`](../revision-documentacion.md#5-problemas-del-producto-encontrados-al-escribir-el-manual)):

- Los avisos de error de la GUI muestran el JSON del backend (ej. `Error del backend (400): {"detail": …}`).
- Un pago pendiente de Mercado Pago vuelve a la página «Pago fallido».
- La web no tiene botón de descarga.
- En Vender, el stock se pinta de naranja con un umbral fijo de 5.
- En Cocina, los pedidos más nuevos aparecen arriba.

---

## 9. Conclusión

- Se cumplen todos los requisitos de prioridad **M** implementados.
- El único defecto (DEF-01) es de prioridad S, tiene solución temporal y está documentado en el manual.
- Se cumplen los requisitos no funcionales de rendimiento (máximo 7,5 ms frente al límite de 1 s),
  concurrencia (30 pedidos simultáneos sin vender de más), funcionamiento sin conexión,
  persistencia y seguridad de credenciales.
- **RNF04** (instalación sin conocimientos técnicos) no se cumple todavía: requiere instalar Java
  y abrir dos programas. Queda en el backlog de la próxima versión.

**La versión 1.0.0 se considera apta para la presentación**, con DEF-01 y RNF04 como pendientes conocidos.

---

## 10. Cómo repetir las pruebas

Instrucciones completas en [`pruebas/README.md`](../../pruebas/README.md). Resumen, desde la raíz del repo:

```bash
# Backend de escritorio (usa el venv del backend)
pip install -r application/barra-backend/requirements.txt -r pruebas/requirements.txt
python pruebas/test_escritorio.py        # → pruebas/resultados/escritorio.json

# Web de venta (requiere MySQL/MariaDB y npm ci en barraPagina/barraWebBackend)
python pruebas/test_web.py               # → pruebas/resultados/web.json
```
