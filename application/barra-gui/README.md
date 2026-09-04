# Barra - GUI Java

Cliente Swing que le habla al backend Python por HTTP local (localhost:8000).
No toca la base de datos directamente: todo pasa por `ApiClient.java`.

La ventana tiene una barra lateral con tres pantallas:

- **Vender**: mostrador táctil. Se toca un producto para sumarlo al pedido,
  se ajustan cantidades en el carrito y se confirma; el pedido se manda tal
  cual quedó armado (ya no hay un botón fijo de "2 hamburguesas de prueba").
- **Cocina**: tablero tipo kanban con los pedidos en curso agrupados por
  estado (en preparación / listo / entregado), con un botón por tarjeta
  para avanzar el estado.
- **Catálogo**: lista de productos con el stock resaltado (rojo si no queda,
  naranja si es bajo) y un formulario para dar de alta productos nuevos.

Todo se sincroniza solo: `MainWindow` hace polling al backend cada 4
segundos y empuja los datos a las tres pantallas.

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
"toast" propio (`Toast.java`) en vez de `JOptionPane`, y el alta de
productos usa un diálogo con la estética de la app en lugar del cartel
genérico de Swing.

## Archivos

- `Main.java`        - punto de entrada
- `MainWindow.java`  - ventana principal: sidebar + polling que sincroniza las 3 pantallas
- `VentaPanel.java`  - pantalla "Vender" (mostrador + carrito)
- `CocinaPanel.java` - pantalla "Cocina" (tablero kanban de pedidos)
- `CatalogoPanel.java` - pantalla "Catálogo" (stock + alta de productos)
- `UiTheme.java`     - colores, tipografías y formato de moneda compartidos
- `AppIcons.java`    - íconos vectoriales propios (sidebar, logo, ícono de ventana)
- `NavButton.java`   - botón del sidebar con estado activo/inactivo
- `Toast.java`       - notificación flotante no bloqueante (reemplaza JOptionPane)
- `RoundedPanel.java`, `RoundButton.java` - componentes con estética propia (independiente del Look&Feel del SO)
- `CarritoItem.java` - línea del carrito de la pantalla "Vender"
- `ApiClient.java`   - toda la comunicación HTTP con Python
- `Json.java`        - parser/writer JSON sin dependencias
- `Producto.java`, `Pedido.java` - modelos que reflejan el JSON del backend
