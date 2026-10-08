# Documentación de Barra

Índice de toda la documentación del proyecto. Versión del sistema: **1.0.0** (rama `development`).

## Para quien usa Barra

| Documento | Contenido |
|---|---|
| [Manual de usuario](manual-de-usuario.md) | Instalación, primer uso, uso diario con capturas, reportes, sin internet, backups, problemas frecuentes y limitaciones |

## Entregables del proyecto

Los 20 puntos del documento de referencia, en orden:

| § | Entregable | Documento |
|---|---|---|
| 1, 7–20 | Arquitectura, análisis del sistema actual, actores, requisitos, FODA, Gantt, casos de uso, alcance, estado del arte, propuesta de solución, DER, tecnologías, Scrum, estructura del código y guía de desarrollo | [Documento del proyecto](proyecto/documento-del-proyecto.md) |
| 2 | Presentación del producto (diapositivas, guion y video de respaldo) | [Presentación](proyecto/presentacion.md) · [video](presentacion/demo-barra.mp4) |
| 3 | Manual de usuario | [Manual de usuario](manual-de-usuario.md) |
| 4 | Presupuesto inicial | [Presupuesto](proyecto/presupuesto.md) |
| 5 | Propuesta formal al cliente | [Propuesta formal](proyecto/propuesta-formal.md) |
| 6 | Entrevistas y encuestas | [Instrumentos de relevamiento](proyecto/entrevistas-y-encuestas.md) |
| 12 | Casos de uso (especificación completa) | [Casos de uso](proyecto/casos-de-uso.md) |
| — | Pruebas integrales (tarea M del Gantt) | [Plan de pruebas y resultados](proyecto/plan-de-pruebas.md) |

## Documentación técnica

| Documento | Para quién |
|---|---|
| [Compilación y empaquetado](tecnico/compilacion-y-empaquetado.md) | Generar el `.exe` del backend y el `.jar` de la GUI, y publicar una versión |
| [Despliegue de la web](tecnico/despliegue-web.md) | Levantar la web de venta: MySQL, `.env`, Mercado Pago, producción |
| [Manual del panel de administración web](tecnico/manual-admin-web.md) | El equipo que vende Barra: ventas, licencias, revocación |
| [Decisiones de arquitectura](tecnico/decisiones.md) | Por qué se eligió cada tecnología y diseño (ADR-001 a ADR-011) |
| [Especificación de la web de venta](specs/barra-pagina.md) | Flujo de compra, licencias y esquema MySQL |

Cada carpeta de código tiene su README: [backend](../application/barra-backend/README.md),
[GUI](../application/barra-gui/README.md), [web](../barraPagina/barraWeb/README.md),
[backend web](../barraPagina/barraWebBackend/README.md) y [pruebas](../pruebas/README.md).

## Legales (borradores)

| Documento | Estado |
|---|---|
| [Términos y condiciones](legal/terminos-y-condiciones.md) | Borrador: completar datos y revisar con un profesional |
| [Política de privacidad](legal/politica-de-privacidad.md) | Borrador: completar datos y revisar con un profesional |

## Seguimiento

| Documento | Contenido |
|---|---|
| [Revisión de la documentación](revision-documentacion.md) | Falencias detectadas el 08/10/2026 y dónde se resolvió cada una |
| [Historial de versiones](../CHANGELOG.md) | Cambios por versión |

## Lo que tiene que completar el equipo

Datos que no se pueden inventar y quedaron marcados con `[completar]` o como supuestos:

- **Nombre del Product Owner** ([documento del proyecto, §18](proyecto/documento-del-proyecto.md#181-roles)).
- **Resultados de las entrevistas y encuestas** ([§8 del relevamiento](proyecto/entrevistas-y-encuestas.md#8-resultados)).
- **Fecha de la entrega y defensa** (tarea S del Gantt).
- **Datos del cliente** en la [propuesta formal](proyecto/propuesta-formal.md) y **datos legales** (razón social, CUIT, contacto) en los borradores legales.
- **Supuestos del presupuesto** para validar: valor hora ($7.000), horas reales y arancel del dominio.
- **Compartir las diapositivas** desde su página para que el resto del grupo pueda abrirlas.
