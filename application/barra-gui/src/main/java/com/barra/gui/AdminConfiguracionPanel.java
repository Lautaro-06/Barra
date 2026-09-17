package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Admin > Configuración: lo que hace que la misma app sirva para cualquier
 * local - hoy solo el nombre, pero es el lugar natural para sumar más
 * adelante moneda, dirección, logo, etc. sin tener que tocar código.
 */
public class AdminConfiguracionPanel extends JPanel {

    private final ApiClient api;
    private final Runnable alCambiar;
    private final JTextField nombreLocalField = new JTextField();
    private final JTextField nombreDuenoField = new JTextField();
    private final JTextField emailDuenoField = new JTextField();
    private final JTextField telefonoField = new JTextField();

    public AdminConfiguracionPanel(ApiClient api, Runnable alCambiar) {
        super();
        this.api = api;
        this.alCambiar = alCambiar;
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        JLabel titulo = new JLabel("Configuración del local");
        titulo.setFont(UiTheme.SUBTITULO);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel etiqueta = new JLabel("Nombre del local");
        etiqueta.setFont(UiTheme.TEXTO_BASE.deriveFont(11f));
        etiqueta.setForeground(UiTheme.MUTED);
        etiqueta.setAlignmentX(Component.LEFT_ALIGNMENT);
        etiqueta.setBorder(BorderFactory.createEmptyBorder(16, 0, 4, 0));

        nombreLocalField.setFont(UiTheme.TEXTO_BASE);
        nombreLocalField.setAlignmentX(Component.LEFT_ALIGNMENT);
        nombreLocalField.setMaximumSize(new Dimension(320, 34));
        nombreLocalField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDE),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        JLabel ayuda = new JLabel("Aparece en el sidebar, en el título de la ventana y en el ticket.");
        ayuda.setFont(UiTheme.TEXTO_BASE.deriveFont(11f));
        ayuda.setForeground(UiTheme.MUTED);
        ayuda.setAlignmentX(Component.LEFT_ALIGNMENT);
        ayuda.setBorder(BorderFactory.createEmptyBorder(6, 0, 16, 0));

        RoundButton guardarBtn = new RoundButton("Guardar", UiTheme.ACENTO, UiTheme.ACENTO_OSCURO);
        guardarBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        guardarBtn.addActionListener(e -> guardar());

        form.add(titulo);
        form.add(etiqueta);
        form.add(nombreLocalField);
        form.add(ayuda);
        form.add(guardarBtn);

        // ---- Datos del dueño (para alertas y resumen diario por email) ----

