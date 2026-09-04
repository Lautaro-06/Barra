package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.List;

/**
 * Pantalla "Catálogo": qué se vende y cuánto queda, pensada para quien
 * administra el local (no para el mostrador). Desde acá se puede dar de
 * alta un producto nuevo sin tocar la base de datos a mano.
 */
public class CatalogoPanel extends JPanel {

    private final ApiClient api;
    private final Runnable alCrear;

    private final DefaultTableModel modelo =
            new DefaultTableModel(new Object[]{"Producto", "Precio", "Stock"}, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };

    public CatalogoPanel(ApiClient api, Runnable alCrear) {
        super(new BorderLayout(0, 12));
        this.api = api;
        this.alCrear = alCrear;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiTheme.FONDO);

        JLabel titulo = new JLabel("Catálogo");
        titulo.setFont(UiTheme.TITULO);

        RoundButton nuevoBtn = new RoundButton("+ Nuevo producto", UiTheme.ACENTO, UiTheme.ACENTO_OSCURO);
        nuevoBtn.addActionListener(e -> abrirDialogoNuevoProducto());

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titulo, BorderLayout.WEST);
        header.add(nuevoBtn, BorderLayout.EAST);

        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(32);
        tabla.setFont(UiTheme.TEXTO_BASE);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.getTableHeader().setFont(UiTheme.TEXTO_NEGRITA);
        tabla.getColumnModel().getColumn(2).setCellRenderer(rendererStock());

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDE));
        scroll.getViewport().setBackground(UiTheme.TARJETA);

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    private DefaultTableCellRenderer rendererStock() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                             boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int stock = value instanceof Number n ? n.intValue() : 0;
                c.setForeground(stock == 0 ? UiTheme.PELIGRO : stock < 5 ? UiTheme.ACENTO_OSCURO : UiTheme.TEXTO);
                return c;
            }
        };
    }

    /** Refresca la tabla con el catálogo actual (llamado por el polling). */
    public void setProductos(List<Producto> productos) {
        modelo.setRowCount(0);
        for (Producto p : productos) {
            modelo.addRow(new Object[]{p.nombre, UiTheme.moneda(p.precio), p.stock});
        }
    }

    private void abrirDialogoNuevoProducto() {
        Window ventana = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(ventana, "Nuevo producto", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setResizable(false);

        JPanel contenido = new JPanel();
        contenido.setBackground(UiTheme.TARJETA);
        contenido.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setPreferredSize(new Dimension(320, 300));

        JLabel titulo = new JLabel("Nuevo producto");
        titulo.setFont(UiTheme.TITULO);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField nombre = campoTexto();
        JTextField precio = campoTexto();
        JTextField stock = campoTexto();
        stock.setText("0");

        contenido.add(titulo);
        contenido.add(Box.createVerticalStrut(22));
        contenido.add(etiquetaCampo("Nombre"));
        contenido.add(nombre);
        contenido.add(Box.createVerticalStrut(14));
        contenido.add(etiquetaCampo("Precio"));
        contenido.add(precio);
        contenido.add(Box.createVerticalStrut(14));
        contenido.add(etiquetaCampo("Stock inicial"));
        contenido.add(stock);
        contenido.add(Box.createVerticalStrut(22));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton cancelar = botonSecundario("Cancelar");
        RoundButton crear = new RoundButton("Crear producto", UiTheme.ACENTO, UiTheme.ACENTO_OSCURO);
        botones.add(cancelar);
        botones.add(crear);
        contenido.add(botones);

        cancelar.addActionListener(e -> dialog.dispose());
        crear.addActionListener(e -> {
            if (crearProducto(nombre.getText(), precio.getText(), stock.getText())) {
                dialog.dispose();
            }
        });

        dialog.add(contenido, BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /** @return true si el producto se creó y el diálogo puede cerrarse. */
    private boolean crearProducto(String nombreTxt, String precioTxt, String stockTxt) {
        try {
            String nombre = nombreTxt.trim();
            double precioVal = Double.parseDouble(precioTxt.trim().replace(",", "."));
            String stockLimpio = stockTxt.trim();
            int stockVal = stockLimpio.isEmpty() ? 0 : Integer.parseInt(stockLimpio);

            if (nombre.isEmpty() || precioVal <= 0) {
                Toast.error(this, "El nombre no puede estar vacío y el precio debe ser mayor a 0.");
                return false;
            }

            api.crearProducto(nombre, precioVal, stockVal);
            alCrear.run();
            Toast.exito(this, "Producto \"" + nombre + "\" creado");
            return true;
        } catch (NumberFormatException nfe) {
            Toast.error(this, "Precio o stock inválido.");
            return false;
        } catch (Exception ex) {
            Toast.error(this, "No se pudo crear el producto: " + ex.getMessage());
            return false;
        }
    }

    private JTextField campoTexto() {
        JTextField t = new JTextField();
        t.setFont(UiTheme.TEXTO_BASE);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDE),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        return t;
    }

    private JLabel etiquetaCampo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(UiTheme.TEXTO_BASE.deriveFont(11f));
        l.setForeground(UiTheme.MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return l;
    }

    private JButton botonSecundario(String texto) {
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
