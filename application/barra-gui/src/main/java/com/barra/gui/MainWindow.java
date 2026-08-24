package com.barra.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * MainWindow
 *
 * GUI mínima (Swing, para no atarnos todavía a JavaFX) que demuestra el
 * punto 1 de la arquitectura: la ventana le habla al backend Python por
 * HTTP local y muestra lo que vuelve. Sirve de base para las pantallas
 * reales (Mostrador, Cocina, etc. - ver sección 19 del documento) que se
 * van a ir sumando.
 */
public class MainWindow extends JFrame {

    private final ApiClient api = new ApiClient();
    private final DefaultTableModel productosModel =
            new DefaultTableModel(new Object[]{"ID", "Producto", "Precio", "Stock"}, 0);
    private final DefaultTableModel pedidosModel =
            new DefaultTableModel(new Object[]{"ID", "Fecha", "Estado", "Total", "Nota"}, 0);

    public MainWindow() {
        super("Barra - Sistema de Pedidos (prototipo)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);

        JLabel estadoConexion = new JLabel("Verificando conexión con el backend...");
        estadoConexion.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(estadoConexion, BorderLayout.NORTH);

        JTable tablaProductos = new JTable(productosModel);
        JTable tablaPedidos = new JTable(pedidosModel);

        JPanel panelProductos = new JPanel(new BorderLayout());
        panelProductos.setBorder(BorderFactory.createTitledBorder("Catálogo"));
        panelProductos.add(new JScrollPane(tablaProductos), BorderLayout.CENTER);

        JPanel panelPedidos = new JPanel(new BorderLayout());
        panelPedidos.setBorder(BorderFactory.createTitledBorder("Pedidos"));
        panelPedidos.add(new JScrollPane(tablaPedidos), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelProductos, panelPedidos);
        split.setResizeWeight(0.4);
        add(split, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel();
        JButton btnRefrescar = new JButton("Refrescar");
        JButton btnCrearPedido = new JButton("Nuevo pedido de prueba (2 hamburguesas)");
        JButton btnMarcarListo = new JButton("Marcar seleccionado como 'listo'");
        panelBotones.add(btnRefrescar);
        panelBotones.add(btnCrearPedido);
        panelBotones.add(btnMarcarListo);
        add(panelBotones, BorderLayout.SOUTH);

        btnRefrescar.addActionListener(e -> refrescarDatos(estadoConexion));

        btnCrearPedido.addActionListener(e -> {
            try {
                api.crearPedido("Pedido de prueba desde Java",
                        List.of(new int[]{1, 2}));
                refrescarDatos(estadoConexion);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al crear pedido: " + ex.getMessage());
            }
        });

        btnMarcarListo.addActionListener(e -> {
            int fila = tablaPedidos.getSelectedRow();
            if (fila == -1) {
                JOptionPane.showMessageDialog(this, "Seleccioná un pedido primero.");
                return;
            }
            int pedidoId = (int) pedidosModel.getValueAt(fila, 0);
            try {
                api.cambiarEstado(pedidoId, "listo");
                refrescarDatos(estadoConexion);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cambiar estado: " + ex.getMessage());
            }
        });

        refrescarDatos(estadoConexion);
    }

    private void refrescarDatos(JLabel estadoConexion) {
        boolean vivo = api.healthCheck();
        estadoConexion.setText(vivo
                ? "Backend Python: conectado (http://127.0.0.1:8000)"
                : "Backend Python: SIN CONEXIÓN - arrancá primero el servidor (uvicorn)");

        if (!vivo) return;

        try {
            productosModel.setRowCount(0);
            for (Producto p : api.listarProductos()) {
                productosModel.addRow(new Object[]{p.id, p.nombre, p.precio, p.stock});
            }

            pedidosModel.setRowCount(0);
            for (Pedido p : api.listarPedidos()) {
                pedidosModel.addRow(new Object[]{p.id, p.fecha, p.estado, p.total, p.nota});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar datos: " + ex.getMessage());
        }
    }
}
