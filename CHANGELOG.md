# Historial de versiones

Todos los cambios importantes de Barra. Formato basado en
[Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/); las versiones siguen
[versionado semántico](https://semver.org/lang/es/).

## [Sin publicar]

### Corregido

- Eliminar una mesa que ya tuvo cuentas respondía error 500 (DEF-01). Ahora la baja es lógica
  (`mesa.activa`): la mesa sale del salón y sus cuentas cerradas siguen en el historial. Las
  bases existentes se actualizan solas al abrir el backend.
- Se regeneró `barra-backend-v1.0.0.exe` en `public/downloads/` con la corrección.

### Documentación

- Documento del proyecto v2 con las 20 secciones actualizadas, casos de uso, presupuesto,
  propuesta formal, instrumentos de relevamiento y plan de pruebas (`docs/proyecto/`).
- Manual de usuario, guías técnicas, manual del panel admin, registro de decisiones y borradores legales.
- Campaña de pruebas automatizada en `pruebas/` (65 de 65 casos pasan).

## [1.0.0] — 2026-10-08

Primera versión distribuible.

### Agregado

- **App de escritorio**
  - Pantalla **Vender**: pedidos de mostrador con carrito táctil, nota y total automático.
  - Pantalla **Mesas**: cuenta por mesa con rondas, cierre y ticket imprimible.
  - Pantalla **Cocina**: tablero con pedidos en preparación, listos y entregados.
  - Pantalla **Admin**: productos (alta, edición, stock, umbral propio, pausa), mesas del salón,
    nombre del local, datos del dueño y configuración de email.
  - Indicador de conexión con el backend y reconexión automática.
- **Backend local**
  - API HTTP con FastAPI y base SQLite con migraciones automáticas.
  - Pool de hilos para pedidos concurrentes, con descuento de stock atómico.
  - Hilo de vigilancia de stock con alertas por email (una por cruce del umbral).
  - Hilo de resumen diario de ventas por email a la hora configurada, y envío manual.
  - Hilo de backup automático cada 4 horas con retención de 5 copias.
  - Contraseña SMTP cifrada con Fernet; email de prueba.
- **Web de venta**
  - Planes Gratis (activo), Pro y Max (listos, deshabilitados); compra con Mercado Pago Checkout Pro.
  - Licencias con código + clave secreta (bcrypt), activación, consulta de estado y recuperación del código.
  - Panel de administración con ventas, licencias y revocación.
- **Distribución:** `barra-backend-v1.0.0.exe` (PyInstaller) y `barra-gui-v1.0.0.jar` en
  `barraPagina/barraWeb/public/downloads/`.

### Problemas conocidos

- Eliminar una mesa que ya tuvo cuentas responde error 500 (DEF-01; corregido, ver «Sin publicar»).
- La GUI requiere Java 17+ y hay que abrir primero el backend (RNF04 pendiente).
- La app de escritorio todavía no pide ni valida la licencia.
- No se pueden modificar ni cancelar pedidos; no hay pantalla de historial ni de reportes.

## Hitos previos (sin versión publicada)

| Fecha | Hito |
|---|---|
| 2026-10-06 | Primeros ejecutables del backend y la GUI |
| 2026-10-04 | Configuración de email, umbral por producto y resumen diario (rama `app/functions`) |
| 2026-09-09 | Vigilancia de stock, `GET /alertas` y backup automático (rama `app/concurrence`) |
| 2026-09-04 | Web de venta completa; GUI como punto de venta con mesas, admin y ticket; pool de hilos |
| 2026-08-24 | Backend y GUI comunicados por HTTP; estructura de la web |
| 2026-08-14 | Creación del repositorio |
