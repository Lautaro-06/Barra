package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Pantalla "Mesas": el salón. Cada tarjeta es una mesa - verde y "Libre" si
 * no tiene a nadie, naranja y con el total acumulado si tiene la cuenta
 * abierta. Tocarla abre (o retoma) la cuenta de esa mesa.
 */
public class MesasPanel extends JPanel {

    private final ApiClient api;
    private final Runnable alCambiar;

    private final JPanel grilla = new JPanel(new GridLayout(0, 4, 14, 14));
    private List<Producto> productos = List.of();

    public MesasPanel(ApiClient api, Runnable alCambiar) {
        super(new BorderLayout(0, 12));
        this.api = api;
        this.alCambiar = alCambiar;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiTheme.FONDO);

        JLabel titulo = new JLabel("Mesas");
        titulo.setFont(UiTheme.TITULO);

        grilla.setBackground(UiTheme.FONDO);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UiTheme.FONDO);
        wrapper.add(grilla, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UiTheme.FONDO);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(titulo, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    /** El catálogo que se va a poder pedir dentro de cada mesa (se comparte con Vender). */
    public void setProductos(List<Producto> productos) {
        this.productos = productos;
    }

    public void setMesas(List<Mesa> mesas) {
        grilla.removeAll();
        for (Mesa mesa : mesas) {
            grilla.add(crearTarjetaMesa(mesa));
        }
        if (mesas.isEmpty()) {
            JLabel vacio = new JLabel("Todavía no hay mesas cargadas - agregalas desde Admin.");
            vacio.setForeground(UiTheme.MUTED);
            grilla.add(vacio);
        }
        grilla.revalidate();
        grilla.repaint();
    }

    private JComponent crearTarjetaMesa(Mesa mesa) {
        RoundedPanel tarjeta = new RoundedPanel(new BorderLayout(), 14);
        tarjeta.setPreferredSize(new Dimension(160, 110));
        tarjeta.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        tarjeta.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        boolean ocupada = mesa.ocupada();
        tarjeta.setColorBorde(ocupada ? UiTheme.ACENTO : UiTheme.EXITO);

        JLabel nombre = new JLabel(mesa.nombre);
        nombre.setFont(UiTheme.SUBTITULO);

        JLabel estado = new JLabel(ocupada ? "Ocupada" : "Libre");
        estado.setFont(UiTheme.TEXTO_BASE);
        estado.setForeground(ocupada ? UiTheme.ACENTO_OSCURO : UiTheme.EXITO_OSCURO);

        JLabel total = new JLabel(ocupada ? UiTheme.moneda(mesa.totalActual) : " ");
        total.setFont(UiTheme.TEXTO_NEGRITA);

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.add(nombre);
        centro.add(Box.createVerticalStrut(6));
        centro.add(estado);
        centro.add(Box.createVerticalStrut(6));
        centro.add(total);

        tarjeta.add(centro, BorderLayout.CENTER);

        tarjeta.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                tarjeta.setColorFondo(new Color(0xF7, 0xF8, 0xFA));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                tarjeta.setColorFondo(UiTheme.TARJETA);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                abrirMesa(mesa);
            }
        });

        return tarjeta;
    }

    private void abrirMesa(Mesa mesa) {
        try {
            Mesa actualizada = mesa.ocupada() ? mesa : api.abrirMesa(mesa.id);
            Window ventana = SwingUtilities.getWindowAncestor(this);
            CuentaMesaDialog dialogo = new CuentaMesaDialog(ventana, api, actualizada, productos, alCambiar);
            // CuentaMesaDialog dispara alCambiar al cerrarse (dispose()), así
            // que acá no hace falta refrescar de nuevo.
            dialogo.setVisible(true);
        } catch (Exception ex) {
            Toast.error(this, "No se pudo abrir la mesa: " + ex.getMessage());
        }
    }
}
