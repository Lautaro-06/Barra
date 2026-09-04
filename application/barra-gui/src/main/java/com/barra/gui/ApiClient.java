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
    private final HttpClient http = HttpClient.newHttpClient();

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
            @SuppressWarnings("unchecked")
            Map<String, Object> o = (Map<String, Object>) item;
            productos.add(new Producto(
                    ((Number) o.get("id")).intValue(),
                    (String) o.get("nombre"),
                    ((Number) o.get("precio")).doubleValue(),
                    ((Number) o.get("stock")).intValue()));
        }
        return productos;
    }

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

    /**
     * Crea un pedido. detalles es una lista de pares [productoId, cantidad].
     */
    public Pedido crearPedido(String nota, List<int[]> detalles) throws IOException, InterruptedException {
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

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/pedidos"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Json.writeObject(body), java.nio.charset.StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        checkOk(resp);

        return pedidoFromMap(Json.parseObject(resp.body()));
    }

    /** Da de alta un producto nuevo en el catálogo (pantalla Catálogo). */
    public Producto crearProducto(String nombre, double precio, int stock) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nombre", nombre);
        body.put("precio", precio);
        body.put("stock", stock);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/productos"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Json.writeObject(body), java.nio.charset.StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        checkOk(resp);

        Map<String, Object> o = Json.parseObject(resp.body());
        return new Producto(
                ((Number) o.get("id")).intValue(),
                (String) o.get("nombre"),
                ((Number) o.get("precio")).doubleValue(),
                ((Number) o.get("stock")).intValue());
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
                detalles);
    }

    private void checkOk(HttpResponse<String> resp) throws IOException {
        if (resp.statusCode() >= 400) {
            throw new IOException("Error del backend (" + resp.statusCode() + "): " + resp.body());
        }
    }
}
