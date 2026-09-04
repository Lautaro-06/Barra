package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla "Admin": todo lo que hace que la app se pueda adaptar a
 * cualquier local sin tocar código - el menú (productos y disponibilidad),
 * las mesas del salón y el nombre del negocio.
 */
public class AdminPanel extends JPanel {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contenido = new JPanel(cardLayout);
    private final Map<String, TabButton> tabs = new LinkedHashMap<>();

    private final AdminProductosPanel productosPanel;
    private final AdminMesasPanel mesasPanel;
    private final AdminConfiguracionPanel configuracionPanel;

    public AdminPanel(ApiClient api, Runnable alCambiar) {
        super(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiTheme.FONDO);

        productosPanel = new AdminProductosPanel(api, alCambiar);
        mesasPanel = new AdminMesasPanel(api, alCambiar);
        configuracionPanel = new AdminConfiguracionPanel(api, alCambiar);

        JLabel titulo = new JLabel("Admin");
        titulo.setFont(UiTheme.TITULO);

        JPanel tabsPanel = new JPanel();
        tabsPanel.setOpaque(false);
        tabsPanel.setLayout(new BoxLayout(tabsPanel, BoxLayout.X_AXIS));
        tabsPanel.add(crearTab("Productos", "productos"));
        tabsPanel.add(Box.createHorizontalStrut(8));
        tabsPanel.add(crearTab("Mesas", "mesas"));
        tabsPanel.add(Box.createHorizontalStrut(8));
        tabsPanel.add(crearTab("Configuración", "configuracion"));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(titulo);
        header.add(Box.createVerticalStrut(12));
        header.add(tabsPanel);

        contenido.add(envolver(productosPanel), "productos");
        contenido.add(envolver(mesasPanel), "mesas");
        contenido.add(envolver(configuracionPanel), "configuracion");
        contenido.setOpaque(false);

        add(header, BorderLayout.NORTH);
        add(contenido, BorderLayout.CENTER);

        mostrar("productos");
    }

    private JPanel envolver(JPanel panel) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }

    private TabButton crearTab(String texto, String clave) {
        TabButton boton = new TabButton(texto);
        boton.addActionListener(e -> mostrar(clave));
        tabs.put(clave, boton);
        return boton;
    }

    private void mostrar(String clave) {
        cardLayout.show(contenido, clave);
        tabs.forEach((k, boton) -> boton.setSeleccionado(k.equals(clave)));
    }

    public void setProductos(List<Producto> productos) {
        productosPanel.setProductos(productos);
    }

    public void setMesas(List<Mesa> mesas) {
        mesasPanel.setMesas(mesas);
    }

    public void setConfiguracion(Configuracion configuracion) {
        configuracionPanel.setConfiguracion(configuracion);
    }
}
