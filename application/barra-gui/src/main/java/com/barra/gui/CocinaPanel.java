package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pantalla "Cocina": tablero tipo kanban con los pedidos en curso, para que
 * quien cocina vea de un vistazo qué falta preparar y qué ya está listo
 * para entregar - en vez de leer una tabla SQL con los ojos.
 */
public class CocinaPanel extends JPanel {

    private final ApiClient api;
    private final Runnable alCambiarEstado;

    private final JPanel columnaPreparacion = new JPanel();
    private final JPanel columnaListo = new JPanel();
    private final JPanel columnaEntregado = new JPanel();

    public CocinaPanel(ApiClient api, Runnable alCambiarEstado) {
        super(new GridLayout(1, 3, 16, 0));
        this.api = api;
        this.alCambiarEstado = alCambiarEstado;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiTheme.FONDO);

        add(construirColumna("En preparación", UiTheme.colorEstado("en_preparacion"), columnaPreparacion));
        add(construirColumna("Listo para entregar", UiTheme.colorEstado("listo"), columnaListo));
        add(construirColumna("Entregados (últimos)", UiTheme.colorEstado("entregado"), columnaEntregado));
    }

    private JComponent construirColumna(String titulo, Color indicador, JPanel contenedor) {
        contenedor.setLayout(new BoxLayout(contenedor, BoxLayout.Y_AXIS));
        contenedor.setOpaque(false);

        JLabel punto = new JLabel("●");
        punto.setForeground(indicador);

        JLabel label = new JLabel(titulo);
        label.setFont(UiTheme.SUBTITULO);

        JPanel encabezado = new JPanel();
        encabezado.setOpaque(false);
        encabezado.setLayout(new BoxLayout(encabezado, BoxLayout.X_AXIS));
        encabezado.setBorder(BorderFactory.createEmptyBorder(0, 4, 8, 0));
        encabezado.add(punto);
        encabezado.add(Box.createHorizontalStrut(6));
        encabezado.add(label);

        JScrollPane scroll = new JScrollPane(contenedor);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(UiTheme.FONDO);

        JPanel columna = new JPanel(new BorderLayout());
        columna.setOpaque(false);
        columna.add(encabezado, BorderLayout.NORTH);
        columna.add(scroll, BorderLayout.CENTER);
        return columna;
    }

    /** Redibuja el tablero completo con el estado actual de los pedidos (llamado por el polling). */
    public void setPedidos(List<Pedido> pedidos) {
        columnaPreparacion.removeAll();
        columnaListo.removeAll();
        columnaEntregado.removeAll();

        List<Pedido> enPreparacion = pedidos.stream()
                .filter(p -> "en_preparacion".equals(p.estado))
                .collect(Collectors.toList());
        List<Pedido> listos = pedidos.stream()
                .filter(p -> "listo".equals(p.estado))
                .collect(Collectors.toList());
        List<Pedido> entregados = pedidos.stream()
                .filter(p -> "entregado".equals(p.estado))
                .limit(10)
                .collect(Collectors.toList());

        if (enPreparacion.isEmpty()) columnaPreparacion.add(mensajeVacio("Nada en preparación"));
        for (Pedido p : enPreparacion) columnaPreparacion.add(crearTarjeta(p));

        if (listos.isEmpty()) columnaListo.add(mensajeVacio("Nada esperando entrega"));
        for (Pedido p : listos) columnaListo.add(crearTarjeta(p));

        if (entregados.isEmpty()) columnaEntregado.add(mensajeVacio("Todavía no se entregó nada"));
        for (Pedido p : entregados) columnaEntregado.add(crearTarjeta(p));

        revalidate();
        repaint();
    }

    private JLabel mensajeVacio(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(UiTheme.MUTED);
        l.setFont(UiTheme.TEXTO_BASE);
        l.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        return l;
    }

    private JComponent crearTarjeta(Pedido pedido) {
        RoundedPanel tarjeta = new RoundedPanel(new BorderLayout(0, 6), 12);
        tarjeta.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        tarjeta.setColorBorde(UiTheme.colorEstado(pedido.estado));

        String encabezadoTxt = "Pedido #" + pedido.id + " · " + horaCorta(pedido.fecha);
        if (pedido.mesaNombre != null) {
            encabezadoTxt = pedido.mesaNombre + " · " + horaCorta(pedido.fecha);
        }
        JLabel header = new JLabel(encabezadoTxt);
        header.setFont(UiTheme.TEXTO_NEGRITA);

        StringBuilder detalleTxt = new StringBuilder("<html>");
        if (pedido.detalles != null) {
            for (Pedido.Detalle d : pedido.detalles) {
                detalleTxt.append(d.cantidad).append("x ").append(UiTheme.escapeHtml(d.nombreProducto)).append("<br>");
            }
        }
        detalleTxt.append("</html>");
        JLabel detalle = new JLabel(detalleTxt.toString());
        detalle.setFont(UiTheme.TEXTO_BASE);

        JLabel total = new JLabel(UiTheme.moneda(pedido.total));
        total.setFont(UiTheme.TEXTO_NEGRITA);
        total.setForeground(UiTheme.ACENTO_OSCURO);

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.add(detalle);
        if (pedido.nota != null && !pedido.nota.isBlank()) {
            JLabel nota = new JLabel("<html><i>" + UiTheme.escapeHtml(pedido.nota) + "</i></html>");
            nota.setFont(UiTheme.TEXTO_BASE);
            nota.setForeground(UiTheme.MUTED);
            centro.add(nota);
        }
        centro.add(Box.createVerticalStrut(4));
        centro.add(total);

        tarjeta.add(header, BorderLayout.NORTH);
        tarjeta.add(centro, BorderLayout.CENTER);

        JComponent accion = crearBotonAccion(pedido);
        if (accion != null) tarjeta.add(accion, BorderLayout.SOUTH);

        JPanel envoltorio = new JPanel(new BorderLayout());
        envoltorio.setOpaque(false);
        envoltorio.setAlignmentX(Component.LEFT_ALIGNMENT);
        envoltorio.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        envoltorio.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        envoltorio.add(tarjeta, BorderLayout.CENTER);
        return envoltorio;
    }

    private JComponent crearBotonAccion(Pedido pedido) {
        RoundButton btn;
        if ("en_preparacion".equals(pedido.estado)) {
            btn = new RoundButton("Marcar listo", UiTheme.INFO, UiTheme.INFO.darker());
            btn.addActionListener(e -> cambiarEstado(pedido.id, "listo"));
        } else if ("listo".equals(pedido.estado)) {
            btn = new RoundButton("Entregar", UiTheme.EXITO, UiTheme.EXITO_OSCURO);
            btn.addActionListener(e -> cambiarEstado(pedido.id, "entregado"));
        } else {
            return null;
        }
        JPanel envoltorio = new JPanel(new BorderLayout());
        envoltorio.setOpaque(false);
        envoltorio.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        envoltorio.add(btn, BorderLayout.CENTER);
        return envoltorio;
    }

    private void cambiarEstado(int pedidoId, String nuevoEstado) {
        try {
            api.cambiarEstado(pedidoId, nuevoEstado);
            alCambiarEstado.run();
        } catch (Exception ex) {
            Toast.error(this, "No se pudo actualizar el pedido: " + ex.getMessage());
        }
    }

    /** La fecha viene del backend como "2026-09-04T14:32:00"; acá nos quedamos solo con "14:32". */
    private static String horaCorta(String fechaIso) {
        int tIdx = fechaIso.indexOf('T');
        if (tIdx >= 0 && fechaIso.length() >= tIdx + 6) {
            return fechaIso.substring(tIdx + 1, tIdx + 6);
        }
        return fechaIso;
    }
}
