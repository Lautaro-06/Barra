# Despliegue de la web de venta

Cómo levantar en desarrollo y publicar en un servidor la web de venta de Barra:

| Parte | Carpeta | Tecnología | Puerto en desarrollo |
|---|---|---|---|
| Frontend | `barraPagina/barraWeb` | React 18 + Vite + Tailwind | 5173 |
| Backend | `barraPagina/barraWebBackend` | Node.js + Express | 4000 |
| Base de datos | — | MySQL 8 o MariaDB 10.11 | 3306 |

> Verificado el 08/10/2026 con Node 22 y MariaDB 10.11 (ver CW-01 a CW-17 y CG-14 en el
> [plan de pruebas](../proyecto/plan-de-pruebas.md)). El cobro real con Mercado Pago no se probó
> porque requiere credenciales de una cuenta.

---

## 1. Requisitos

- Node.js 18 o superior (con npm).
- MySQL 8 o MariaDB 10.11 o superior.
- Para cobrar: una cuenta de Mercado Pago con credenciales (de prueba o de producción).
- Para mandar los mails de licencia: una cuenta SMTP. Sin ella, en desarrollo los mails se muestran en la consola.

## 2. Base de datos

El backend crea solo las tablas (`planes`, `compradores`, `licencias`, `pagos`) y carga los tres
planes al arrancar. **La base y el usuario hay que crearlos a mano:**

```sql
CREATE DATABASE barra_web CHARACTER SET utf8mb4;
CREATE USER 'barra'@'localhost' IDENTIFIED BY 'una-contraseña-segura';
GRANT ALL ON barra_web.* TO 'barra'@'localhost';
FLUSH PRIVILEGES;
```

## 3. Variables de entorno del backend

Copiar `barraWebBackend/.env.example` a `barraWebBackend/.env` y completar:

| Variable | Ejemplo | Para qué |
|---|---|---|
| `PORT` | `4000` | Puerto del backend |
| `PUBLIC_URL` | `https://barra.com.ar` | URL pública del frontend. Se usa para las páginas de vuelta de Mercado Pago (`/pago-exitoso`, `/pago-fallido`) |
| `DB_HOST`, `DB_PORT` | `localhost`, `3306` | Servidor MySQL |
| `DB_USER`, `DB_PASSWORD`, `DB_NAME` | `barra`, `…`, `barra_web` | Credenciales y nombre de la base |
| `MP_ACCESS_TOKEN` | `APP_USR-…` o `TEST-…` | Access token de Mercado Pago. Sin él, la compra de planes pagos falla |
| `MP_WEBHOOK_SECRET` | `…` | Clave secreta de la firma de webhooks de Mercado Pago. **Sin ella se aceptan webhooks sin validar**: solo para desarrollo |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_SECURE` | `smtp.gmail.com`, `587`, `false` | Servidor de mail. `SMTP_SECURE=true` para el puerto 465. Sin `SMTP_HOST`, los mails se imprimen en la consola |
| `SMTP_USER`, `SMTP_PASS` | `ventas@…`, `…` | Credenciales SMTP (en Gmail, contraseña de aplicación) |
| `SMTP_FROM` | `Barra <no-reply@barra.com.ar>` | Remitente de los mails |
| `DOWNLOAD_URL` | `https://barra.com.ar/downloads/` | Link de descarga que va en el mail de licencia |
| `ADMIN_USER`, `ADMIN_PASSWORD` | `admin`, `…` | Usuario y contraseña del panel `/admin`. **Cambiar la contraseña de ejemplo** |

El `.env` tiene secretos: está en el `.gitignore` y nunca se sube al repositorio.

## 4. Levantar en desarrollo

```bash
# Backend
cd barraPagina/barraWebBackend
npm ci
npm run dev                  # http://localhost:4000/api (se reinicia solo al guardar)

# Frontend (en otra terminal)
cd barraPagina/barraWeb
npm ci
npm run dev                  # http://localhost:5173
```

- El frontend le habla a `http://localhost:4000/api` por defecto. Para otra URL, definir
  `VITE_API_URL` (por ejemplo en `barraWeb/.env.local`: `VITE_API_URL=http://localhost:4000/api`).
- Para probar rápido: `curl http://localhost:4000/api/health` → `{"ok":true}`; `curl http://localhost:4000/api/planes` → los tres planes.

## 5. Mercado Pago

