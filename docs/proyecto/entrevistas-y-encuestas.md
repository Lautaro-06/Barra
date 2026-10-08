# Entrevistas y encuestas — Barra

Instrumentos de relevamiento del proyecto: guía de entrevista al dueño, encuesta corta para varios
locales, planillas para registrar las respuestas y método para convertirlas en conclusiones.

> **Estado:** instrumentos listos para aplicar. La sección [8. Resultados](#8-resultados) se
> completa con las respuestas reales. No se cargan datos inventados: si el relevamiento ya se
> hizo, hay que pasar las respuestas a las planillas de las secciones 5 y 6.

---

## 1. Objetivos

1. Confirmar los problemas del sistema actual (D1 a D6 del [documento del proyecto](documento-del-proyecto.md#7-análisis-del-sistema-actual)).
2. Validar las decisiones de diseño: funcionamiento sin internet, mesas, alertas por email, una sola PC.
3. Conocer cuánto pagaría un local y si prefiere pago único o suscripción (insumo del [presupuesto](presupuesto.md)).
4. Relevar el estado del arte: qué sistemas usan hoy los locales y qué les falta.

### Hipótesis a validar

| ID | Hipótesis | Si se confirma | Si no se confirma |
|---|---|---|---|
| H1 | En horas pico se pierden o confunden pedidos | Justifica RF01–RF04 | Reforzar otro valor (stock, reportes) en la venta |
| H2 | No hay registro digital de ventas | Justifica RF07 y RF12 | Revisar con qué sistema registran y cómo integrarse |
| H3 | El stock se controla a mano o no se controla | Justifica RF05, RF06 y RF08 | Bajar la prioridad de las alertas |
| H4 | El local tiene una PC y su internet no es 100 % estable | Justifica RNF02 y la base local | Evaluar una versión web |
| H5 | Más de la mitad de los locales atiende mesas | Justifica RF09 en el alcance | Ofrecer mesas como opcional |
| H6 | El email es un canal aceptable para alertas | Mantener solo email | Priorizar WhatsApp o Telegram (RF23) |
| H7 | Prefieren suscripción y pagarían al menos $12.000 por mes | Mantener el precio propuesto | Ajustar el precio o el modelo |
| H8 | Con una sola PC alcanza (cajero y cocina juntos) | Mantener el alcance | Priorizar varias PC en red |

---

## 2. A quién relevar

| Instrumento | A quién | Cantidad mínima sugerida | Cómo |
|---|---|---|---|
| Entrevista | Dueño o encargado de un local gastronómico chico (rotisería, café, local al paso, cervecería) | 3 locales | Presencial, 20–30 minutos, en un horario tranquilo |
| Encuesta | Dueños o encargados de locales similares | 15 respuestas | Formulario online (Google Forms o similar) por WhatsApp o en persona |

Conviene que al menos uno de los locales entrevistados atienda mesas y otro trabaje solo de
mostrador.

**Texto de consentimiento** (leerlo al inicio de la entrevista y ponerlo al principio de la encuesta):

> Somos estudiantes y estamos desarrollando Barra, un sistema para ordenar los pedidos de locales
> gastronómicos. Te vamos a hacer unas preguntas sobre cómo trabajan hoy. Las respuestas se usan
> solo para este proyecto, no se publican datos que identifiquen al local y podés no responder
> cualquier pregunta. ¿Estás de acuerdo?

---

## 3. Guía de entrevista al dueño

Para cada pregunta, la columna «Para qué» indica qué hipótesis o parte del proyecto alimenta.

### Bloque 1 — El local

| # | Pregunta | Repreguntas | Para qué |
|---|---|---|---|
| 1.1 | ¿Qué tipo de local es y hace cuánto funciona? | ¿Cuántas personas trabajan? ¿En qué turnos? | Perfil |
| 1.2 | ¿Atienden mesas, mostrador, para llevar, delivery? | ¿Cuántas mesas tienen? | H5 |
| 1.3 | ¿Cuántas personas usarían un sistema y en qué rol? | ¿Quién cobra? ¿Quién cocina? ¿El dueño está en el local? | Actores (§8) |

### Bloque 2 — Pedidos

| # | Pregunta | Repreguntas | Para qué |
|---|---|---|---|
| 2.1 | ¿Cómo toman los pedidos hoy (mostrador, teléfono, WhatsApp)? | ¿Cómo llega el pedido a la cocina? | D1 |
| 2.2 | ¿Cuántos pedidos manejan en un día normal y en uno pico? | ¿A qué hora es el pico? | H1, dimensionamiento |
| 2.3 | ¿Se les pierden o confunden pedidos? ¿Con qué frecuencia? | Contame la última vez que pasó. ¿Qué costó? | H1 |
| 2.4 | ¿Cómo saben qué mesa debe qué cuando piden la cuenta? | ¿Se equivocaron alguna vez al cobrar? | D4, H5 |

### Bloque 3 — Stock

| # | Pregunta | Repreguntas | Para qué |
|---|---|---|---|
| 3.1 | ¿Llevan algún control de stock? ¿Cómo? | ¿Quién lo actualiza? ¿Cada cuánto? | H3 |
| 3.2 | ¿Les pasó quedarse sin un producto en medio del servicio? | ¿Cómo se enteraron? | H3 |
| 3.3 | ¿Cómo les gustaría enterarse de que un producto se está por agotar? | ¿Email, WhatsApp, Telegram, un aviso en pantalla? | H6 |

### Bloque 4 — Información del negocio

| # | Pregunta | Repreguntas | Para qué |
|---|---|---|---|
| 4.1 | ¿Saben cuánto vendieron ayer? ¿Cómo lo calculan? | ¿Lo anotan en algún lado? | H2 |
| 4.2 | ¿Qué información les gustaría ver de su negocio? | Ventas por día, producto más pedido, horarios pico | RF07, RF22 |
| 4.3 | ¿Cuándo y dónde les gustaría recibir ese resumen? | ¿Al cierre? ¿Al día siguiente? | RF12 |

### Bloque 5 — Tecnología

| # | Pregunta | Repreguntas | Para qué |
|---|---|---|---|
| 5.1 | ¿Qué dispositivos tienen (PC, notebook, tablet)? | ¿Con qué sistema operativo? ¿Pantalla táctil? ¿Impresora? | Requisitos del equipo, H8 |
| 5.2 | ¿Qué tan estable es su internet? | ¿Se les corta? ¿Cuánto tiempo? | H4 |
| 5.3 | ¿Usan o usaron algún sistema? ¿Cuál? | ¿Qué les gustó? ¿Por qué lo dejaron? | Estado del arte (§14) |
| 5.4 | ¿La cocina tiene o podría tener su propia pantalla? | ¿En la misma PC (segundo monitor) o en otra? | H8 |

### Bloque 6 — Precio

| # | Pregunta | Repreguntas | Para qué |
|---|---|---|---|
| 6.1 | ¿Preferirían pagar una vez o una suscripción mensual? | ¿Por qué? | H7 |
| 6.2 | ¿Cuánto les parece razonable pagar por mes por un sistema así? | Si costara $12.000 por mes, ¿lo pagarías? | H7 |
| 6.3 | ¿Qué los haría probar un sistema nuevo? | ¿Una prueba gratis? ¿Que alguien lo instale? | Propuesta formal |

**Cierre:** mostrar la demo de 2 minutos (Vender → Cocina → alerta) y preguntar: «¿Qué es lo
primero que cambiarías o agregarías?».

---

## 4. Encuesta corta

Lista para copiar en un formulario online. Tiempo estimado: 3 minutos.

1. **Rubro del local** *(opción única)*: Rotisería · Café / bar · Local al paso / comida rápida · Restaurante · Cervecería · Panadería / confitería · Otro: ______
2. **¿Cuántas personas trabajan?** *(opción única)*: 1–2 · 3–5 · 6–10 · Más de 10
3. **¿Cómo atienden?** *(varias opciones)*: Mostrador / para llevar · Mesas · Delivery propio · Apps de delivery · Pedidos por WhatsApp
4. **Pedidos en un día normal** *(opción única)*: Menos de 30 · 30–80 · 80–150 · Más de 150
5. **¿Con qué frecuencia se pierde o confunde un pedido?** *(opción única)*: Nunca · Una vez por mes · Una vez por semana · Varias veces por semana
6. **¿Cómo controlan el stock?** *(opción única)*: No lo controlamos · A ojo · Cuaderno o planilla · Con un sistema
7. **¿Usan algún sistema digital para los pedidos?** *(opción única)*: No · Sí, ¿cuál? ______
8. **¿Qué dispositivos tienen en el local?** *(varias opciones)*: PC de escritorio · Notebook · Tablet · Solo celular · Impresora
9. **¿Qué tan estable es su internet?** *(escala 1 a 5, de «se corta seguido» a «nunca falla»)*
10. **¿Por dónde preferirían recibir avisos de stock bajo?** *(opción única)*: Email · WhatsApp · Telegram · Aviso en la pantalla del sistema · No me interesa
11. **¿Probarían un sistema nuevo?** *(opción única)*: Sí · Sí, con una prueba gratis · Sí, si alguien me lo instala · No
12. **¿Prefieren pagar una vez o por mes?** *(opción única)*: Una vez · Por mes · Por año con descuento
13. **¿Cuánto les parece razonable pagar por mes?** *(opción única)*: Nada · Hasta $8.000 · $8.000–$15.000 · $15.000–$25.000 · Más de $25.000
14. **¿Querés que te avisemos cuando Barra esté disponible?** *(opcional)*: Email o teléfono: ______

---

## 5. Planilla de registro de entrevistas

Una tabla por entrevista. Anotar frases textuales entre comillas: después sirven para la
presentación.

| Campo | Entrevista N.º __ |
|---|---|
| Fecha y lugar | |
| Entrevistador/a | |
| Local (rubro, sin nombre si no autorizó) | |
| Personas que trabajan / roles | |
| Atiende mesas (cuántas) | |
| Cómo toman pedidos hoy | |
| Pedidos por día (normal / pico) | |
| Pérdidas o confusiones (frecuencia, ejemplo) | |
| Control de stock actual | |
| Canal preferido para alertas | |
| Información que quiere ver | |
| Dispositivos y estabilidad de internet | |
| Sistemas usados antes | |
| Preferencia de pago y monto razonable | |
| Reacción a la demo / qué cambiaría | |
| Frases textuales | |

---

## 6. Planilla de tabulación de la encuesta

Exportar las respuestas del formulario a una planilla y resumirlas así:

| Pregunta | Respuesta | Cantidad | % |
|---|---|---|---|
| P3 Atienden mesas | Sí | | |
| P5 Pierden pedidos semanalmente o más | Sí | | |
| P6 Controlan stock con cuaderno, a ojo o no lo controlan | Sí | | |
| P8 Tienen PC o notebook | Sí | | |
| P9 Internet inestable (1–3) | Sí | | |
| P10 Canal preferido | Email / WhatsApp / Telegram / Pantalla | | |
| P12 Prefieren pagar por mes o por año | Sí | | |
| P13 Pagarían $8.000 o más por mes | Sí | | |

---

## 7. Cómo analizar los resultados

1. **Contar** las respuestas de la encuesta con la planilla de la sección 6.
2. **Agrupar** lo dicho en las entrevistas por bloque y marcar las frases que se repiten en dos o más locales.
3. **Decidir cada hipótesis** con este criterio:

   | Hipótesis | Se confirma si… |
   |---|---|
   | H1 | ≥ 50 % pierde o confunde pedidos al menos una vez por semana (P5) o lo cuentan 2 de 3 entrevistados |
   | H2 | ≥ 50 % no tiene sistema digital (P7) |
   | H3 | ≥ 60 % controla el stock a ojo, en cuaderno o no lo controla (P6) |
   | H4 | ≥ 70 % tiene PC o notebook (P8) y ≥ 30 % califica su internet con 3 o menos (P9) |
   | H5 | ≥ 50 % atiende mesas (P3) |
   | H6 | Email es la primera o segunda opción más elegida (P10) |
   | H7 | ≥ 50 % prefiere pagar por mes o por año (P12) y ≥ 50 % pagaría $8.000 o más (P13) |
   | H8 | Ningún entrevistado considera indispensable una segunda PC para la cocina (5.4) |

4. **Volcar las conclusiones** en la sección 8 y, si alguna hipótesis no se confirma, actualizar los
   requisitos (§9), el alcance (§13), el FODA (§10) o el precio del [presupuesto](presupuesto.md).

---

## 8. Resultados

*Completar con los datos reales del relevamiento.*

### 8.1 Ficha del relevamiento

| | |
|---|---|
| Fechas | |
| Entrevistas realizadas | |
| Respuestas de la encuesta | |
| Rubros alcanzados | |

### 8.2 Hipótesis

| Hipótesis | Resultado (dato) | ¿Se confirma? | Decisión para el proyecto |
|---|---|---|---|
| H1 Pérdida de pedidos | | | |
| H2 Sin registro de ventas | | | |
| H3 Stock manual | | | |
| H4 PC e internet inestable | | | |
| H5 Atienden mesas | | | |
| H6 Email como canal | | | |
| H7 Suscripción y precio | | | |
| H8 Una sola PC | | | |

### 8.3 Hallazgos y frases destacadas

-

### 8.4 Cambios al proyecto a partir del relevamiento

-
