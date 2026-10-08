# Revisión de la documentación de Barra

| | |
|---|---|
| Fecha | 8 de octubre de 2026 |
| Rama revisada | `development` (commit `4470dd9`) |
| Criterio | Documento de referencia «Proyecto Programación sobre Redes – Grupo 1 (Barra)» (20 secciones) |
| Documentos revisados | Documento de referencia, `README.md` (raíz), `application/barra-backend/README.md`, `application/barra-gui/README.md`, `barraPagina/barraWeb/README.md`, `barraPagina/barraWebBackend/README.md`, `barraPagina/barraWebBackend/.env.example`, `docs/specs/barra-pagina.md` |

Para revisar cada documento se lo comparó con el código y se ejecutó la aplicación (backend, GUI
y web). Los hallazgos marcados como «verificado» se reprodujeron en la app.

Solo se revisó lo que está en el repositorio y el documento de referencia. Si alguno de los
entregables marcados como faltantes existe en otro lado (Drive, etc.), conviene sumarlo a `docs/`.

---

## Resumen

1. **El documento de referencia se contradice en la tecnología del frontend de escritorio y no
   coincide con lo construido.** Habla de JavaFX + jpackage (§1, §17), de Electron/React (§11,
   §19) y el código es **Java Swing**, distribuido como `.jar`.
2. **El DER, el alcance, los requisitos y los casos de uso no incluyen las mesas, las cuentas ni
   el ticket**, que es una de las funciones más grandes del sistema. Tampoco incluyen la
   configuración de email ni el resumen diario tal como quedaron.
3. **Varias promesas de la documentación no se cumplen hoy:** instalación sin Java, la app abre
   el backend sola, activación de licencia en la app, pantalla de reportes, historial, Telegram,
   modificar o cancelar pedidos.
4. **Los README de la web de venta están vacíos** (0 bytes) y el README raíz tiene solo el título.
   No hay forma documentada de levantar la web, su backend ni MySQL.
5. **No están en el repo** el presupuesto, la propuesta formal, los resultados de entrevistas
   y encuestas, el plan de pruebas, el diagrama de casos de uso, el DER como diagrama y la
   presentación. En el documento de referencia solo figura qué deberían contener.

El **manual de usuario** (`docs/manual-de-usuario.md`) quedó hecho en esta misma rama, con
capturas reales. Al final del manual hay una tabla con las limitaciones de esta versión.

---

## 1. Inventario de lo que existe

| Documento | Ubicación | Estado |
|---|---|---|
| Documento de referencia (requisitos del proyecto) | Fuera del repo | Completo como guía, desactualizado e inconsistente en partes (ver §3) |
| README raíz | `README.md` | Solo el título «# Barra - Sistema de Pedidos» |
| README backend | `application/barra-backend/README.md` | Bueno como base, desactualizado en partes (ver §4.2) |
| README GUI | `application/barra-gui/README.md` | Bueno como base, desactualizado en partes (ver §4.3) |
| README web (frontend) | `barraPagina/barraWeb/README.md` | **Vacío** |
| README web (backend) | `barraPagina/barraWebBackend/README.md` | **Vacío** |
| Variables de entorno de la web | `barraPagina/barraWebBackend/.env.example` | Bien comentado, pero ningún documento lo menciona |
| Especificación de la web de venta | `docs/specs/barra-pagina.md` | Buena, con partes desactualizadas (ver §4.4) |
| Documentación automática de la API local | `http://127.0.0.1:8000/docs` (FastAPI) | Existe y está al día, pero no se menciona fuera del README del backend |
| **Manual de usuario** | `docs/manual-de-usuario.md` | **Nuevo** (esta revisión) |

---

## 2. Qué falta, sección por sección del documento de referencia

Leyenda: ✅ completo · ⚠️ parcial o desactualizado · ❌ falta.

