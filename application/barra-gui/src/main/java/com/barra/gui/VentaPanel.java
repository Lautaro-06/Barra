package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla "Vender": el mostrador. En vez de una tabla para elegir productos
 * y un botón fijo de prueba, acá se toca el producto para sumarlo al pedido
 * (como una caja registradora táctil) y se confirma de una.
 */
public class VentaPanel extends JPanel {

    private final ApiClient api;
    private final Runnable alConfirmar; // dispara un refresco del resto de la app

    private final JPanel grillaProductos = new JPanel(new GridLayout(0, 3, 12, 12));
    private final JPanel listaCarrito = new JPanel();
    private final JLabel totalLabel = new JLabel(UiTheme.moneda(0));
    private final JTextField notaField = new JTextField();
    private final RoundButton confirmarBtn =
            new RoundButton("Confirmar pedido", UiTheme.EXITO, UiTheme.EXITO_OSCURO);

    private final Map<Integer, CarritoItem> carrito = new LinkedHashMap<>();

    public VentaPanel(ApiClient api, Runnable alConfirmar) {
        super(new BorderLayout(16, 16));
        this.api = api;
        this.alConfirmar = alConfirmar;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiTheme.FONDO);

        JLabel titulo = new JLabel("Nuevo pedido");
        titulo.setFont(UiTheme.TITULO);

