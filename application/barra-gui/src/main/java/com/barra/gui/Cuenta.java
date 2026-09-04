package com.barra.gui;

import java.util.List;

/**
 * Refleja el CuentaOut del backend Python (app/models.py): la cuenta de
 * una mesa, con todas las rondas de pedido que se le fueron sumando desde
 * que se abrió hasta que se cierra (y se genera el ticket).
 */
public class Cuenta {
    public final int id;
    public final int mesaId;
    public final String mesaNombre;
    public final String fechaApertura;
    public final String fechaCierre; // null mientras sigue abierta
    public final String estado; // "abierta" | "cerrada"
    public final List<Pedido> pedidos;
    public final double total;

    public Cuenta(int id, int mesaId, String mesaNombre, String fechaApertura, String fechaCierre,
                  String estado, List<Pedido> pedidos, double total) {
        this.id = id;
        this.mesaId = mesaId;
        this.mesaNombre = mesaNombre;
        this.fechaApertura = fechaApertura;
        this.fechaCierre = fechaCierre;
        this.estado = estado;
        this.pedidos = pedidos;
        this.total = total;
    }
}
