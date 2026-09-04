package com.barra.gui;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ApiClient
 *
 * Punto clave de la arquitectura: acá es donde la GUI Java "le pasa el
 * pedido a la cocina por la ventanita" -> hace HTTP contra el backend
 * Python que corre en localhost. Java NUNCA toca la base de datos
 * directamente; todo pasa por acá.
 */
public class ApiClient {

    private static final String BASE_URL = "http://127.0.0.1:8000";

    // uvicorn (el backend) solo habla HTTP/1.1. El HttpClient por defecto
    // intenta negociar HTTP/2 en cada pedido igual, y cuando el servidor lo
    // rechaza a veces se pierde el body de los POST (uvicorn responde 422
    // "Field required" como si no hubiera llegado nada) - forzar 1.1 evita
    // esa negociación y el problema por completo.
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    /** Chequea que el backend Python esté levantado antes de mostrar la GUI. */
    public boolean healthCheck() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/health"))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            return resp.statusCode() == 200;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    // ---------- Configuración del local ----------

    public Configuracion obtenerConfiguracion() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/configuracion"))
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        checkOk(resp);
        Map<String, Object> o = Json.parseObject(resp.body());
        return new Configuracion((String) o.get("nombre_local"));
    }

    public Configuracion actualizarConfiguracion(String nombreLocal) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nombre_local", nombreLocal);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/configuracion"))
                .header("Content-Type", "application/json")
                .method("PUT", HttpRequest.BodyPublishers.ofString(Json.writeObject(body), java.nio.charset.StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        checkOk(resp);
        Map<String, Object> o = Json.parseObject(resp.body());
        return new Configuracion((String) o.get("nombre_local"));
    }

    // ---------- Productos (catálogo / admin) ----------

    public List<Producto> listarProductos() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/productos"))
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        checkOk(resp);

        List<Object> arr = Json.parseArray(resp.body());
        List<Producto> productos = new ArrayList<>();
        for (Object item : arr) {
            productos.add(productoFromMap(item));
        }
        return productos;
    }

    // ---------- Pedidos (para el tablero de Cocina) ----------

    public List<Pedido> listarPedidos() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/pedidos"))
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        checkOk(resp);

        List<Object> arr = Json.parseArray(resp.body());
        List<Pedido> pedidos = new ArrayList<>();
        for (Object item : arr) {
            pedidos.add(pedidoFromMap(item));
        }
        return pedidos;
    }

    /** Da de alta un producto nuevo (panel de Admin). */
    public Producto crearProducto(String nombre, double precio, int stock, boolean disponible) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nombre", nombre);
        body.put("precio", precio);
        body.put("stock", stock);
        body.put("disponible", disponible);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/productos"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Json.writeObject(body), java.nio.charset.StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        checkOk(resp);
        return productoFromMap(Json.parseObject(resp.body()));
    }

    /** Edita nombre/precio/stock/disponibilidad de un producto existente (panel de Admin). */
    public Producto editarProducto(int id, String nombre, double precio, int stock, boolean disponible) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nombre", nombre);
        body.put("precio", precio);
        body.put("stock", stock);
        body.put("disponible", disponible);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/productos/" + id))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(Json.writeObject(body), java.nio.charset.StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        checkOk(resp);
        return productoFromMap(Json.parseObject(resp.body()));
    }

    // ---------- Mesas y cuentas (salón) ----------

    public List<Mesa> listarMesas() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/mesas"))
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        checkOk(resp);

        List<Object> arr = Json.parseArray(resp.body());
        List<Mesa> mesas = new ArrayList<>();
        for (Object item : arr) {
            mesas.add(mesaFromMap(item));
        }
        return mesas;
    }

    public Mesa crearMesa(String nombre) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nombre", nombre);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/mesas"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Json.writeObject(body), java.nio.charset.StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        checkOk(resp);
        return mesaFromMap(Json.parseObject(resp.body()));
    }

    public void eliminarMesa(int id) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/mesas/" + id))
                .DELETE()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        checkOk(resp);
    }

    /** Abre la cuenta de una mesa libre (si ya tenía una abierta, la devuelve tal cual). */
    public Mesa abrirMesa(int id) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/mesas/" + id + "/abrir"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        checkOk(resp);
        return mesaFromMap(Json.parseObject(resp.body()));
    }

    /** Trae la cuenta abierta de la mesa con todas las rondas de pedido acumuladas. */
    public Cuenta obtenerCuentaMesa(int mesaId) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/mesas/" + mesaId + "/cuenta"))
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        checkOk(resp);
        return cuentaFromMap(Json.parseObject(resp.body()));
    }

    /** Suma una ronda de pedido a la cuenta abierta de la mesa. */
    public Pedido crearPedidoMesa(int mesaId, String nota, List<int[]> detalles) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/mesas/" + mesaId + "/pedidos"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Json.writeObject(pedidoBody(nota, detalles)), java.nio.charset.StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        checkOk(resp);
        return pedidoFromMap(Json.parseObject(resp.body()));
    }

    /** Cierra la cuenta de la mesa (que vuelve a quedar libre) y devuelve la cuenta completa para el ticket. */
    public Cuenta cerrarMesa(int mesaId) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/mesas/" + mesaId + "/cerrar"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        checkOk(resp);
        return cuentaFromMap(Json.parseObject(resp.body()));
    }

    // ---------- Pedidos de mostrador (sin mesa) ----------

    /**
     * Crea un pedido de mostrador/para llevar. detalles es una lista de
     * pares [productoId, cantidad].
     */
    public Pedido crearPedido(String nota, List<int[]> detalles) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/pedidos"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Json.writeObject(pedidoBody(nota, detalles)), java.nio.charset.StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        checkOk(resp);
        return pedidoFromMap(Json.parseObject(resp.body()));
    }

    public void cambiarEstado(int pedidoId, String nuevoEstado) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("estado", nuevoEstado);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/pedidos/" + pedidoId + "/estado"))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(Json.writeObject(body), java.nio.charset.StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        checkOk(resp);
    }

    // ---------- Helpers de armado/parseo de JSON ----------

    private Map<String, Object> pedidoBody(String nota, List<int[]> detalles) {
        List<Object> detallesJson = new ArrayList<>();
        for (int[] d : detalles) {
            Map<String, Object> det = new LinkedHashMap<>();
            det.put("producto_id", d[0]);
            det.put("cantidad", d[1]);
            detallesJson.add(det);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nota", nota);
        body.put("detalles", detallesJson);
        return body;
    }

    @SuppressWarnings("unchecked")
    private Producto productoFromMap(Object raw) {
        Map<String, Object> o = (Map<String, Object>) raw;
        return new Producto(
                ((Number) o.get("id")).intValue(),
                (String) o.get("nombre"),
                ((Number) o.get("precio")).doubleValue(),
                ((Number) o.get("stock")).intValue(),
                Boolean.TRUE.equals(o.get("disponible")));
    }

    @SuppressWarnings("unchecked")
    private Mesa mesaFromMap(Object raw) {
        Map<String, Object> o = (Map<String, Object>) raw;
        Object cuentaIdRaw = o.get("cuenta_id");
        Integer cuentaId = cuentaIdRaw == null ? null : ((Number) cuentaIdRaw).intValue();
        return new Mesa(
                ((Number) o.get("id")).intValue(),
                (String) o.get("nombre"),
                (String) o.get("estado"),
                cuentaId,
                ((Number) o.get("total_actual")).doubleValue());
    }

    @SuppressWarnings("unchecked")
    private Cuenta cuentaFromMap(Object raw) {
        Map<String, Object> o = (Map<String, Object>) raw;
        List<Pedido> pedidos = new ArrayList<>();
        List<Object> pedidosRaw = (List<Object>) o.get("pedidos");
        if (pedidosRaw != null) {
            for (Object p : pedidosRaw) {
                pedidos.add(pedidoFromMap(p));
            }
        }
        return new Cuenta(
                ((Number) o.get("id")).intValue(),
                ((Number) o.get("mesa_id")).intValue(),
                (String) o.get("mesa_nombre"),
                (String) o.get("fecha_apertura"),
                (String) o.get("fecha_cierre"),
                (String) o.get("estado"),
                pedidos,
                ((Number) o.get("total")).doubleValue());
    }

    @SuppressWarnings("unchecked")
    private Pedido pedidoFromMap(Object raw) {
        Map<String, Object> o = (Map<String, Object>) raw;

        List<Pedido.Detalle> detalles = new ArrayList<>();
        List<Object> detallesRaw = (List<Object>) o.get("detalles");
        if (detallesRaw != null) {
            for (Object d : detallesRaw) {
                Map<String, Object> dm = (Map<String, Object>) d;
                detalles.add(new Pedido.Detalle(
                        ((Number) dm.get("producto_id")).intValue(),
                        (String) dm.get("nombre_producto"),
                        ((Number) dm.get("cantidad")).intValue(),
                        ((Number) dm.get("subtotal")).doubleValue()));
            }
        }

        return new Pedido(
                ((Number) o.get("id")).intValue(),
                (String) o.get("fecha"),
                (String) o.get("estado"),
                ((Number) o.get("total")).doubleValue(),
                (String) o.get("nota"),
                (String) o.get("mesa_nombre"),
                detalles);
    }

    private void checkOk(HttpResponse<String> resp) throws IOException {
        if (resp.statusCode() >= 400) {
            throw new IOException("Error del backend (" + resp.statusCode() + "): " + resp.body());
        }
    }
}
