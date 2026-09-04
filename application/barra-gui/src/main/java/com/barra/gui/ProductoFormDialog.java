package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;

/**
 * Formulario para crear o editar un producto (nombre, precio, stock y si
 * está disponible para vender). Lo usa Admin > Productos tanto para el
 * alta como para la edición, para no repetir el mismo formulario dos veces.
 */
public class ProductoFormDialog extends JDialog {

    /** Qué hacer con los datos del formulario al tocar "Guardar". */
    public interface Guardador {
        void guardar(String nombre, double precio, int stock, boolean disponible) throws Exception;
    }

    private final JTextField nombreField = campoTexto();
    private final JTextField precioField = campoTexto();
    private final JTextField stockField = campoTexto();
    private final JCheckBox disponibleCheck = new JCheckBox("Disponible para vender");

    public ProductoFormDialog(Window parent, String titulo, Producto existente, Guardador guardador) {
        super(parent, titulo, ModalityType.APPLICATION_MODAL);
        setResizable(false);

        if (existente != null) {
            nombreField.setText(existente.nombre);
            precioField.setText(formatearNumero(existente.precio));
            stockField.setText(String.valueOf(existente.stock));
            disponibleCheck.setSelected(existente.disponible);
        } else {
            stockField.setText("0");
            disponibleCheck.setSelected(true);
        }

        JPanel contenido = new JPanel();
        contenido.setBackground(UiTheme.TARJETA);
        contenido.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setPreferredSize(new Dimension(320, 340));

        JLabel tituloLbl = new JLabel(titulo);
        tituloLbl.setFont(UiTheme.TITULO);
        tituloLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        disponibleCheck.setOpaque(false);
        disponibleCheck.setFont(UiTheme.TEXTO_BASE);
        disponibleCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        disponibleCheck.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        contenido.add(tituloLbl);
        contenido.add(Box.createVerticalStrut(22));
        contenido.add(etiquetaCampo("Nombre"));
        contenido.add(nombreField);
        contenido.add(Box.createVerticalStrut(14));
        contenido.add(etiquetaCampo("Precio"));
        contenido.add(precioField);
        contenido.add(Box.createVerticalStrut(14));
        contenido.add(etiquetaCampo("Stock"));
        contenido.add(stockField);
        contenido.add(Box.createVerticalStrut(14));
        contenido.add(disponibleCheck);
        contenido.add(Box.createVerticalStrut(22));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton cancelar = botonSecundario("Cancelar");
        RoundButton guardar = new RoundButton("Guardar", UiTheme.ACENTO, UiTheme.ACENTO_OSCURO);
        botones.add(cancelar);
        botones.add(guardar);
        contenido.add(botones);

        cancelar.addActionListener(e -> dispose());
        guardar.addActionListener(e -> {
            try {
                String nombre = nombreField.getText().trim();
                double precio = Double.parseDouble(precioField.getText().trim().replace(",", "."));
                String stockTxt = stockField.getText().trim();
                int stock = stockTxt.isEmpty() ? 0 : Integer.parseInt(stockTxt);

                if (nombre.isEmpty() || precio <= 0) {
                    Toast.error(this, "El nombre no puede estar vacío y el precio debe ser mayor a 0.");
                    return;
                }
                guardador.guardar(nombre, precio, stock, disponibleCheck.isSelected());
                dispose();
            } catch (NumberFormatException nfe) {
                Toast.error(this, "Precio o stock inválido.");
            } catch (Exception ex) {
                Toast.error(this, "No se pudo guardar: " + ex.getMessage());
            }
        });

        add(contenido, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(parent);
    }

    private static String formatearNumero(double valor) {
        if (valor == Math.floor(valor)) return String.valueOf((long) valor);
        return String.valueOf(valor);
    }

    private static JTextField campoTexto() {
        JTextField t = new JTextField();
        t.setFont(UiTheme.TEXTO_BASE);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDE),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        return t;
    }

    private static JLabel etiquetaCampo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(UiTheme.TEXTO_BASE.deriveFont(11f));
        l.setForeground(UiTheme.MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return l;
    }

    private static JButton botonSecundario(String texto) {
        JButton b = new JButton(texto);
        b.setFont(UiTheme.TEXTO_NEGRITA);
        b.setForeground(UiTheme.MUTED);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
