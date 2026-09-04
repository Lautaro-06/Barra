package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin > Productos: el menú del local. Alta, edición de precio/stock y el
 * toggle de disponibilidad (para pausar un producto sin perder el conteo
 * de stock, ej. "hoy no hay pescado").
 */
public class AdminProductosPanel extends JPanel {

    private final ApiClient api;
    private final Runnable alCambiar;
    private List<Producto> productos = List.of();

    private final DefaultTableModel modelo =
            new DefaultTableModel(new Object[]{"Producto", "Precio", "Stock", "Disponible"}, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };

    public AdminProductosPanel(ApiClient api, Runnable alCambiar) {
        super(new BorderLayout(0, 12));
        this.api = api;
        this.alCambiar = alCambiar;
        setOpaque(false);

        JLabel titulo = new JLabel("Productos");
        titulo.setFont(UiTheme.SUBTITULO);

        RoundButton nuevoBtn = new RoundButton("+ Nuevo producto", UiTheme.ACENTO, UiTheme.ACENTO_OSCURO);
        nuevoBtn.addActionListener(e -> abrirFormulario(null));

        JLabel ayuda = new JLabel("Doble clic en una fila para editarla");
        ayuda.setFont(UiTheme.TEXTO_BASE.deriveFont(11f));
        ayuda.setForeground(UiTheme.MUTED);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titulo, BorderLayout.WEST);
        header.add(nuevoBtn, BorderLayout.EAST);
        header.add(ayuda, BorderLayout.SOUTH);

        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(32);
        tabla.setFont(UiTheme.TEXTO_BASE);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.getTableHeader().setFont(UiTheme.TEXTO_NEGRITA);
        tabla.getColumnModel().getColumn(2).setCellRenderer(rendererStock());
        tabla.getColumnModel().getColumn(3).setCellRenderer(rendererDisponible());
        tabla.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int fila = tabla.rowAtPoint(e.getPoint());
                    if (fila >= 0 && fila < productos.size()) {
                        abrirFormulario(productos.get(fila));
                    }
                }
            }
        });

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

    private DefaultTableCellRenderer rendererDisponible() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                             boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                boolean disponible = Boolean.TRUE.equals(value);
                c.setForeground(disponible ? UiTheme.EXITO_OSCURO : UiTheme.MUTED);
                setText(disponible ? "Sí" : "No");
                return c;
            }
        };
    }

    /** Refresca la tabla con el catálogo actual (llamado por el polling). */
    public void setProductos(List<Producto> productos) {
        this.productos = new ArrayList<>(productos);
        modelo.setRowCount(0);
        for (Producto p : productos) {
            modelo.addRow(new Object[]{p.nombre, UiTheme.moneda(p.precio), p.stock, p.disponible});
        }
    }

    private void abrirFormulario(Producto existente) {
        Window ventana = SwingUtilities.getWindowAncestor(this);
        String titulo = existente == null ? "Nuevo producto" : "Editar producto";
        ProductoFormDialog dialogo = new ProductoFormDialog(ventana, titulo, existente, (nombre, precio, stock, disponible) -> {
            if (existente == null) {
                api.crearProducto(nombre, precio, stock, disponible);
                Toast.exito(this, "Producto \"" + nombre + "\" creado");
            } else {
                api.editarProducto(existente.id, nombre, precio, stock, disponible);
                Toast.exito(this, "Producto \"" + nombre + "\" actualizado");
            }
            alCambiar.run();
        });
        dialogo.setVisible(true);
    }
}
