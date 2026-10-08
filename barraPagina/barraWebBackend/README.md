# Barra — Backend de la web de venta

API de la web de venta: compras, emisión y validación de licencias, notificaciones de Mercado
Pago y panel de administración. Hecho con **Node.js + Express** y **MySQL**.

Especificación funcional: [`docs/specs/barra-pagina.md`](../../docs/specs/barra-pagina.md).

## Cómo correrlo

Requiere Node.js 18 o superior y MySQL 8 o MariaDB 10.11 o superior.

1. Crear la base y un usuario (las tablas y los planes se crean solos al arrancar):

   ```sql
   CREATE DATABASE barra_web CHARACTER SET utf8mb4;
   CREATE USER 'barra'@'localhost' IDENTIFIED BY 'una-contraseña';
   GRANT ALL ON barra_web.* TO 'barra'@'localhost';
   ```

2. Copiar `.env.example` a `.env` y completarlo (ver la tabla de abajo).
3. Instalar y arrancar:

   ```bash
   npm ci
   npm run dev        # http://localhost:4000/api, se reinicia solo al guardar
   ```

| Comando | Para qué |
|---|---|
| `npm run dev` | Desarrollo con recarga automática (`node --watch`) |
| `npm start` | Producción |

Prueba rápida: `curl http://localhost:4000/api/health` → `{"ok":true}`.

## Variables de entorno (`.env`)

| Variable | Para qué |
|---|---|
| `PORT` | Puerto (por defecto 4000) |
| `PUBLIC_URL` | URL pública del frontend, para las páginas de vuelta de Mercado Pago |
| `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME` | Conexión a MySQL |
| `MP_ACCESS_TOKEN` | Access token de Mercado Pago (de prueba o producción) |
| `MP_WEBHOOK_SECRET` | Clave para verificar la firma de los webhooks. Sin ella se aceptan webhooks sin validar (solo desarrollo) |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_SECURE`, `SMTP_USER`, `SMTP_PASS`, `SMTP_FROM` | Envío de mails. Sin `SMTP_HOST`, los mails se imprimen en la consola |
| `DOWNLOAD_URL` | Link de descarga que va en el mail de licencia |
| `ADMIN_USER`, `ADMIN_PASSWORD` | Acceso al panel de administración |

Detalle y valores de ejemplo en [despliegue de la web](../../docs/tecnico/despliegue-web.md#3-variables-de-entorno-del-backend).

## Endpoints

Todos bajo `/api`.

| Método | Ruta | Para qué |
|---|---|---|
| GET | `/health` | Comprobar que el backend responde |
| GET | `/planes` | Lista de planes (Gratis, Pro, Max) |
| POST | `/compras` | `{ plan_id, comprador: { nombre, email } }`. Plan gratis: emite la licencia y responde `{ redirect: "/pago-exitoso" }`. Plan pago: crea la preferencia y responde la URL de Mercado Pago. Plan no disponible: 409 |
| POST | `/pagos/webhook` | Notificación de Mercado Pago. Verifica la firma, consulta el pago y, si está aprobado, emite la licencia. Siempre responde 200 |
| POST | `/licencias/activar` | `{ codigo, secret }`. Registra una activación y fija el vencimiento. 401 clave incorrecta, 404 código inexistente, 403 sin activaciones |
| GET | `/licencias/estado?codigo=…&secret=…` | `{ valido, fecha_vencimiento }` |
| POST | `/licencias/recuperar` | `{ email }`. Reenvía el **código** (nunca la clave). Misma respuesta exista o no el email |
| GET | `/admin/ventas` | Ventas con comprador y plan (requiere autenticación Basic) |
| GET | `/admin/licencias` | Licencias con comprador y plan (requiere autenticación Basic) |
| POST | `/admin/licencias/:id/revocar` | Deja la licencia sin activaciones (requiere autenticación Basic) |

## Estructura

| Carpeta / archivo | Qué hay |
|---|---|
| `src/app.js` | Servidor Express, rutas y manejo de errores |
| `src/db.js` | Pool de MySQL, creación de tablas y planes iniciales |
| `src/routes/`, `src/controllers/` | Rutas y lógica de cada endpoint |
| `src/models/` | `plan`, `comprador`, `licencia`, `pago` |
| `src/services/emailService.js` | Mails de licencia y de recuperación (nodemailer) |
| `src/services/mercadoPagoService.js` | Preferencias, consulta de pagos y verificación de firma del webhook |
| `src/middleware/adminAuth.js` | Autenticación Basic del panel, con comparación en tiempo constante |
| `src/utils/licenciaUtils.js` | Código `BARRA-XXXX-XXXX-XXXX`, clave secreta y hash bcrypt |

## Seguridad

- La clave secreta de cada licencia se manda una sola vez y en la base solo queda su **hash bcrypt**.
- Los webhooks se aceptan solo con una firma `x-signature` válida (HMAC-SHA256 con `MP_WEBHOOK_SECRET`).
- La fuente de verdad del pago es el webhook, no la página de vuelta del navegador.
- `.env` está en el `.gitignore`: nunca subir credenciales al repositorio.

## Pruebas

17 casos automatizados contra una base de prueba propia (`barra_web_pruebas`):

```bash
python ../../pruebas/test_web.py
```

Ver [`pruebas/README.md`](../../pruebas/README.md) y los resultados en el
[plan de pruebas](../../docs/proyecto/plan-de-pruebas.md).

## Limitaciones conocidas

- Un mismo email puede obtener el plan Gratis varias veces.
- Un pago pendiente redirige a la página de pago fallido (`back_urls.pending`).
- No hay forma de emitir una licencia manual ni de reenviar el mail de licencia desde el panel.
