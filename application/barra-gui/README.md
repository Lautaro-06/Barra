# Barra — GUI de escritorio (Java)

Cliente **Swing** (Java 17) que le habla al backend Python por HTTP local
(`http://127.0.0.1:8000`). No toca la base de datos directamente: todo pasa por `ApiClient.java`.

## Pantallas

La ventana tiene una barra lateral con cuatro pantallas:

- **Vender:** mostrador táctil para pedidos sin mesa (para llevar). Se toca un producto para
  sumarlo al pedido, se ajustan cantidades en el carrito, se agrega una nota opcional y se confirma.
- **Mesas:** el salón. Cada tarjeta es una mesa: verde y «Libre» si no tiene a nadie, naranja con
  el total acumulado si tiene la cuenta abierta. Tocarla abre la cuenta de esa mesa
  (`CuentaMesaDialog`): se suman rondas de pedido mientras el comensal sigue en el local y desde
  ahí se cierra la cuenta y se genera el ticket (`TicketDialog`, imprimible con el diálogo de
  impresión del sistema).
- **Cocina:** tablero con los pedidos agrupados por estado (en preparación / listo / entregados,
  los últimos 10), con un botón por tarjeta para avanzar el estado. Si el pedido es de una mesa,
  la tarjeta muestra «Mesa X» en vez del número de pedido.
- **Admin:** lo que hace que la app sirva para cualquier local sin tocar código, en tres pestañas:
  - **Productos:** alta, edición de precio y stock, umbral de stock bajo propio y disponibilidad.
  - **Mesas:** agregar y eliminar mesas del salón.
  - **Configuración:** nombre del local (aparece en la barra lateral, el título y el ticket),
    datos del dueño, umbral global de stock, alertas y resumen diario por email, servidor SMTP,
    y los botones «Enviar email de prueba» y «Enviar resumen ahora».

Todo se sincroniza solo: `MainWindow` consulta al backend cada 4 segundos y actualiza las cuatro
pantallas. El indicador de abajo de la barra lateral dice «Backend conectado» o «Backend caído»;
si el backend se cae, la GUI se reconecta sola cuando vuelve.

Guía de uso con capturas: [manual de usuario](../../docs/manual-de-usuario.md).

## Requisitos

- **JDK 17** o superior.
- **Maven** 3.8 o superior (o un IDE con soporte Maven).
- El backend corriendo en `http://127.0.0.1:8000` (ver el [README del backend](../barra-backend/README.md)).
  La URL está fija en `ApiClient.java`, así que la GUI y el backend tienen que estar en la misma PC.

## Cómo correrlo

**Terminal:**

```bash
mvn package
java -jar target/barra-gui-1.0.0.jar
```

**Eclipse:** File → Import → Maven → Existing Maven Projects → elegir esta carpeta
(`barra-gui`) → Run As → Java Application sobre `Main.java`.

**IntelliJ IDEA:** Open → elegir `pom.xml` → Run sobre `Main`.

Para publicar el `.jar` en la web: [compilación y empaquetado](../../docs/tecnico/compilacion-y-empaquetado.md#3-publicar-una-versión).

## Sin dependencias externas

HTTP con `java.net.http.HttpClient` (nativo del JDK) y un parser JSON propio (`Json.java`), para
no depender de Maven Central. Si más adelante conviene sumar una librería (por ejemplo Gson), se
agrega en el `pom.xml` sin tocar el resto.

## Identidad visual

Nada de emojis ni imágenes externas: los íconos de la barra lateral (y el logo y el ícono de la
ventana) son siluetas vectoriales propias dibujadas con Java2D (`AppIcons.java`), así se ven igual
en cualquier sistema operativo y resolución. Los avisos de éxito y error usan un «toast» propio
(`Toast.java`) en vez de `JOptionPane`, y los formularios usan diálogos con la estética de la app.

## Archivos

| Archivo | Qué es |
|---|---|
| `Main.java` | Punto de entrada |
| `MainWindow.java` | Ventana principal: barra lateral, indicador de conexión y sondeo cada 4 s |
| `VentaPanel.java` | Pantalla Vender (mostrador + carrito, pedidos sin mesa) |
| `MesasPanel.java` | Pantalla Mesas (grilla del salón) |
| `CuentaMesaDialog.java` | La cuenta de una mesa: rondas de pedido y cierre |
| `TicketDialog.java` | El ticket de una cuenta cerrada, con impresión |
| `CocinaPanel.java` | Pantalla Cocina (tablero de pedidos por estado) |
| `AdminPanel.java` | Pantalla Admin (pestañas Productos, Mesas y Configuración) |
| `AdminProductosPanel.java` | Tabla de productos: alta, edición, umbral y disponibilidad |
| `AdminMesasPanel.java` | Alta y baja de mesas |
| `AdminConfiguracionPanel.java` | Nombre del local, datos del dueño, umbral global, email y resumen diario |
| `ProductoFormDialog.java` | Formulario compartido para crear y editar un producto |
| `ProductoCard.java` | Tarjeta de producto de Vender y de la cuenta de una mesa |
| `CarritoItem.java` | Línea del carrito (Vender y cuenta de una mesa) |
| `ApiClient.java` | Toda la comunicación HTTP con el backend |
| `Json.java` | Parser y writer JSON sin dependencias |
| `Producto.java`, `Pedido.java`, `Mesa.java`, `Cuenta.java`, `Configuracion.java`, `Admin.java` | Modelos que reflejan el JSON del backend |
| `UiTheme.java` | Colores, tipografías y formato de moneda |
| `AppIcons.java` | Íconos vectoriales propios |
| `NavButton.java`, `TabButton.java` | Botones de navegación con estado activo |
| `Toast.java` | Notificación flotante no bloqueante |
| `RoundedPanel.java`, `RoundButton.java` | Componentes con estética propia, independientes del look and feel del sistema |

## Limitaciones conocidas

- Los avisos de error muestran el texto del backend tal cual (por ejemplo `Error del backend (400): {"detail": …}`).
- En Vender, el stock se pinta de naranja con un umbral fijo de 5, sin usar el umbral configurado.
- No se pueden modificar ni cancelar pedidos, ni ver el historial completo.
