package com.barra.gui;

/**
 * Una línea del carrito de la pantalla "Vender": un producto y la cantidad
 * que se va a mandar a cocina.
 */
public class CarritoItem {

    public final Producto producto;
    public int cantidad;

    public CarritoItem(Producto producto, int cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
    }

    public double subtotal() {
        return producto.precio * cantidad;
    }
}
