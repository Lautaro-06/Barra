# Barra - GUI Java

Cliente Swing que le habla al backend Python por HTTP local (localhost:8000).
No toca la base de datos directamente: todo pasa por `ApiClient.java`.

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
http://127.0.0.1:8000 (ver README de esa carpeta). Si no lo está, la GUI
te avisa arriba de todo: "Backend Python: SIN CONEXIÓN".

## Archivos

- `Main.java`        - punto de entrada
- `MainWindow.java`  - ventana Swing de prueba (catálogo + pedidos)
- `ApiClient.java`   - toda la comunicación HTTP con Python
- `Json.java`        - parser/writer JSON sin dependencias
- `Producto.java`, `Pedido.java` - modelos que reflejan el JSON del backend
