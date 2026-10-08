# Barra — Web de venta (frontend)

Sitio donde los dueños de locales obtienen una licencia de Barra. Hecho con **React 18**,
**Vite**, **Tailwind CSS** y **React Router**. Le habla al [backend de la web](../barraWebBackend/README.md).

## Páginas

| Ruta | Archivo | Qué muestra |
|---|---|---|
| `/` | `src/pages/home.jsx` | Presentación y planes (`GET /api/planes`). Los planes no disponibles muestran «No disponible» |
| `/checkout/:planId` | `src/pages/checkout.jsx` | Nombre y email del comprador → `POST /api/compras`. Redirige a `/pago-exitoso` (plan gratis) o a Mercado Pago |
| `/pago-exitoso` | `src/pages/PagoExitoso.jsx` | «¡Listo!»: el mail con la licencia está en camino |
| `/pago-fallido` | `src/pages/PagoFallido.jsx` | El pago no se pudo procesar |
| `/recuperar-licencia` | `src/pages/RecuperarLicencia.jsx` | Reenvío del código de licencia por email (`POST /api/licencias/recuperar`) |
| `/admin` | `src/pages/admin.jsx` | Panel del equipo de Barra: ventas, licencias y revocación (ver [manual](../../docs/tecnico/manual-admin-web.md)) |

Las páginas de resultado son solo informativas: la licencia se emite cuando el backend recibe la
confirmación de Mercado Pago por webhook.

Otros archivos:

- `src/components/`: `header`, `footer`, `PlanCard`, `button`, `card`, `input`, `table`.
- `src/services/api.js`: todas las llamadas al backend.
- `public/downloads/`: los ejecutables de la app de escritorio (`barra-backend-vX.Y.Z.exe` y
  `barra-gui-vX.Y.Z.jar`). Vite los copia tal cual al build, así quedan en `/downloads/…`.

## Cómo correrlo

Requiere Node.js 18 o superior y el backend de la web corriendo.

```bash
npm ci
npm run dev        # http://localhost:5173
```

| Comando | Para qué |
|---|---|
| `npm run dev` | Servidor de desarrollo con recarga automática (puerto 5173) |
| `npm run build` | Build de producción en `dist/` |
| `npm run preview` | Sirve el build para revisarlo |

## Configuración

| Variable | Por defecto | Para qué |
|---|---|---|
| `VITE_API_URL` | `http://localhost:4000/api` | URL del backend. En producción, con el backend en el mismo dominio: `VITE_API_URL=/api npm run build` |

Se define al compilar (por ejemplo en `.env.local`); Vite la incorpora al build.

Despliegue en un servidor: [despliegue de la web](../../docs/tecnico/despliegue-web.md).

## Limitaciones conocidas

- No hay botón ni página de descarga: el link llega en el mail de licencia (`DOWNLOAD_URL` del backend).
- Un pago **pendiente** (por ejemplo, en efectivo) vuelve a `/pago-fallido`, que dice que no se generó ningún cargo.
- El panel `/admin` no tiene link desde la web y no tiene botón para salir.
- Faltan las páginas de términos y condiciones y de política de privacidad (los textos están en [`docs/legal/`](../../docs/legal/)).