| § | Entregable | Estado | Qué falta |
|---|---|---|---|
| 1 | Resumen de la arquitectura | ⚠️ | Corregir la tecnología del front y el empaquetado (ver 3.1 y 3.2). Agregar un **diagrama de componentes/despliegue** (GUI ↔ HTTP local ↔ backend Python ↔ SQLite; web ↔ Express ↔ MySQL ↔ Mercado Pago) |
| 2 | Presentación del producto | ❌ | Slides y video de respaldo. El guion pide mostrar la «pantalla de reportes», que no existe: ajustar el guion o implementarla |
| 3 | Manual de usuario | ✅ | Hecho en `docs/manual-de-usuario.md`. Actualizarlo cuando estén el instalador único y la activación de licencia |
| 4 | Presupuesto inicial | ❌ | Tabla de costos fijos y variables, horas × valor hora, hosting, dominio, comisión de Mercado Pago y precio de venta final (hoy la web usa precios de prueba de $100 y $150) |
| 5 | Propuesta formal al cliente | ❌ | Documento completo con la estructura sugerida (portada, resumen ejecutivo, alcance, cronograma, presupuesto, soporte) |
| 6 | Entrevistas y encuestas | ❌ | Están las preguntas, faltan las **respuestas**: a quién se entrevistó, cuándo, resultados y conclusiones |
| 7 | Análisis del sistema actual | ⚠️ | El documento recomienda vincular cada «dolor» 1 a 1 con un requisito funcional, pero esa trazabilidad no está |
| 8 | Identificación de actores | ⚠️ | Falta el actor **Mozo/Salón** (abre y cierra mesas). El «Comprador del software» no aparece en los casos de uso de la app de escritorio |
| 9 | Requisitos funcionales y no funcionales | ⚠️ | Sin identificadores (RF01, RNF01…), sin prioridad y sin criterio de aceptación. Faltan los requisitos de mesas, cuentas, ticket y configuración de email. Los no funcionales no dicen cómo se mide cada uno |
| 10 | FODA | ⚠️ | Está en viñetas. Revisar las fortalezas contra la situación actual: hoy hay que instalar Java y abrir dos programas |
| 11 | Diagrama de Gantt | ⚠️ | Es una lista de tareas, no un diagrama. Sin fechas reales, sin responsables y sin las tareas que se hicieron (mesas, email, resumen, backup). La tarea H dice «Frontend Electron/React» |
| 12 | Casos de uso | ⚠️ | Solo hay nombres. Falta el **diagrama de casos de uso** y la especificación de cada uno (actor, precondición, flujo principal, flujos alternativos). Faltan los casos de mesas, configuración y resumen diario |
| 13 | Definición del alcance | ⚠️ | Agregar mesas, cuentas y ticket (adentro) y aclarar Telegram (hoy afuera de hecho) |
| 14 | Estado del arte | ⚠️ | Viñetas sin fuentes, precios ni tabla comparativa de funciones |
| 15 | Propuesta de solución | ✅ | Breve pero suficiente. Habla de «pago único (o suscripción)»: la web vende planes por días, conviene alinearlo |
| 16 | DER | ⚠️ | Desactualizado (ver 3.3) y sin diagrama |
| 17 | Elección y justificación de tecnologías | ⚠️ | Dice JavaFX. Es Swing. No justifica FastAPI ni Tailwind |
| 18 | Roles de Scrum | ⚠️ | Falta el nombre del Product Owner. No hay evidencia de sprints, backlog ni ceremonias |
| 19 | Estructura del frontend | ❌ | Describe una estructura React/Electron (`Activacion.jsx`, `Mostrador.jsx`, `electron/main.js`) que no existe. Hay que reescribirla con la estructura real (`com.barra.gui.*`) |
| 20 | Cómo levantar Python en el puerto 8000 | ⚠️ | Incompleto: no menciona `pip install -r requirements.txt`, `BARRA_SECRET_KEY` ni `run_backend.py`. Es una guía para desarrolladores y debería estar en el README del backend, no en el documento del proyecto |

