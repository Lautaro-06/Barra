# Barra — Sistema de Pedidos

Sistema de gestión **interno** para locales gastronómicos chicos: toma de pedidos de mostrador y
de mesa, tablero de cocina, control de stock con alertas por email al dueño, resumen diario de
ventas y copias de seguridad automáticas. Funciona en la PC del local, sin depender de internet.
Se vende con licencia a través de una web propia.

Proyecto de **Programación sobre Redes — Grupo 1**: Sofía Power (Scrum Master), Mauro Beltrán,
Lautaro Palombo y Thomas Barrera Fuentes.

**Versión actual:** 1.0.0 — ver [CHANGELOG](CHANGELOG.md).

![Pantalla Vender](docs/img/manual/01-vender-pedido.png)

---

## Componentes

| Carpeta | Qué es | Tecnología |
|---|---|---|
| [`application/barra-backend`](application/barra-backend) | Backend local: lógica de negocio, hilos, emails. Único dueño de la base | Python, FastAPI, SQLite |
| [`application/barra-gui`](application/barra-gui) | App de escritorio: pantallas Vender, Mesas, Cocina y Admin | Java 17, Swing, Maven |
| [`barraPagina/barraWeb`](barraPagina/barraWeb) | Web de venta: planes, compra, recuperar licencia, panel admin | React, Vite, Tailwind |
| [`barraPagina/barraWebBackend`](barraPagina/barraWebBackend) | Backend de la web: compras, licencias, Mercado Pago | Node.js, Express, MySQL |
| [`pruebas`](pruebas) | Campaña de pruebas automatizada | Python |
| [`docs`](docs) | Toda la documentación | Markdown |

```text
GUI (Java) ──HTTP 127.0.0.1:8000──▶ Backend local (Python) ──▶ barra.db (SQLite)
                                          └──▶ email del dueño (alertas y resumen)

Web de venta (React) ──/api──▶ Backend web (Node) ──▶ MySQL
                                     ├──▶ Mercado Pago (cobro + webhook)
                                     └──▶ email del comprador (licencia)
```

## Para usar Barra

Si sos dueño o empleado de un local, todo está en el **[manual de usuario](docs/manual-de-usuario.md)**:
instalación, primer uso, uso diario, reportes y problemas frecuentes.

## Para desarrollar

| Parte | Cómo levantarla | Queda en |
|---|---|---|
| Backend local | `cd application/barra-backend` → `python -m venv venv` → activar → `pip install -r requirements.txt` → `python run_backend.py` | `http://127.0.0.1:8000` (API en `/docs`) |
| GUI | `cd application/barra-gui` → `mvn package` → `java -jar target/barra-gui-1.0.0.jar` | Ventana de escritorio |
| Backend web | `cd barraPagina/barraWebBackend` → `npm ci` → `.env` desde `.env.example` → `npm run dev` | `http://localhost:4000/api` |
| Web de venta | `cd barraPagina/barraWeb` → `npm ci` → `npm run dev` | `http://localhost:5173` |

Requisitos: Python 3.10+, JDK 17+ y Maven, Node.js 18+, MySQL 8 o MariaDB 10.11+. Detalle en el
README de cada carpeta y en:

- [Compilación y empaquetado](docs/tecnico/compilacion-y-empaquetado.md) (generar el `.exe` y el `.jar`)
- [Despliegue de la web](docs/tecnico/despliegue-web.md) (MySQL, `.env`, Mercado Pago, producción)

**Pruebas:** `python pruebas/test_escritorio.py` y `python pruebas/test_web.py` (ver [`pruebas/README.md`](pruebas/README.md)).

## Documentación

Índice completo en [`docs/README.md`](docs/README.md). Lo principal:

| Documento | Para quién |
|---|---|
| [Manual de usuario](docs/manual-de-usuario.md) | Locales que usan Barra |
| [Documento del proyecto](docs/proyecto/documento-del-proyecto.md) | Evaluación del proyecto: arquitectura, requisitos, DER, Gantt, FODA y más |
| [Plan de pruebas](docs/proyecto/plan-de-pruebas.md) | Resultados de las pruebas (64 de 65 casos) |
| [Decisiones de arquitectura](docs/tecnico/decisiones.md) | Equipo de desarrollo |

## Ramas

Se trabaja sobre `development`; cada funcionalidad se desarrolla en su rama (`app/concurrence`,
`app/functions`, `appView`) y se integra a `development` con un merge.
