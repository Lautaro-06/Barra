package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.util.List;

/**
 * Admin > Mesas: cuántas mesas tiene el salón. Cada local es distinto (una
 * barra puede no necesitar mesas, un restaurante puede tener veinte), así
 * que se cargan a mano en vez de venir fijas.
 */
public class AdminMesasPanel extends JPanel {

    private final ApiClient api;
    private final Runnable alCambiar;

    private final JPanel lista = new JPanel();
    private final JTextField nombreField = new JTextField();

    public AdminMesasPanel(ApiClient api, Runnable alCambiar) {
        super(new BorderLayout(0, 12));
        this.api = api;
        this.alCambiar = alCambiar;
        setOpaque(false);

        JLabel titulo = new JLabel("Mesas del salón");
        titulo.setFont(UiTheme.SUBTITULO);

        nombreField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDE),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        nombreField.setPreferredSize(new Dimension(160, 32));

        RoundButton agregarBtn = new RoundButton("+ Agregar mesa", UiTheme.ACENTO, UiTheme.ACENTO_OSCURO);
        agregarBtn.addActionListener(e -> agregarMesa());

        JPanel formulario = new JPanel();
        formulario.setOpaque(false);
        formulario.setLayout(new BoxLayout(formulario, BoxLayout.X_AXIS));
        formulario.add(nombreField);
        formulario.add(Box.createHorizontalStrut(8));
        formulario.add(agregarBtn);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titulo, BorderLayout.WEST);
        header.add(formulario, BorderLayout.EAST);

        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setOpaque(false);
        JScrollPane scroll = new JScrollPane(lista);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDE));
        scroll.getViewport().setBackground(UiTheme.TARJETA);

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    public void setMesas(List<Mesa> mesas) {
        lista.removeAll();
        for (Mesa mesa : mesas) {
            lista.add(crearFila(mesa));
        }
        lista.revalidate();
        lista.repaint();
    }

    private Component crearFila(Mesa mesa) {
        JPanel fila = new JPanel(new BorderLayout());
        fila.setOpaque(true);
        fila.setBackground(UiTheme.TARJETA);
        fila.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDE),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel nombre = new JLabel(mesa.nombre);
        nombre.setFont(UiTheme.TEXTO_NEGRITA);

        JLabel estado = new JLabel(mesa.ocupada() ? "Ocupada" : "Libre");
        estado.setFont(UiTheme.TEXTO_BASE);
        estado.setForeground(mesa.ocupada() ? UiTheme.ACENTO_OSCURO : UiTheme.EXITO_OSCURO);
        estado.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        JPanel izquierda = new JPanel();
        izquierda.setOpaque(false);
        izquierda.setLayout(new BoxLayout(izquierda, BoxLayout.X_AXIS));
        izquierda.add(nombre);
        izquierda.add(estado);

        JButton eliminarBtn = new JButton("Eliminar");
        eliminarBtn.setFont(UiTheme.TEXTO_BASE);
        eliminarBtn.setForeground(mesa.ocupada() ? UiTheme.MUTED : UiTheme.PELIGRO);
        eliminarBtn.setContentAreaFilled(false);
        eliminarBtn.setBorderPainted(false);
        eliminarBtn.setFocusPainted(false);
        eliminarBtn.setEnabled(!mesa.ocupada());
        eliminarBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eliminarBtn.addActionListener(e -> eliminarMesa(mesa));

        fila.add(izquierda, BorderLayout.WEST);
        fila.add(eliminarBtn, BorderLayout.EAST);
        return fila;
    }

    private void agregarMesa() {
        String nombre = nombreField.getText().trim();
        if (nombre.isEmpty()) {
            Toast.error(this, "Ponele un nombre a la mesa");
            return;
        }
        try {
            api.crearMesa(nombre);
            nombreField.setText("");
            alCambiar.run();
            Toast.exito(this, "Mesa \"" + nombre + "\" agregada");
        } catch (Exception ex) {
            Toast.error(this, "No se pudo agregar la mesa: " + ex.getMessage());
        }
    }

    private void eliminarMesa(Mesa mesa) {
        try {
            api.eliminarMesa(mesa.id);
            alCambiar.run();
        } catch (Exception ex) {
            Toast.error(this, "No se pudo eliminar la mesa: " + ex.getMessage());
        }
    }
}
