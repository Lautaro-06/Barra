package com.barra.gui;

import java.util.List;

/** Refleja el PedidoOut del backend Python (app/models.py). */
public class Pedido {
    public final int id;
    public final String fecha;
    public final String estado;
    public final double total;
    public final String nota;
    public final String mesaNombre;
    public final List<Detalle> detalles;

    public Pedido(int id, String fecha, String estado, double total, String nota, String mesaNombre, List<Detalle> detalles) {
        this.id = id;
        this.fecha = fecha;
        this.estado = estado;
        this.total = total;
        this.nota = nota;
        this.mesaNombre = mesaNombre;
        this.detalles = detalles;
    }

    @Override
    public String toString() {
        return "Pedido #" + id + " [" + estado + "] - $" + total
                + (nota != null && !nota.isBlank() ? " (" + nota + ")" : "");
    }

    /** Un renglón del detalle de un pedido (ver DetalleOut del backend). */
    public static class Detalle {
        public final int productoId;
        public final String nombreProducto;
        public final int cantidad;
        public final double subtotal;

        public Detalle(int productoId, String nombreProducto, int cantidad, double subtotal) {
            this.productoId = productoId;
            this.nombreProducto = nombreProducto;
            this.cantidad = cantidad;
            this.subtotal = subtotal;
        }
    }
}
