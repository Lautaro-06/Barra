# Presentación del producto — Barra

Formato: **demo en vivo + 8 diapositivas de apoyo**, unos 10 minutos. Si algo falla en la demo,
se pasa el video de respaldo.

| Material | Dónde |
|---|---|
| Diapositivas (con notas del orador; se descargan como PowerPoint o PDF) | <https://claude.ai/artifact/EkahGqhX72yFt5kYGBiied> |
| Video de respaldo (1 min 31 s, MP4) | [`docs/presentacion/demo-barra.mp4`](../presentacion/demo-barra.mp4) |
| Manual de usuario (para preguntas sobre el uso) | [`docs/manual-de-usuario.md`](../manual-de-usuario.md) |

> Las diapositivas son privadas hasta que se compartan desde el menú **Compartir** de la página:
> para que el resto del grupo o el docente las vean, hay que compartirlas.

---

## 1. Guion

| Min. | Diapositiva | Qué decir / hacer |
|---|---|---|
| 0:00 | **1. Portada** | Presentarse. Barra es un sistema interno para el local; nada de cara al comensal. Funciona en la PC del local, sin depender de internet |
| 0:30 | **2. El problema** | 30 segundos: pedidos que se pisan, sin registro de ventas, sin control de stock |
| 1:00 | **3. La solución** | Las cuatro pantallas. El dueño recibe alertas de stock y un resumen diario por email |
| 1:45 | **4. Demo en vivo** | Cambiar a la app y seguir los 4 pasos (ver §2). Si falla: video de respaldo |
| 5:30 | **5. Cómo está hecho** | Java muestra, Python decide. Python único dueño de SQLite. Dos usos de concurrencia: pool de pedidos + hilos de fondo. La web de venta es otro sistema |
| 6:45 | **6. Probado** | 64 de 65 pruebas automáticas. Concurrencia: 30 pedidos sobre 10 unidades → 10 aceptados. Respuesta máxima 7,5 ms. El único defecto está documentado |
| 7:45 | **7. Cómo se consigue** | Web → mail con licencia → instalar. Gratis 10 días, Pro $12.000/mes; Fudo arranca en $22.500. Si hay tiempo, mostrar la web |
| 8:45 | **8. Lo que sigue** | Licencia dentro de la app, instalador único, cancelar pedidos e historial. Preguntas |

---

## 2. Demo en vivo (paso a paso)

**Antes de empezar** (5 minutos antes):

- [ ] Abrir `barra-backend-v1.0.0.exe` y después `barra-gui-v1.0.0.jar`. Abajo a la izquierda: **● Backend conectado**.
- [ ] Cargar datos de ejemplo: el nombre del local, productos con stock y al menos una mesa libre.
- [ ] Dejar un producto **justo por encima del umbral** (por ejemplo, Milanesa con stock 6 y umbral 5) para que la alerta salte en vivo.
- [ ] Configurar el email (Admin → Configuración), tildar «Enviar alertas de stock bajo» y probar con «Enviar email de prueba».
- [ ] Tener abierto el mail del dueño en un celular o en otra pestaña.
- [ ] Tener a mano el video de respaldo.

**Durante la demo:**

1. **Vender:** tocar 2 hamburguesas, papas y **2 milanesas**; escribir la nota «Para llevar – Ana»; **Confirmar pedido**. Mostrar el aviso «Pedido #N enviado a cocina».
2. **Cocina:** el pedido aparece en «En preparación». **Marcar listo** y después **Entregar**.
3. **Mesas:** tocar una mesa libre, cargar una ronda (pizza y 2 gaseosas), **Agregar a la cuenta**; cargar otra ronda (2 cafés); **Cerrar cuenta y generar ticket**. Mostrar el ticket y el botón **Imprimir**.
4. **Alerta de stock:** a los pocos segundos de vender las milanesas, llega el mail «[Local] Stock bajo: Milanesa napolitana». Mostrarlo. En **Admin → Productos**, el stock de la milanesa aparece en naranja.

**Si algo falla:** no improvisar arreglos; pasar el video de respaldo, que muestra exactamente estos pasos.

---

## 3. Video de respaldo

[`docs/presentacion/demo-barra.mp4`](../presentacion/demo-barra.mp4) — 1280×720, 1 min 31 s, sin audio.

| Tiempo | Escena |
|---|---|
| 0:00 | Portada |
| 0:04 | 1 · Vender un pedido en el mostrador (con nota) y confirmarlo |
| 0:20 | 2 · La cocina lo marca listo y entregado |
| 0:32 | 3 · Mesa 3: dos rondas, cierre y ticket |
| 1:00 | 4 · El email de alerta de stock que recibió el dueño (real) y el stock en naranja en Admin |
| 1:13 | 5 · Obtener el plan Gratis desde la web |
| 1:27 | Cierre |

Cómo se grabó: la app real conectada a su backend, manejada con clics automáticos y grabada
desde la pantalla. El email de alerta es el que mandó el hilo de vigilancia de stock a un servidor
de email de prueba, segundos después de vender las milanesas. La web se grabó con el backend y
MySQL reales.

---

## 4. Preguntas probables

| Pregunta | Respuesta corta |
|---|---|
| ¿Por qué Java y Python juntos? | Separación en capas (la GUI solo muestra; la lógica vive en Python) y la consigna de integrar lenguajes. Se hablan por HTTP local |
| ¿Por qué Swing y no JavaFX o Electron? | Viene con el JDK, el `.jar` pesa 100 KB y no tiene dependencias ([ADR-001](../tecnico/decisiones.md)) |
| ¿Qué pasa si dos cajeros venden el último producto a la vez? | El descuento de stock está protegido por un lock: se acepta uno y el otro recibe «Stock insuficiente» (prueba CP-32) |
| ¿Y si se corta internet? | Se sigue vendiendo; solo los emails esperan y se reintentan cada 5 minutos |
| ¿Dónde están los datos? | En `barra.db`, en la PC del local, con copias automáticas cada 4 horas |
| ¿Cómo se protege la licencia? | Código + clave secreta; la clave se guarda con bcrypt y nunca se reenvía |
| ¿Por qué hay que instalar Java? | Es una limitación de esta versión; el próximo paso es un instalador con `jpackage` que lo incluya |
| ¿Qué no hace? | Facturación electrónica, pedidos online, varias PC en red, avisos al comensal |