### Documentación que no pide el documento de referencia pero hace falta

| Documento | Por qué |
|---|---|
| **README raíz completo** | Es lo primero que ve cualquiera que abre el repo: qué es Barra, estructura de carpetas (`application/`, `barraPagina/`, `docs/`), cómo levantar cada parte, enlaces al manual y a la spec, integrantes |
| **Guía de compilación y empaquetado** | No está escrito cómo se generan `barra-backend-v1.0.0.exe` (comando y opciones de PyInstaller; el `.spec` está en `.gitignore`) ni `barra-gui-v1.0.0.jar` (`mvn package`), ni cómo se publican en `public/downloads/` |
| **Guía de despliegue de la web** | MySQL (la base `barra_web` hay que crearla a mano; las tablas se crean solas), variables del `.env`, credenciales de prueba de Mercado Pago, URL pública para el webhook, `VITE_API_URL`, build de Vite |
| **Plan de pruebas y resultados** | La tarea M del Gantt («Pruebas integrales») no tiene documento. No hay tests en el repo (ver 4.2 y 4.4) |
| **Manual del panel de administración web** | `/admin` (ventas, licencias, revocar) es para el equipo que vende Barra, no para el local: no entra en el manual de usuario, pero necesita una guía |
| **Registro de decisiones** | Hay decisiones de arquitectura sin dejar asentadas: Swing en vez de JavaFX/Electron, mesas dentro del alcance, solo email (sin Telegram), licencia con código + clave secreta |
| **Historial de versiones (changelog)** | Los ejecutables dicen `v1.0.0`, pero no hay registro de qué incluye cada versión |
| **Términos y condiciones y política de privacidad de la web** | La web recolecta nombre y email de compradores y cobra con Mercado Pago (Ley 25.326 de Protección de Datos Personales) |

---

## 3. Falencias del documento de referencia

### 3.1 Tecnología del frontend de escritorio (gravedad alta)

El documento dice tres cosas distintas:

- §1 y §17: GUI en **Java con JavaFX**, empaquetada con `jpackage`.
- §11 (tarea H): «Frontend **Electron/React**».
- §19: árbol de carpetas **React + Electron** (`src/pages/*.jsx`, `electron/main.js`) y, a
  continuación, «Java (GUI de escritorio): JavaFX…».

El código real (`application/barra-gui`) es **Java 17 con Swing** (sin JavaFX) y se compila
con Maven a un `.jar`. `docs/specs/barra-pagina.md` también menciona la «desviación Java» como
decisión pendiente.

**Sugerencia:** registrar la decisión («Swing, por …») y unificar §1, §11, §17 y §19.

### 3.2 Instalación y empaquetado (gravedad alta)

| El documento dice | Hoy es así |
|---|---|
| «`jpackage` … incluye el runtime, el cliente no instala Java» (§1) | Se distribuye `barra-gui-v1.0.0.jar`: **hay que instalar Java 17+** |
| «Descargar el instalador desde la web, ejecutar» (§3) | Son **dos archivos** sueltos (`.exe` de consola de Windows + `.jar`). La web no tiene botón de descarga: el link llega en el mail de licencia |
| «Al iniciar, [la GUI] lanza el .exe de Python como subproceso» (§19) | La GUI no lanza nada: hay que abrir primero el `.exe` y después el `.jar` |
| «La instalación no requiere conocimientos técnicos» (§9, RNF) | Hoy no se cumple |

**Sugerencia:** documentar el estado actual (ya está en el manual) y dejar «instalador único con
jpackage» como trabajo pendiente con fecha en el Gantt.

### 3.3 DER desactualizado (gravedad alta)

Base local (SQLite), comparando el §16 con `app/database.py`:

