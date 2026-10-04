package com.barra.gui;

/** Refleja el MesaOut del backend Python (app/models.py). */
public class Mesa {
    public final int id;
    public final String nombre;
    public final String estado; // "libre" | "ocupada"
    public final Integer cuentaId; // null si está libre
    public final double totalActual;

    public Mesa(int id, String nombre, String estado, Integer cuentaId, double totalActual) {
        this.id = id;
        this.nombre = nombre;
        this.estado = estado;
        this.cuentaId = cuentaId;
        this.totalActual = totalActual;
    }

    public boolean ocupada() {
        return "ocupada".equals(estado);
    }
}
