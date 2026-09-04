package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
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
        JTextField nombre = new JTextField();
        JTextField precio = new JTextField();
        JTextField stock = new JTextField("0");

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Nombre"));
        form.add(nombre);
        form.add(new JLabel("Precio"));
        form.add(precio);
        form.add(new JLabel("Stock inicial"));
        form.add(stock);

        int r = JOptionPane.showConfirmDialog(this, form, "Nuevo producto",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        try {
            String nombreTxt = nombre.getText().trim();
            double precioVal = Double.parseDouble(precio.getText().trim().replace(",", "."));
            String stockTxt = stock.getText().trim();
            int stockVal = stockTxt.isEmpty() ? 0 : Integer.parseInt(stockTxt);

            if (nombreTxt.isEmpty() || precioVal <= 0) {
                JOptionPane.showMessageDialog(this, "El nombre no puede estar vacío y el precio debe ser mayor a 0.",
                        "Datos inválidos", JOptionPane.WARNING_MESSAGE);
                return;
            }

            api.crearProducto(nombreTxt, precioVal, stockVal);
            alCrear.run();
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Precio o stock inválido.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo crear el producto:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