| Entidad | En el §16 | En el código |
|---|---|---|
| Producto | id, nombre, precio, stock | además: `disponible`, `umbral_stock`, `alerta_stock_enviada` |
| Pedido | id, fecha, estado, total, nota | además: `cuenta_id` (FK a Cuenta, nula en pedidos de mostrador) |
| DetallePedido | igual | igual |
| **Mesa** | — | id, nombre, estado (libre/ocupada) |
| **Cuenta** | — | id, mesa_id, fecha_apertura, fecha_cierre, estado (abierta/cerrada) |
| **Configuracion** | — | fila única: nombre del local, umbral global, SMTP, resumen diario |
| **Admin** | — | fila única: nombre, email y teléfono del dueño |

Faltan las relaciones Mesa 1—N Cuenta y Cuenta 1—N Pedido.

Base de la web (MySQL), comparando el §16 con `barraWebBackend/src/db.js`:

| Entidad | En el §16 | En el código |
|---|---|---|
| **Plan** | — | id, nombre, precio_ars, dias_renovacion, max_activaciones, disponible |
| Licencia | id, comprador_id, código, fecha_activación | además: `plan_id`, `secret_hash`, `dias_renovacion`, `activaciones_usadas`, `max_activaciones`, `fecha_vencimiento` |
| Pago | id, comprador_id, monto, estado, fecha | además: `licencia_id`, `plan_id`, `mp_payment_id`; estado ∈ pendiente/aprobado/rechazado/gratuito |

### 3.4 Requisitos y casos de uso que no coinciden con el sistema (gravedad media)

| Documentado | Situación real |
|---|---|
| Alertas «por mail o Telegram» | Solo email. No hay nada de Telegram en el código |
| «Reportes simples: ventas del día, producto más vendido» + «pantalla de reportes» (§2) | No hay pantalla. El reporte es el resumen diario por email (y el botón «Enviar resumen ahora») |
| Uso diario: «ver historial» (§3) | No hay historial. Cocina muestra los últimos 10 entregados |
| CU «Modificar/cancelar pedido (cajero)» | No implementado |
| CU «Activar licencia (administrador, primer uso)» y «validación de licencia» | La web emite y valida licencias, pero la app de escritorio no las pide |
| «Versión de prueba gratis» | Existe como plan «Gratis» de 10 días en la web, pero la app no controla el vencimiento |
| RF «Alta, baja y modificación de productos» | No hay baja: solo se puede pausar con «Disponible» |
| RF «Mostrar el estado … visible para cajero y cocina» | Solo si están en **la misma PC**: la GUI apunta fija a `127.0.0.1:8000` |
| (no documentado) | **Mesas, cuentas por rondas, ticket imprimible, datos del dueño, email de prueba, resumen diario configurable, hilo de backup** |

### 3.5 Errores de redacción y formato (gravedad baja)

- Línea del alcance clave: «No hay **|||componente**» (sobran caracteres).
- §1: «el cliente **o** instala Python» → «el cliente **no** instala Python».
- Antes del §11 hay un encabezado vacío (`## `).
- §19: «Expone una API HTTP local (**Flask/FastAPI**)»: se usó FastAPI, hay que decidirlo en el texto.
- §19: «Corre en **tu** servidor»: cambia el registro (el resto del documento es impersonal).
- Las secciones «Conexiones externas del sistema» y «Nuevo a sumar» no están numeradas y
  funcionan como backlog. Conviene pasarlas a requisitos con ID o al Gantt.
- El documento de referencia **no está versionado en el repo**. Conviene guardarlo en `docs/`.

---

## 4. Falencias de los documentos del repositorio

### 4.1 `README.md` (raíz)

- Tiene solo el título y no termina con salto de línea.
- Falta: descripción del sistema, estructura de carpetas, requisitos, cómo levantar cada parte
  (backend, GUI, web, backend web), enlaces a `docs/`, integrantes y roles.

### 4.2 `application/barra-backend/README.md`

- **Lista de endpoints incompleta:** faltan `POST /configuracion/probar-email`,
  `GET /resumen-diario` y `POST /resumen-diario/enviar`.