        JLabel tituloDueno = new JLabel("Datos del dueño");
        tituloDueno.setFont(UiTheme.SUBTITULO);
        tituloDueno.setAlignmentX(Component.LEFT_ALIGNMENT);
        tituloDueno.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));

        JLabel etiquetaNombreDueno = new JLabel("Nombre del dueño");
        etiquetaNombreDueno.setFont(UiTheme.TEXTO_BASE.deriveFont(11f));
        etiquetaNombreDueno.setForeground(UiTheme.MUTED);
        etiquetaNombreDueno.setAlignmentX(Component.LEFT_ALIGNMENT);
        etiquetaNombreDueno.setBorder(BorderFactory.createEmptyBorder(16, 0, 4, 0));

        nombreDuenoField.setFont(UiTheme.TEXTO_BASE);
        nombreDuenoField.setAlignmentX(Component.LEFT_ALIGNMENT);
        nombreDuenoField.setMaximumSize(new Dimension(320, 34));
        nombreDuenoField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDE),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        JLabel etiquetaEmailDueno = new JLabel("Email del dueño");
        etiquetaEmailDueno.setFont(UiTheme.TEXTO_BASE.deriveFont(11f));
        etiquetaEmailDueno.setForeground(UiTheme.MUTED);
        etiquetaEmailDueno.setAlignmentX(Component.LEFT_ALIGNMENT);
        etiquetaEmailDueno.setBorder(BorderFactory.createEmptyBorder(12, 0, 4, 0));

        emailDuenoField.setFont(UiTheme.TEXTO_BASE);
        emailDuenoField.setAlignmentX(Component.LEFT_ALIGNMENT);
        emailDuenoField.setMaximumSize(new Dimension(320, 34));
        emailDuenoField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDE),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        JLabel etiquetaTelefono = new JLabel("Teléfono (opcional)");
        etiquetaTelefono.setFont(UiTheme.TEXTO_BASE.deriveFont(11f));
        etiquetaTelefono.setForeground(UiTheme.MUTED);
        etiquetaTelefono.setAlignmentX(Component.LEFT_ALIGNMENT);
        etiquetaTelefono.setBorder(BorderFactory.createEmptyBorder(12, 0, 4, 0));

        telefonoField.setFont(UiTheme.TEXTO_BASE);
        telefonoField.setAlignmentX(Component.LEFT_ALIGNMENT);
        telefonoField.setMaximumSize(new Dimension(320, 34));
        telefonoField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDE),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        JLabel ayudaDueno = new JLabel("Se usa como contacto para las alertas y el resumen diario por email.");
        ayudaDueno.setFont(UiTheme.TEXTO_BASE.deriveFont(11f));
        ayudaDueno.setForeground(UiTheme.MUTED);
        ayudaDueno.setAlignmentX(Component.LEFT_ALIGNMENT);
        ayudaDueno.setBorder(BorderFactory.createEmptyBorder(6, 0, 16, 0));

        RoundButton guardarDuenoBtn = new RoundButton("Guardar datos del dueño", UiTheme.ACENTO, UiTheme.ACENTO_OSCURO);
        guardarDuenoBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        guardarDuenoBtn.addActionListener(e -> guardarDueno());

        form.add(tituloDueno);
        form.add(etiquetaNombreDueno);
        form.add(nombreDuenoField);
        form.add(etiquetaEmailDueno);
        form.add(emailDuenoField);
        form.add(etiquetaTelefono);
        form.add(telefonoField);
        form.add(ayudaDueno);
        form.add(guardarDuenoBtn);

        add(form, BorderLayout.NORTH);
    }

    public void setConfiguracion(Configuracion config) {
        if (!nombreLocalField.getText().equals(config.nombreLocal)) {
            nombreLocalField.setText(config.nombreLocal);
        }
    }

    public void setAdmin(Admin admin) {
        if (!nombreDuenoField.getText().equals(admin.nombreDueno)) {
            nombreDuenoField.setText(admin.nombreDueno);
        }
        if (!emailDuenoField.getText().equals(admin.emailDueno)) {
            emailDuenoField.setText(admin.emailDueno);
        }
        String telefonoActual = admin.telefono == null ? "" : admin.telefono;
        if (!telefonoField.getText().equals(telefonoActual)) {
            telefonoField.setText(telefonoActual);
        }
    }

    private void guardar() {
        String nombre = nombreLocalField.getText().trim();
        if (nombre.isEmpty()) {
            Toast.error(this, "El nombre del local no puede estar vacío");
            return;
        }
        try {
            api.actualizarConfiguracion(nombre);
            alCambiar.run();
            Toast.exito(this, "Configuración guardada");
        } catch (Exception ex) {
            Toast.error(this, "No se pudo guardar: " + ex.getMessage());
        }
    }

    private void guardarDueno() {
        String nombre = nombreDuenoField.getText().trim();
        String email = emailDuenoField.getText().trim();
        String telefono = telefonoField.getText().trim();
        if (nombre.isEmpty()) {
            Toast.error(this, "El nombre del dueño no puede estar vacío");
            return;
        }
        if (email.isEmpty()) {
            Toast.error(this, "El email del dueño no puede estar vacío");
            return;
        }
        try {
            api.actualizarAdmin(nombre, email, telefono.isEmpty() ? null : telefono);
            Toast.exito(this, "Datos del dueño guardados");
        } catch (Exception ex) {
            Toast.error(this, "No se pudo guardar: " + ex.getMessage());
        }
    }
}