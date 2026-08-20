package com.barra.gui;

/** Refleja el PedidoOut del backend Python (app/models.py). */
public class Pedido {
    public final int id;
    public final String fecha;
    public final String estado;
    public final double total;
    public final String nota;

    public Pedido(int id, String fecha, String estado, double total, String nota) {
        this.id = id;
        this.fecha = fecha;
        this.estado = estado;
        this.total = total;
        this.nota = nota;
    }

    @Override
    public String toString() {
        return "Pedido #" + id + " [" + estado + "] - $" + total
                + (nota != null && !nota.isBlank() ? " (" + nota + ")" : "");
    }
}