- **Checklist contradictorio:** en «Configuración» hay ítems tildados `[x]` cuyo texto dice que
  todavía falta algo («falta exponerlo en `ProductoIn/Out/Patch`», «el watcher todavía no
  distingue el momento exacto…»). Eso ya está hecho: hay que reescribir el texto.
- **«[x] Tests de backend»:** no hay ningún test en el repo.
- **«Empaquetado con PyInstaller»** figura sin tildar, pero el `.exe` existe. Falta documentar
  cómo se generó (comando y opciones).
- **No menciona `run_backend.py`**, que es lo que corre el `.exe`: genera sola
  `barra_secret.key` si no existe `BARRA_SECRET_KEY`. El README dice que la variable es
  obligatoria, pero no explica cómo generarla (el comando está solo en el docstring de `app/main.py`).
- `app/main.py` dice que se corre con `--env-file app/.env`, pero no hay `.env.example` del
  backend y el README no lo menciona.
- **Variables de entorno incompletas:** faltan `BARRA_EMAIL_RETRY_SECONDS`,
  `BARRA_RESUMEN_CHECK_INTERVAL` y `BARRA_BACKUP_DIR`. Dice que el umbral es
  `BARRA_STOCK_MINIMO`, pero ahora es solo un respaldo: manda el umbral del producto o el global.
- La sección «Concurrencia» está desactualizada: no menciona el **hilo del resumen diario**
  (son cuatro: pool de pedidos, vigilancia de stock, backup y resumen) ni que la vigilancia ahora manda emails.
- Falta: cómo **restaurar un backup** y dónde quedan `barra.db`, `barra_secret.key` y `backups/` cuando se corre el `.exe`.
- (Del repo, no del README) `application/barra-backend/.gitignore` tiene `dist/yzzzzz`: con
  ese error `dist/` no se ignora, y la próxima compilación con PyInstaller va a aparecer como cambio en git.

### 4.3 `application/barra-gui/README.md`

- Describe `AdminConfiguracionPanel` como «nombre del local». Hoy incluye datos del dueño,
  umbral global, SMTP, resumen diario y los botones «Enviar email de prueba» y «Enviar resumen ahora».
- La descripción de la pantalla **Admin** no menciona la configuración de email ni el umbral de stock por producto.
- Falta `Admin.java` en la lista de archivos.
- Solo explica cómo importarlo en Eclipse. Falta: requisito de **Java 17**, `mvn package`,
  `java -jar target/barra-gui-1.0.0.jar` y cómo se publica el `.jar` en `barraPagina/barraWeb/public/downloads/`.
- No avisa que la URL del backend está fija en `ApiClient.java` (`127.0.0.1:8000`), así que no se puede usar desde otra PC.
- Dice «Cliente Swing», lo que es correcto pero contradice el documento de referencia (ver 3.1).

### 4.4 `docs/specs/barra-pagina.md`

- «Al momento de escribir esto es un esqueleto vacío…»: ya no es cierto. Actualizar el estado
  («aprobado, en implementación») y la fecha (3 de septiembre de 2026).
- Usa nombres de carpetas que no existen: `barraAplicacion/`, `barraGui`. En el repo son
  `application/barra-backend` y `application/barra-gui`.
- El esquema de `pagos` no tiene `plan_id` ni `comprador_id`, que sí están en el código.
- La sección «Testing» promete 8 tests (unitarios, integración y E2E). No hay ninguno
  (`npm test` corre `node --test` sin archivos de test).
- «Diseño: componentes de 21st.dev, sistema de diseño vía `/ui-ux-pro-max`»: nombra
  herramientas de trabajo internas que no le dicen nada a quien lee. Conviene describir la paleta y los componentes.
- No explica cómo se sirve la descarga (`public/downloads/`) ni la variable `DOWNLOAD_URL`.
- Deja afuera «la lógica de expiración/bloqueo del lado de la app», pero no dice dónde se
  registra esa tarea pendiente. Hoy no está en ningún lado.