1. En el panel de desarrolladores de Mercado Pago, crear una aplicación y copiar el **Access Token** (de prueba para desarrollo) en `MP_ACCESS_TOKEN`.
2. Configurar el **webhook** de la aplicación:
   - URL: `https://<tu-dominio>/api/pagos/webhook`
   - Evento: **Pagos**
   - Copiar la **clave secreta** que muestra Mercado Pago en `MP_WEBHOOK_SECRET`.
3. **En desarrollo**, Mercado Pago necesita una URL pública para el webhook y las páginas de
   vuelta. Usar un túnel (por ejemplo `cloudflared` o `ngrok`) hacia los puertos 4000 y 5173, y
   poner la URL pública del frontend en `PUBLIC_URL`.
4. Probar con las tarjetas de prueba de Mercado Pago y verificar en `/admin` que el pago pasa a
   «aprobado» y que llega el mail con la licencia.

**Cómo fluye un pago:** `POST /api/compras` crea un pago «pendiente» y la preferencia → el
comprador paga en Mercado Pago → Mercado Pago llama al webhook → el backend verifica la firma,
consulta el pago y, si está aprobado, emite la licencia y manda el mail. Las páginas de vuelta son
solo visuales: **la fuente de verdad es el webhook**.

### Habilitar los planes pagos

Hoy Pro y Max tienen precios de prueba y están deshabilitados. Con los precios del
[presupuesto](../proyecto/presupuesto.md):

```sql
UPDATE planes SET precio_ars = 12000,  disponible = true WHERE nombre = 'Pro';
UPDATE planes SET precio_ars = 120000, disponible = true WHERE nombre = 'Max';
```

## 6. Producción

Ejemplo para un servidor Ubuntu (por ejemplo un VPS de 1 GB) con un dominio apuntando a él.

### 6.1 Frontend

```bash
cd barraPagina/barraWeb
npm ci
VITE_API_URL=/api npm run build      # genera dist/ (incluye dist/downloads/ con los ejecutables)
```

`VITE_API_URL=/api` hace que el frontend llame al backend en el mismo dominio.

### 6.2 Backend como servicio (systemd)

`/etc/systemd/system/barra-web.service`:

```ini
[Unit]
Description=Barra - backend de la web de venta
After=network.target mysql.service

[Service]
WorkingDirectory=/opt/barra/barraPagina/barraWebBackend
ExecStart=/usr/bin/node src/app.js
Restart=always
User=barra
Environment=NODE_ENV=production

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable --now barra-web
```

### 6.3 Nginx

`/etc/nginx/sites-available/barra`:

```nginx
server {
    server_name barra.com.ar;
    root /opt/barra/barraPagina/barraWeb/dist;

    # React Router: cualquier ruta que no sea un archivo vuelve a index.html
    location / {
        try_files $uri /index.html;
    }

    # Listado de descargas (el link del mail de licencia apunta acá)
    location /downloads/ {
        autoindex on;
    }

    # Backend
    location /api/ {
        proxy_pass http://127.0.0.1:4000;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

Certificado SSL gratuito con Let's Encrypt: `sudo certbot --nginx -d barra.com.ar`.

### 6.4 Respaldo de la base

Copia diaria con `cron` (ejemplo a las 4 de la mañana, conservando 14 días):

```bash
0 4 * * * mysqldump barra_web | gzip > /var/backups/barra_web_$(date +\%F).sql.gz && find /var/backups -name 'barra_web_*.sql.gz' -mtime +14 -delete
```

## 7. Lista de verificación antes de publicar

- [ ] `ADMIN_PASSWORD` cambiada (no la del ejemplo).
- [ ] `MP_WEBHOOK_SECRET` cargada (sin ella se aceptan webhooks sin firma).
- [ ] `MP_ACCESS_TOKEN` de **producción** y webhook configurado con la URL pública.
- [ ] SMTP configurado y probado (comprar el plan Gratis con un email propio).
- [ ] `PUBLIC_URL` y `DOWNLOAD_URL` con el dominio real, en `https`.
- [ ] Los ejecutables de la versión vigente en `public/downloads/` (ver [compilación](compilacion-y-empaquetado.md#3-publicar-una-versión)).
- [ ] Precios y planes habilitados como corresponde.
- [ ] Términos y condiciones y política de privacidad publicados (ver [`docs/legal/`](../legal/)).
- [ ] Backup diario de MySQL funcionando.
