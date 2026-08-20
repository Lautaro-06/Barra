package com.barra.gui;

/** Refleja el ProductoOut del backend Python (app/models.py). */
public class Producto {
    public final int id;
    public final String nombre;
    public final double precio;
    public final int stock;

    public Producto(int id, String nombre, double precio, int stock) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
    }

    @Override
    public String toString() {
        return nombre + " - $" + precio + " (stock: " + stock + ")";
    }
}