### 4.5 `barraPagina/barraWeb/README.md` y `barraPagina/barraWebBackend/README.md`

Los dos están **vacíos**. Como mínimo deberían tener:

- **barraWeb:** `npm ci`, `npm run dev` (puerto 5173), `npm run build`, la variable
  `VITE_API_URL` (por defecto `http://localhost:4000/api`), las rutas (`/`, `/checkout/:planId`,
  `/pago-exitoso`, `/pago-fallido`, `/recuperar-licencia`, `/admin`) y la carpeta `public/downloads/`.
- **barraWebBackend:** requisitos (Node 18+ y MySQL 8), crear la base `barra_web`, copiar
  `.env.example` a `.env` y qué es cada variable, `npm run dev`, la lista de endpoints
  (`/api/planes`, `/api/compras`, `/api/pagos/webhook`, `/api/licencias/activar|estado|recuperar`,
  `/api/admin/*`), cómo probar con el sandbox de Mercado Pago y cómo exponer el webhook en desarrollo.

---

## 5. Problemas del producto encontrados al escribir el manual

No son de documentación, pero aparecen en el manual como limitaciones o en «Problemas frecuentes»:

| # | Problema | Dónde | Verificado |
|---|---|---|---|
| 1 | Eliminar una mesa que alguna vez tuvo una cuenta devuelve **error 500** (`FOREIGN KEY constraint failed`). En la GUI aparece «Error del backend (500): Internal Server Error» | `DELETE /mesas/{id}` en `app/main.py` | Sí |
| 2 | Los mensajes de error de la GUI muestran el JSON crudo del backend (ej. `Error del backend (400): {"detail":"Stock insuficiente…"}`) | `ApiClient.checkOk` | Sí |
| 3 | Un pago **pendiente** de Mercado Pago (ej. en efectivo) vuelve a «Pago fallido», que dice «no se generó ningún cargo» | `back_urls.pending` en `mercadoPagoService.js` | Por código |
| 4 | La web no tiene botón ni página de descarga, aunque los archivos están en `public/downloads/` | `barraWeb` | Sí |
| 5 | En Vender y en las mesas, el stock se pinta de naranja con un umbral fijo de 5, sin usar el umbral configurado | `ProductoCard.java` | Por código |
| 6 | Cocina muestra los pedidos más **nuevos arriba**: el más viejo, que suele ser el más urgente, queda abajo | `GET /pedidos` (`ORDER BY id DESC`) | Sí |
| 7 | Tocar una mesa libre abre la cuenta enseguida: si se cierra la ventana sin pedir, la mesa queda «Ocupada» con $ 0 | `MesasPanel.abrirMesa` | Sí |
| 8 | «Eliminar» mesa no pide confirmación | `AdminMesasPanel` | Sí |
| 9 | `app/main.py` importa dos veces de `.concurrency` y redefine `obtener_email_destino_efectivo`, que ya existe en `mailer.py` | `app/main.py` | Por código |

---

## 6. Prioridades sugeridas

1. **Unificar la tecnología y la instalación** en el documento de referencia (§1, §3, §11, §17,
   §19) y dejar asentada la decisión. Es lo que más confunde a un evaluador.
2. **Actualizar el DER, el alcance, los requisitos (con ID) y los casos de uso** para incluir
   mesas, cuentas, ticket y configuración de email. Marcar lo pendiente (Telegram, reportes en
   pantalla, historial, cancelar pedido, activación de licencia) como fuera de esta versión o
   como trabajo planificado.
3. **Completar los README:** raíz, `barraWeb` y `barraWebBackend`. Corregir los checklists del
   README del backend.
4. **Escribir los entregables que faltan:** presupuesto, propuesta formal, resultados de
   entrevistas y encuestas, plan de pruebas, Gantt con fechas reales y presentación.
5. Decidir si los problemas de la sección 5 se arreglan antes de la entrega. Si se arreglan,
   actualizar las secciones 13 y 14 del manual.
