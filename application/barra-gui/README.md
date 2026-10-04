# Barra - GUI Java

Cliente Swing que le habla al backend Python por HTTP local (localhost:8000).
No toca la base de datos directamente: todo pasa por `ApiClient.java`.

La ventana tiene una barra lateral con cuatro pantallas:

- **Vender**: mostrador táctil para pedidos sin mesa (para llevar). Se toca
  un producto para sumarlo al pedido, se ajustan cantidades en el carrito y
  se confirma; el pedido se manda tal cual quedó armado.
- **Mesas**: el salón. Cada tarjeta es una mesa - verde y "Libre" si no
  tiene a nadie, naranja con el total acumulado si tiene la cuenta abierta.
  Tocarla abre la cuenta de esa mesa (`CuentaMesaDialog`): se le pueden ir
  sumando rondas de pedido mientras el comensal sigue en el local, y desde
  ahí mismo se cierra la cuenta y se genera el ticket (`TicketDialog`,
  imprimible con el diálogo de impresión del sistema).
- **Cocina**: tablero tipo kanban con los pedidos en curso agrupados por
  estado (en preparación / listo / entregado), con un botón por tarjeta
  para avanzar el estado. Si el pedido es de una mesa, la tarjeta muestra
  "Mesa X" en vez del número de pedido.
- **Admin**: lo que hace que la app sirva para cualquier local sin tocar
  código - productos (alta, edición de precio/stock y toggle de
  disponibilidad), mesas del salón (agregar/eliminar) y el nombre del local
  (aparece en el sidebar, el título de la ventana y el ticket).

Todo se sincroniza solo: `MainWindow` hace polling al backend cada 4
segundos y empuja los datos a las cuatro pantallas.

## Cómo importarlo en Eclipse

1. Eclipse -> File -> Import -> Maven -> Existing Maven Projects
2. Elegir esta carpeta (`barra-gui`)
3. Eclipse va a reconocer el `pom.xml` y armar el proyecto solo.
4. Correr `Main.java` (Run As -> Java Application)

Sin dependencias externas: HTTP con `java.net.http.HttpClient` (nativo del
JDK) y un parser JSON casero (`Json.java`), para no depender de bajar nada
de Maven Central en esta primera etapa. Si más adelante conviene sumar una
librería (ej. Gson), se agrega en el `pom.xml` sin tocar el resto.

## Importante: primero levantar el backend

La GUI espera que `barra-backend` ya esté corriendo en
http://127.0.0.1:8000 (ver README de esa carpeta). Si no lo está, el
indicador de abajo del sidebar avisa "Backend caído".

## Identidad visual

Nada de emojis ni imágenes externas: los íconos del sidebar (y el logo/
ícono de la ventana) son siluetas vectoriales propias dibujadas con Java2D
(`AppIcons.java`), así se ven igual de nítidas en cualquier sistema
operativo y a cualquier resolución. Los avisos de éxito/error usan un
"toast" propio (`Toast.java`) en vez de `JOptionPane`, y los formularios
(nuevo/editar producto, ticket) usan diálogos con la estética de la app en
lugar del cartel genérico de Swing.

## Archivos

- `Main.java`        - punto de entrada
- `MainWindow.java`  - ventana principal: sidebar + polling que sincroniza las 4 pantallas
- `VentaPanel.java`  - pantalla "Vender" (mostrador + carrito, pedidos sin mesa)
- `MesasPanel.java`  - pantalla "Mesas" (grilla del salón)
- `CuentaMesaDialog.java` - la cuenta de una mesa: rondas de pedido + cierre
- `TicketDialog.java` - el ticket de una cuenta cerrada, con impresión real
- `CocinaPanel.java` - pantalla "Cocina" (tablero kanban de pedidos)
- `AdminPanel.java`  - pantalla "Admin" (pestañas Productos/Mesas/Configuración)
- `AdminProductosPanel.java` - alta/edición de productos y disponibilidad
- `AdminMesasPanel.java` - alta/baja de mesas del salón
- `AdminConfiguracionPanel.java` - nombre del local
- `ProductoFormDialog.java` - formulario compartido para crear/editar un producto
- `ProductoCard.java` - tarjeta de producto reutilizada por Vender y por la cuenta de una mesa
- `UiTheme.java`     - colores, tipografías y formato de moneda compartidos
- `AppIcons.java`    - íconos vectoriales propios (sidebar, logo, ícono de ventana)
- `NavButton.java`, `TabButton.java` - botones de navegación con estado activo/inactivo
- `Toast.java`       - notificación flotante no bloqueante (reemplaza JOptionPane)
- `RoundedPanel.java`, `RoundButton.java` - componentes con estética propia (independiente del Look&Feel del SO)
- `CarritoItem.java` - línea del carrito (Vender y la cuenta de una mesa)
- `ApiClient.java`   - toda la comunicación HTTP con Python
- `Json.java`        - parser/writer JSON sin dependencias
- `Producto.java`, `Pedido.java`, `Mesa.java`, `Cuenta.java`, `Configuracion.java` - modelos que reflejan el JSON del backend
