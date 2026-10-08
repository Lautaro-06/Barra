# Propuesta formal

<br>

**Barra — Sistema de gestión de pedidos para tu local**

| | |
|---|---|
| Para | **[Nombre del local]** — [Nombre del dueño o responsable] |
| De | Equipo Barra — Grupo 1, Programación sobre Redes |
| Integrantes | Sofía Power, Mauro Beltrán, Lautaro Palombo, Thomas Barrera Fuentes |
| Fecha | 8 de octubre de 2026 |
| Validez de la propuesta | 30 días |

> Los campos entre corchetes se completan con los datos del local al que se presenta la
> propuesta. El resto del documento sirve tal cual para cualquier local gastronómico chico.

---

## 1. Resumen ejecutivo

Barra ordena los pedidos del local: el cajero o el mozo los carga en una pantalla táctil, la
cocina los ve al instante y el dueño controla el stock y recibe cada noche un resumen de ventas
por email. Funciona en la computadora del local, sin depender de internet. Se contrata por
período, con una prueba gratis de 10 días y un plan mensual de **$12.000**.

---

## 2. El problema

En el relevamiento de locales chicos (rotiserías, cafés, locales al paso) aparecieron estos
problemas:

- Los pedidos se anotan a mano o se dictan a la cocina, y se pierden o se confunden en las horas pico.
- No queda registro de qué se vendió cada día.
- El stock no se controla, o se controla tarde: uno se entera de que falta algo cuando un cliente lo pide.
- Lo que se cobra en una mesa no está conectado con lo que se preparó.
- No hay datos para decidir qué producto reforzar o cuándo sumar personal.
- Si se pierde el cuaderno o la planilla, se pierde todo.

---

## 3. La solución

| Necesidad del local | Qué hace Barra |
|---|---|
| Tomar pedidos sin errores | Pantalla **Vender** táctil: se tocan los productos y el total se calcula solo |
| Atender mesas | Pantalla **Mesas**: cuenta por mesa, rondas, cierre y **ticket imprimible** |
| Que la cocina no se pierda nada | Pantalla **Cocina**: pedidos en preparación, listos y entregados |
| Controlar el stock | Se descuenta con cada pedido; **alerta por email** cuando un producto baja de su mínimo |
| Saber cuánto se vendió | **Resumen diario por email**: total, pedidos, ticket promedio, mostrador y mesas, productos más vendidos |
| No depender de internet | Todo funciona en la PC del local; solo los emails esperan a que vuelva la conexión |
| No perder datos | **Copias de seguridad automáticas** cada 4 horas |

Barra **no** tiene funciones de cara al comensal (no le manda avisos ni recibe pedidos online):
es una herramienta interna del local.

---

## 4. Alcance

**Incluye:**

- Licencia de uso de Barra para una PC del local durante el período contratado.
- Aplicación de escritorio con las pantallas Vender, Mesas, Cocina y Admin.
- Alertas de stock bajo y resumen diario por email.
- Copias de seguridad automáticas locales.
- Asistencia para la instalación y la carga inicial del catálogo (ver §5).
- Actualizaciones de la versión mientras el plan esté vigente.
- Soporte según el §7.

**No incluye:**

- Hardware (PC, monitor táctil, impresora).
- Facturación electrónica ni reemplazo de un controlador fiscal.
- Registro de medios de pago o arqueo de caja.
- Pedidos online, delivery o comunicación con el comensal.
- Varias PC o sucursales conectadas entre sí.

**Requisitos del local:** PC con Windows 10 u 11 de 64 bits, Java 17 o superior (se instala en la
puesta en marcha), una cuenta de email para recibir alertas y, opcional, una impresora.

---

## 5. Cronograma

**Desarrollo:** el sistema ya está desarrollado y probado (versión 1.0.0, octubre de 2026). El
cronograma completo del proyecto está en el
[Gantt del documento del proyecto](documento-del-proyecto.md#11-diagrama-de-gantt).

**Puesta en marcha en el local:**

| Día | Actividad | Responsable |
|---|---|---|
| 1 | Obtener la licencia en la web, instalar Java y Barra, primer arranque | Equipo Barra + dueño |
| 1 | Cargar nombre del local, datos del dueño y mesas del salón | Equipo Barra + dueño |
| 2 | Cargar el catálogo con precios, stock y umbrales | Dueño (con asistencia) |
| 2 | Configurar el email y probar alertas y resumen | Equipo Barra |
| 3 | Capacitación de 1 hora para cajero, mozos y cocina (con el [manual de usuario](../manual-de-usuario.md)) | Equipo Barra |
| 4 a 10 | Uso en producción con el plan Gratis; seguimiento y ajustes | Local + equipo Barra |
| 11 | Pase al plan Pro o Max | Dueño |

---

## 6. Presupuesto

| Plan | Duración | Precio | Incluye |
|---|---|---|---|
| Gratis | 10 días | $0 | Todo el sistema, para probarlo en el local |
| **Pro** | 30 días | **$12.000** | Sistema completo, actualizaciones y soporte |
| **Max** | 365 días | **$120.000** | Lo mismo que Pro, con 2 meses sin cargo |

- Puesta en marcha (instalación, carga inicial y capacitación): **sin cargo** durante el lanzamiento.
- Pago con Mercado Pago (tarjeta, débito, dinero en cuenta).
- Precios finales en pesos, vigentes durante la validez de esta propuesta.
- Como referencia, el plan más barato de una suite gastronómica en la nube líder del mercado cuesta $22.500 por mes.

El detalle de costos y la justificación del precio están en el [presupuesto](presupuesto.md).

---

## 7. Soporte y mantenimiento

| Tema | Condición |
|---|---|
| Canal | Email y WhatsApp del equipo Barra |
| Horario | Lunes a viernes de 9 a 18 h |
| Tiempo de primera respuesta | Hasta 24 horas hábiles; problemas que impiden vender, en el mismo día hábil |
| Actualizaciones | Incluidas mientras el plan esté vigente |
| Correcciones de errores | Incluidas, sin cargo |
| Pedidos de nuevas funciones | Se evalúan y se priorizan en la planificación del producto |
| Respaldo de datos | Automático y local; se recomienda al local copiar la carpeta `backups` a un pendrive o a la nube una vez por semana |
| Fin del plan | Los datos siguen siendo del local y quedan en su PC |

---

## 8. Próximas versiones

Ya planificadas: activación de la licencia dentro de la app, modificación y cancelación de
pedidos, pantalla de historial y reportes, e instalador único que no requiera instalar Java.

---

## 9. Aceptación

Para avanzar, alcanza con responder a esta propuesta confirmando la fecha de la puesta en marcha.

| | |
|---|---|
| Por el local | Firma: ____________________ Aclaración: ____________________ Fecha: ___/___/______ |
| Por el equipo Barra | Firma: ____________________ Aclaración: ____________________ Fecha: ___/___/______ |
