package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MainWindow
 *
 * Ventana principal: una barra lateral para moverse entre las cuatro
 * pantallas del local (Vender, Mesas, Cocina, Admin) y, atrás de todo, un
 * polling periódico al backend Python que las mantiene sincronizadas entre sí.
 */
public class MainWindow extends JFrame {

    private final ApiClient api = new ApiClient();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contenido = new JPanel(cardLayout);
    private final Map<String, NavButton> botonesNav = new LinkedHashMap<>();

    private final VentaPanel ventaPanel = new VentaPanel(api, this::refrescarDatos);
    private final MesasPanel mesasPanel = new MesasPanel(api, this::refrescarDatos);
    private final CocinaPanel cocinaPanel = new CocinaPanel(api, this::refrescarDatos);
    private final AdminPanel adminPanel = new AdminPanel(api, this::refrescarDatos);

    private final JLabel estadoDot = new JLabel("●");
    private final JLabel estadoTexto = new JLabel("Conectando...");
    private final JLabel marcaTexto = new JLabel("Barra");

    public MainWindow() {
        super("Barra");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 680);
        setMinimumSize(new Dimension(860, 560));
        setLocationRelativeTo(null);
        setIconImage(AppIcons.marcaComoImagen(64));
        getContentPane().setBackground(UiTheme.FONDO);
        setLayout(new BorderLayout());

        add(construirBarraLateral(), BorderLayout.WEST);

        contenido.add(ventaPanel, "vender");
        contenido.add(mesasPanel, "mesas");
        contenido.add(cocinaPanel, "cocina");
        contenido.add(adminPanel, "admin");
        add(contenido, BorderLayout.CENTER);

        mostrarPantalla("vender");
        refrescarDatos();

        // Polling simple cada 4s: alcanza para que el mostrador, las mesas
        // y la cocina se vean sincronizados entre sí casi al instante, sin
        // meter WebSockets todavía a esta primera versión de la GUI.
        Timer timer = new Timer(4000, e -> refrescarDatos());
        timer.start();
    }

    private JComponent construirBarraLateral() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UiTheme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(190, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 16, 16, 16));

        JPanel marca = new JPanel();
        marca.setOpaque(false);
        marca.setLayout(new BoxLayout(marca, BoxLayout.X_AXIS));
        marca.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel logo = new JLabel(AppIcons.marca(30));
        marcaTexto.setFont(UiTheme.TITULO);
        marcaTexto.setForeground(Color.WHITE);
        marcaTexto.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        marca.add(logo);
        marca.add(marcaTexto);
        sidebar.add(marca);
        sidebar.add(Box.createVerticalStrut(28));

        sidebar.add(crearBotonNav("Vender", AppIcons.vender(Color.WHITE), "vender"));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(crearBotonNav("Mesas", AppIcons.mesas(Color.WHITE), "mesas"));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(crearBotonNav("Cocina", AppIcons.cocina(Color.WHITE), "cocina"));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(crearBotonNav("Admin", AppIcons.admin(Color.WHITE), "admin"));

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

    private NavButton crearBotonNav(String texto, javax.swing.Icon icono, String card) {
        NavButton btn = new NavButton(texto, icono);
        btn.addActionListener(e -> mostrarPantalla(card));
        botonesNav.put(card, btn);
        return btn;
    }

    private void mostrarPantalla(String card) {
        cardLayout.show(contenido, card);
        botonesNav.forEach((clave, boton) -> boton.setSeleccionado(clave.equals(card)));
    }

    private void refrescarDatos() {
        boolean vivo = api.healthCheck();
        estadoDot.setForeground(vivo ? UiTheme.EXITO : UiTheme.PELIGRO);
        estadoTexto.setText(vivo ? "Backend conectado" : "Backend caído");

        if (!vivo) return;

        try {
            List<Producto> productos = api.listarProductos();
            List<Pedido> pedidos = api.listarPedidos();
            List<Mesa> mesas = api.listarMesas();
            Configuracion config = api.obtenerConfiguracion();

            ventaPanel.setProductos(productos);
            mesasPanel.setProductos(productos);
            mesasPanel.setMesas(mesas);
            cocinaPanel.setPedidos(pedidos);
            adminPanel.setProductos(productos);
            adminPanel.setMesas(mesas);
            adminPanel.setConfiguracion(config);

            if (!marcaTexto.getText().equals(config.nombreLocal)) {
                marcaTexto.setText(config.nombreLocal);
                setTitle(config.nombreLocal);
            }
        } catch (Exception ex) {
            estadoTexto.setText("Error al sincronizar");
        }
    }
}