        grillaProductos.setBackground(UiTheme.FONDO);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UiTheme.FONDO);
        wrapper.add(grillaProductos, BorderLayout.NORTH);

        JScrollPane scrollProductos = new JScrollPane(wrapper);
        scrollProductos.setBorder(null);
        scrollProductos.getViewport().setBackground(UiTheme.FONDO);
        scrollProductos.getVerticalScrollBar().setUnitIncrement(16);

        JPanel izquierda = new JPanel(new BorderLayout(0, 12));
        izquierda.setOpaque(false);
        izquierda.add(titulo, BorderLayout.NORTH);
        izquierda.add(scrollProductos, BorderLayout.CENTER);

        add(izquierda, BorderLayout.CENTER);
        add(construirPanelCarrito(), BorderLayout.EAST);

        confirmarBtn.addActionListener(e -> confirmarPedido());
    }

    private JComponent construirPanelCarrito() {
        RoundedPanel panel = new RoundedPanel(new BorderLayout(0, 12), 16);
        panel.setPreferredSize(new Dimension(300, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel titulo = new JLabel("Pedido actual");
        titulo.setFont(UiTheme.SUBTITULO);

        listaCarrito.setOpaque(false);
        JScrollPane scrollCarrito = new JScrollPane(listaCarrito);
        scrollCarrito.setBorder(null);
        scrollCarrito.setOpaque(false);
        scrollCarrito.getViewport().setOpaque(false);

        notaField.setBorder(BorderFactory.createTitledBorder("Nota (opcional)"));
        notaField.setAlignmentX(Component.LEFT_ALIGNMENT);
        notaField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setOpaque(false);
        totalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel totalTitulo = new JLabel("Total");
        totalTitulo.setFont(UiTheme.TEXTO_BASE);
        totalLabel.setFont(UiTheme.TOTAL);
        totalPanel.add(totalTitulo, BorderLayout.WEST);
        totalPanel.add(totalLabel, BorderLayout.EAST);

        confirmarBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmarBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        confirmarBtn.setEnabled(false);

        JPanel pie = new JPanel();
        pie.setOpaque(false);
        pie.setLayout(new BoxLayout(pie, BoxLayout.Y_AXIS));
        pie.add(notaField);
        pie.add(Box.createVerticalStrut(10));
        pie.add(totalPanel);
        pie.add(Box.createVerticalStrut(10));
        pie.add(confirmarBtn);

        panel.add(titulo, BorderLayout.NORTH);
        panel.add(scrollCarrito, BorderLayout.CENTER);
        panel.add(pie, BorderLayout.SOUTH);

        renderizarCarrito();
        return panel;
    }

    /** Refresca la grilla de productos disponibles (se llama cada vez que llega el polling). */
    public void setProductos(List<Producto> productos) {
        grillaProductos.removeAll();
        for (Producto p : productos) {
            grillaProductos.add(crearTarjetaProducto(p));
        }
        grillaProductos.revalidate();
        grillaProductos.repaint();
    }

    private JComponent crearTarjetaProducto(Producto p) {
        RoundedPanel tarjeta = new RoundedPanel(new BorderLayout(), 14);
        tarjeta.setPreferredSize(new Dimension(150, 92));
        tarjeta.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        boolean sinStock = p.stock <= 0;

        JLabel nombre = new JLabel("<html><body style='width:120px'>" + UiTheme.escapeHtml(p.nombre) + "</body></html>");
        nombre.setFont(UiTheme.TEXTO_NEGRITA);
        nombre.setForeground(UiTheme.TEXTO);
        nombre.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel precio = new JLabel(UiTheme.moneda(p.precio));
        precio.setFont(UiTheme.SUBTITULO);
        precio.setForeground(UiTheme.ACENTO_OSCURO);
        precio.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel stockLbl = new JLabel(sinStock ? "Sin stock" : "Stock: " + p.stock);
        stockLbl.setFont(UiTheme.TEXTO_BASE.deriveFont(11f));
        stockLbl.setForeground(sinStock ? UiTheme.PELIGRO : (p.stock < 5 ? UiTheme.ACENTO_OSCURO : UiTheme.MUTED));
        stockLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.add(nombre);
        textos.add(Box.createVerticalStrut(6));
        textos.add(precio);
        textos.add(stockLbl);

        tarjeta.add(textos, BorderLayout.CENTER);

        if (sinStock) {
            tarjeta.setColorFondo(new Color(0xF2, 0xF2, 0xF4));
        } else {
            tarjeta.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            tarjeta.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    tarjeta.setColorFondo(new Color(0xFF, 0xF1, 0xE8));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    tarjeta.setColorFondo(UiTheme.TARJETA);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    agregarAlCarrito(p);
                }
            });
        }
        return tarjeta;
    }

    private void agregarAlCarrito(Producto p) {
        CarritoItem item = carrito.get(p.id);
        if (item == null) {
            carrito.put(p.id, new CarritoItem(p, 1));
        } else if (item.cantidad < p.stock) {
            item.cantidad++;
        }
        renderizarCarrito();
    }

    private void cambiarCantidad(int productoId, int delta) {
        CarritoItem item = carrito.get(productoId);
        if (item == null) return;
        int nueva = item.cantidad + delta;
        if (nueva <= 0) {
            carrito.remove(productoId);
        } else if (nueva <= item.producto.stock) {
            item.cantidad = nueva;
        }
        renderizarCarrito();
    }

    private void quitarDelCarrito(int productoId) {
        carrito.remove(productoId);
        renderizarCarrito();
    }

    private void renderizarCarrito() {
        listaCarrito.removeAll();
        listaCarrito.setLayout(new BoxLayout(listaCarrito, BoxLayout.Y_AXIS));

        if (carrito.isEmpty()) {
            JLabel vacio = new JLabel("Tocá un producto para agregarlo");
            vacio.setForeground(UiTheme.MUTED);
            vacio.setFont(UiTheme.TEXTO_BASE);
            vacio.setAlignmentX(Component.LEFT_ALIGNMENT);
            listaCarrito.add(vacio);
        }

        double total = 0;
        for (CarritoItem item : carrito.values()) {
            listaCarrito.add(crearFilaCarrito(item));
            listaCarrito.add(Box.createVerticalStrut(8));
            total += item.subtotal();
        }

        totalLabel.setText(UiTheme.moneda(total));
        confirmarBtn.setEnabled(!carrito.isEmpty());
        listaCarrito.revalidate();
        listaCarrito.repaint();
    }

    private JComponent crearFilaCarrito(CarritoItem item) {
        JPanel fila = new JPanel(new BorderLayout(8, 0));
        fila.setOpaque(false);
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel nombre = new JLabel("<html><body style='width:90px'>" + UiTheme.escapeHtml(item.producto.nombre) + "</body></html>");
        nombre.setFont(UiTheme.TEXTO_BASE);

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        controles.setOpaque(false);
        JButton menos = botonMini("–");
        JLabel cantidadLbl = new JLabel(String.valueOf(item.cantidad));
        cantidadLbl.setFont(UiTheme.TEXTO_NEGRITA);
        JButton mas = botonMini("+");
        JButton sacar = botonMini("x");

        menos.addActionListener(e -> cambiarCantidad(item.producto.id, -1));
        mas.addActionListener(e -> cambiarCantidad(item.producto.id, +1));
        sacar.addActionListener(e -> quitarDelCarrito(item.producto.id));

        controles.add(menos);
        controles.add(cantidadLbl);
        controles.add(mas);
        controles.add(sacar);

        fila.add(nombre, BorderLayout.CENTER);
        fila.add(controles, BorderLayout.EAST);
        return fila;
    }

    private JButton botonMini(String texto) {
        JButton b = new JButton(texto);
        b.setMargin(new Insets(2, 6, 2, 6));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void confirmarPedido() {
        if (carrito.isEmpty()) return;

        List<int[]> detalles = new ArrayList<>();
        for (CarritoItem item : carrito.values()) {
            detalles.add(new int[]{item.producto.id, item.cantidad});
        }
        String notaTexto = notaField.getText().trim();
        String nota = notaTexto.isEmpty() ? null : notaTexto;

        confirmarBtn.setEnabled(false);
        try {
            Pedido creado = api.crearPedido(nota, detalles);
            carrito.clear();
            notaField.setText("");
            renderizarCarrito();
            alConfirmar.run();
            Toast.exito(this, "Pedido #" + creado.id + " enviado a cocina");
        } catch (Exception ex) {
            Toast.error(this, "No se pudo confirmar el pedido: " + ex.getMessage());
        } finally {
            confirmarBtn.setEnabled(!carrito.isEmpty());
        }
    }
}
