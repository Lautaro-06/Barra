package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.util.List;

/**
 * MainWindow
 *
 * Ventana principal: una barra lateral para moverse entre las tres pantallas
 * del local (Vender, Cocina, Catálogo) y, atrás de todo, un polling
 * periódico al backend Python que las mantiene sincronizadas entre sí.
 */
public class MainWindow extends JFrame {

    private final ApiClient api = new ApiClient();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contenido = new JPanel(cardLayout);

    private final VentaPanel ventaPanel = new VentaPanel(api, this::refrescarDatos);
    private final CocinaPanel cocinaPanel = new CocinaPanel(api, this::refrescarDatos);
    private final CatalogoPanel catalogoPanel = new CatalogoPanel(api, this::refrescarDatos);

    private final JLabel estadoDot = new JLabel("●");
    private final JLabel estadoTexto = new JLabel("Conectando...");

    public MainWindow() {
        super("Barra");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 680);
        setMinimumSize(new Dimension(860, 560));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UiTheme.FONDO);
        setLayout(new BorderLayout());

        add(construirBarraLateral(), BorderLayout.WEST);

        contenido.add(ventaPanel, "vender");
        contenido.add(cocinaPanel, "cocina");
        contenido.add(catalogoPanel, "catalogo");
        add(contenido, BorderLayout.CENTER);

        refrescarDatos();

        // Polling simple cada 4s: alcanza para que el mostrador y la cocina
        // vean los cambios del otro casi al instante, sin meter WebSockets
        // todavía a esta primera versión de la GUI.
        Timer timer = new Timer(4000, e -> refrescarDatos());
        timer.start();
    }

    private JComponent construirBarraLateral() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UiTheme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(190, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 16, 16, 16));

        JLabel marca = new JLabel("🍔 Barra");
        marca.setFont(UiTheme.TITULO);
        marca.setForeground(Color.WHITE);
        marca.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(marca);
        sidebar.add(Box.createVerticalStrut(28));

        sidebar.add(botonNav("🧾  Vender", "vender"));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(botonNav("🧑‍🍳  Cocina", "cocina"));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(botonNav("📦  Catálogo", "catalogo"));

        sidebar.add(Box.createVerticalGlue());

        JPanel estadoPanel = new JPanel();
        estadoPanel.setOpaque(false);
        estadoPanel.setLayout(new BoxLayout(estadoPanel, BoxLayout.X_AXIS));
        estadoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        estadoDot.setForeground(UiTheme.PELIGRO);
        estadoTexto.setForeground(new Color(0xB8, 0xBA, 0xC4));
        estadoTexto.setFont(UiTheme.TEXTO_BASE.deriveFont(11f));
        estadoPanel.add(estadoDot);
        estadoPanel.add(Box.createHorizontalStrut(6));
        estadoPanel.add(estadoTexto);
        sidebar.add(estadoPanel);

        return sidebar;
    }

    private JButton botonNav(String texto, String card) {
        JButton btn = new JButton(texto);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setForeground(Color.WHITE);
        btn.setBackground(UiTheme.SIDEBAR);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setFont(UiTheme.TEXTO_NEGRITA);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> cardLayout.show(contenido, card));
        return btn;
    }

    private void refrescarDatos() {
        boolean vivo = api.healthCheck();
        estadoDot.setForeground(vivo ? UiTheme.EXITO : UiTheme.PELIGRO);
        estadoTexto.setText(vivo ? "Backend conectado" : "Backend caído");

        if (!vivo) return;

        try {
            List<Producto> productos = api.listarProductos();
            List<Pedido> pedidos = api.listarPedidos();
            ventaPanel.setProductos(productos);
            cocinaPanel.setPedidos(pedidos);
            catalogoPanel.setProductos(productos);
        } catch (Exception ex) {
            estadoTexto.setText("Error al sincronizar");
        }
    }
}
