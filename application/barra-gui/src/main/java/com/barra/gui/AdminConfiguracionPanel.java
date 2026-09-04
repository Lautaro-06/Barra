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

        add(form, BorderLayout.NORTH);
    }

    public void setConfiguracion(Configuracion config) {
        if (!nombreLocalField.getText().equals(config.nombreLocal)) {
            nombreLocalField.setText(config.nombreLocal);
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
}
