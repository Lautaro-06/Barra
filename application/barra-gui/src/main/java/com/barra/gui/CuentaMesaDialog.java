package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La cuenta de una mesa: se le van sumando rondas de pedido mientras el
 * comensal sigue en el local, y desde acá mismo se cierra y se genera el
 * ticket cuando termina.
 */
public class CuentaMesaDialog extends JDialog {

    private final ApiClient api;
    private final int mesaId;
    private final Runnable alCambiar;

    private final JPanel grillaProductos = new JPanel(new GridLayout(0, 3, 10, 10));
    private final JPanel listaRondas = new JPanel();
    private final JPanel listaCarrito = new JPanel();
    private final JLabel totalRondaLabel = new JLabel(UiTheme.moneda(0));
    private final JLabel totalMesaLabel = new JLabel(UiTheme.moneda(0));
    private final JTextField notaField = new JTextField();
    private final RoundButton agregarBtn = new RoundButton("Agregar a la cuenta", UiTheme.EXITO, UiTheme.EXITO_OSCURO);
    private final RoundButton cerrarCuentaBtn = new RoundButton("Cerrar cuenta y generar ticket", UiTheme.ACENTO, UiTheme.ACENTO_OSCURO);

    private final Map<Integer, CarritoItem> carrito = new LinkedHashMap<>();

    public CuentaMesaDialog(Window parent, ApiClient api, Mesa mesa, List<Producto> productos, Runnable alCambiar) {
        super(parent, mesa.nombre, ModalityType.APPLICATION_MODAL);
        this.api = api;
        this.mesaId = mesa.id;
        this.alCambiar = alCambiar;

        setSize(1040, 660);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(UiTheme.FONDO);
        setLayout(new BorderLayout(16, 16));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(construirPanelProductos(), BorderLayout.CENTER);
        add(construirPanelCuenta(mesa), BorderLayout.EAST);

        agregarBtn.addActionListener(e -> agregarRonda());
        cerrarCuentaBtn.addActionListener(e -> cerrarCuenta());

        setProductos(productos);
        cargarCuenta();
    }

