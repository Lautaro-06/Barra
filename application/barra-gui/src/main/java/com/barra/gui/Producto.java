package com.barra.gui;

/** Refleja el ProductoOut del backend Python (app/models.py). */
public class Producto {
    public final int id;
    public final String nombre;
    public final double precio;
    public final int stock;
    public final boolean disponible;
    /** Umbral propio del producto. null = usa el umbral global de configuracion. */
    public final Integer umbralStock;

    public Producto(int id, String nombre, double precio, int stock, boolean disponible, Integer umbralStock) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
        this.disponible = disponible;
        this.umbralStock = umbralStock;
    }

    @Override
    public String toString() {
        return nombre + " - $" + precio + " (stock: " + stock + ")";
    }
}