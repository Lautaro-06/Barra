package com.barra.gui;

/** Refleja el AdminOut del backend Python (app/models.py): datos del
 * dueño del local (nombre, email de contacto, teléfono). */
public class Admin {
    public final String nombreDueno;
    public final String emailDueno;
    public final String telefono;

    public Admin(String nombreDueno, String emailDueno, String telefono) {
        this.nombreDueno = nombreDueno;
        this.emailDueno = emailDueno;
        this.telefono = telefono;
    }
}