    private JComponent construirPanelProductos() {
        JLabel titulo = new JLabel("Agregar productos");
        titulo.setFont(UiTheme.TITULO);

        grillaProductos.setBackground(UiTheme.FONDO);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UiTheme.FONDO);
        wrapper.add(grillaProductos, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UiTheme.FONDO);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.add(titulo, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JComponent construirPanelCuenta(Mesa mesa) {
        RoundedPanel panel = new RoundedPanel(new BorderLayout(0, 10), 16);
        panel.setPreferredSize(new Dimension(360, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel header = new JLabel(mesa.nombre);
        header.setFont(UiTheme.TITULO);

        JLabel subHeader = new JLabel("Pedidos de esta cuenta");
        subHeader.setFont(UiTheme.SUBTITULO);
        subHeader.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));

        listaRondas.setOpaque(false);
        JScrollPane scrollRondas = new JScrollPane(listaRondas);
        scrollRondas.setBorder(BorderFactory.createLineBorder(UiTheme.BORDE));
        scrollRondas.setPreferredSize(new Dimension(0, 170));
        scrollRondas.getViewport().setOpaque(false);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.add(header, BorderLayout.NORTH);
        JPanel subNorte = new JPanel(new BorderLayout());
        subNorte.setOpaque(false);
        subNorte.add(subHeader, BorderLayout.NORTH);
        subNorte.add(scrollRondas, BorderLayout.CENTER);
        norte.add(subNorte, BorderLayout.CENTER);

        JLabel nuevaRondaLbl = new JLabel("Ronda actual");
        nuevaRondaLbl.setFont(UiTheme.SUBTITULO);

        listaCarrito.setOpaque(false);
        JScrollPane scrollCarrito = new JScrollPane(listaCarrito);
        scrollCarrito.setBorder(null);
        scrollCarrito.setOpaque(false);
        scrollCarrito.getViewport().setOpaque(false);

        JPanel centro = new JPanel(new BorderLayout(0, 6));
        centro.setOpaque(false);
        centro.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        centro.add(nuevaRondaLbl, BorderLayout.NORTH);
        centro.add(scrollCarrito, BorderLayout.CENTER);

        notaField.setBorder(BorderFactory.createTitledBorder("Nota (opcional)"));
        notaField.setAlignmentX(Component.LEFT_ALIGNMENT);
        notaField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JPanel totalRondaPanel = new JPanel(new BorderLayout());
        totalRondaPanel.setOpaque(false);
        totalRondaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel totalRondaTitulo = new JLabel("Total de esta ronda");
        totalRondaTitulo.setFont(UiTheme.TEXTO_BASE);
        totalRondaLabel.setFont(UiTheme.SUBTITULO);
        totalRondaPanel.add(totalRondaTitulo, BorderLayout.WEST);
        totalRondaPanel.add(totalRondaLabel, BorderLayout.EAST);

        agregarBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        agregarBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        agregarBtn.setEnabled(false);

        JPanel totalMesaPanel = new JPanel(new BorderLayout());
        totalMesaPanel.setOpaque(false);
        totalMesaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalMesaPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JLabel totalMesaTitulo = new JLabel("Total de la mesa");
        totalMesaTitulo.setFont(UiTheme.TEXTO_BASE);
        totalMesaLabel.setFont(UiTheme.TOTAL);
        totalMesaPanel.add(totalMesaTitulo, BorderLayout.WEST);
        totalMesaPanel.add(totalMesaLabel, BorderLayout.EAST);

        cerrarCuentaBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        cerrarCuentaBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JPanel sur = new JPanel();
        sur.setOpaque(false);
        sur.setLayout(new BoxLayout(sur, BoxLayout.Y_AXIS));
        sur.add(notaField);
        sur.add(Box.createVerticalStrut(8));
        sur.add(totalRondaPanel);
        sur.add(Box.createVerticalStrut(8));
        sur.add(agregarBtn);
        sur.add(totalMesaPanel);
        sur.add(Box.createVerticalStrut(8));
        sur.add(cerrarCuentaBtn);

        panel.add(norte, BorderLayout.NORTH);
        panel.add(centro, BorderLayout.CENTER);
        panel.add(sur, BorderLayout.SOUTH);

        renderizarCarrito();
        return panel;
    }

    private void setProductos(List<Producto> productos) {
        grillaProductos.removeAll();
        for (Producto p : productos) {
            grillaProductos.add(ProductoCard.crear(p, this::agregarAlCarrito));
        }
        grillaProductos.revalidate();
        grillaProductos.repaint();
    }

    private void cargarCuenta() {
        try {
            Cuenta cuenta = api.obtenerCuentaMesa(mesaId);
            renderizarRondas(cuenta);
            totalMesaLabel.setText(UiTheme.moneda(cuenta.total));
        } catch (Exception ex) {
            Toast.error(this, "No se pudo cargar la cuenta: " + ex.getMessage());
        }
    }

    private void renderizarRondas(Cuenta cuenta) {
        listaRondas.removeAll();
        listaRondas.setLayout(new BoxLayout(listaRondas, BoxLayout.Y_AXIS));

        if (cuenta.pedidos.isEmpty()) {
            JLabel vacio = new JLabel("Todavía no se pidió nada");
            vacio.setForeground(UiTheme.MUTED);
            vacio.setFont(UiTheme.TEXTO_BASE);
            vacio.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            listaRondas.add(vacio);
        }

        for (Pedido pedido : cuenta.pedidos) {
            listaRondas.add(crearFilaRonda(pedido));
        }
        listaRondas.revalidate();
        listaRondas.repaint();
    }

    private JComponent crearFilaRonda(Pedido pedido) {
        StringBuilder items = new StringBuilder();
        for (Pedido.Detalle d : pedido.detalles) {
            if (items.length() > 0) items.append(", ");
            items.append(d.cantidad).append("x ").append(d.nombreProducto);
        }
        JLabel linea = new JLabel("<html><body style='width:220px'>" + UiTheme.escapeHtml(items.toString())
                + " <b>" + UiTheme.moneda(pedido.total) + "</b></body></html>");
        linea.setFont(UiTheme.TEXTO_BASE.deriveFont(12f));
        linea.setAlignmentX(Component.LEFT_ALIGNMENT);
        linea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDE),
                BorderFactory.createEmptyBorder(6, 4, 6, 4)));
        return linea;
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
            listaCarrito.add(Box.createVerticalStrut(6));
            total += item.subtotal();
        }

        totalRondaLabel.setText(UiTheme.moneda(total));
        agregarBtn.setEnabled(!carrito.isEmpty());
        listaCarrito.revalidate();
        listaCarrito.repaint();
    }

    private JComponent crearFilaCarrito(CarritoItem item) {
        JPanel fila = new JPanel(new BorderLayout(8, 0));
        fila.setOpaque(false);
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel nombre = new JLabel("<html><body style='width:110px'>" + UiTheme.escapeHtml(item.producto.nombre) + "</body></html>");
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

    private void agregarRonda() {
        if (carrito.isEmpty()) return;

        List<int[]> detalles = new ArrayList<>();
        for (CarritoItem item : carrito.values()) {
            detalles.add(new int[]{item.producto.id, item.cantidad});
        }
        String notaTexto = notaField.getText().trim();
        String nota = notaTexto.isEmpty() ? null : notaTexto;

        agregarBtn.setEnabled(false);
        try {
            api.crearPedidoMesa(mesaId, nota, detalles);
            carrito.clear();
            notaField.setText("");
            renderizarCarrito();
            cargarCuenta();
            Toast.exito(this, "Ronda agregada a la cuenta");
        } catch (Exception ex) {
            Toast.error(this, "No se pudo agregar la ronda: " + ex.getMessage());
        } finally {
            agregarBtn.setEnabled(!carrito.isEmpty());
        }
    }

    private void cerrarCuenta() {
        if (!carrito.isEmpty()) {
            Toast.error(this, "Confirmá o vaciá el carrito antes de cerrar la cuenta");
            return;
        }
        try {
            Cuenta cuenta = api.cerrarMesa(mesaId);
            if (cuenta.pedidos.isEmpty()) {
                // Mesa cerrada sin haber pedido nada: no tiene sentido mostrar un ticket vacío.
                dispose();
                return;
            }
            TicketDialog ticket = new TicketDialog(getOwner(), api, cuenta);
            ticket.setVisible(true);
            dispose();
        } catch (Exception ex) {
            Toast.error(this, "No se pudo cerrar la cuenta: " + ex.getMessage());
        }
    }

    @Override
    public void dispose() {
        alCambiar.run();
        super.dispose();
    }
}
