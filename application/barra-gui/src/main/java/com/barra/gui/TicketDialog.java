package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.List;

/**
 * El ticket de una mesa recién cerrada: todo lo que se pidió, ronda por
 * ronda, con el total final - listo para mandar a imprimir o simplemente
 * para que el mozo lo lea y cobre.
 */
public class TicketDialog extends JDialog {

    private static final Font FUENTE_TICKET = new Font(Font.MONOSPACED, Font.PLAIN, 13);
    private final List<String> lineas;

    public TicketDialog(Window parent, ApiClient api, Cuenta cuenta) {
        super(parent, "Ticket - " + cuenta.mesaNombre, ModalityType.APPLICATION_MODAL);

        String nombreLocal = "Mi local";
        try {
            nombreLocal = api.obtenerConfiguracion().nombreLocal;
        } catch (Exception ignored) {
            // Si falla, el ticket sale igual con el nombre por defecto.
        }
        this.lineas = generarLineas(cuenta, nombreLocal);

        setSize(380, 560);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(UiTheme.FONDO);
        setLayout(new BorderLayout(0, 12));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel papel = new JPanel();
        papel.setBackground(UiTheme.TARJETA);
        papel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDE),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        papel.setLayout(new BoxLayout(papel, BoxLayout.Y_AXIS));
        for (String linea : lineas) {
            JLabel l = new JLabel(linea.isEmpty() ? " " : linea);
            l.setFont(FUENTE_TICKET);
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            papel.add(l);
        }

        JScrollPane scroll = new JScrollPane(papel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UiTheme.TARJETA);

        RoundButton imprimirBtn = new RoundButton("Imprimir", UiTheme.ACENTO, UiTheme.ACENTO_OSCURO);
        RoundButton cerrarBtn = new RoundButton("Cerrar", UiTheme.MUTED, UiTheme.MUTED.darker());
        imprimirBtn.addActionListener(e -> imprimir());
        cerrarBtn.addActionListener(e -> dispose());

        JPanel botones = new JPanel();
        botones.setOpaque(false);
        botones.setLayout(new BoxLayout(botones, BoxLayout.X_AXIS));
        botones.add(cerrarBtn);
        botones.add(Box.createHorizontalGlue());
        botones.add(imprimirBtn);

        add(scroll, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
    }

    private static List<String> generarLineas(Cuenta cuenta, String nombreLocal) {
        List<String> l = new ArrayList<>();
        l.add(centrado(nombreLocal.toUpperCase(), 32));
        l.add(separador());
        l.add(cuenta.mesaNombre);
        l.add("Abierta: " + fechaLegible(cuenta.fechaApertura));
        l.add("Cerrada: " + fechaLegible(cuenta.fechaCierre));
        l.add(separador());
        for (Pedido pedido : cuenta.pedidos) {
            for (Pedido.Detalle d : pedido.detalles) {
                String izquierda = d.cantidad + "x " + d.nombreProducto;
                l.add(fila(izquierda, UiTheme.moneda(d.subtotal)));
            }
            if (pedido.nota != null && !pedido.nota.isBlank()) {
                l.add("  (" + pedido.nota + ")");
            }
        }
        l.add(separador());
        l.add(fila("TOTAL", UiTheme.moneda(cuenta.total)));
        l.add(separador());
        l.add(centrado("¡Gracias por venir!", 32));
        return l;
    }

    private static String separador() {
        return "-".repeat(32);
    }

    private static String centrado(String texto, int ancho) {
        if (texto.length() >= ancho) return texto;
        int espacios = (ancho - texto.length()) / 2;
        return " ".repeat(espacios) + texto;
    }

    private static String fila(String izquierda, String derecha) {
        int ancho = 32;
        int espacios = ancho - izquierda.length() - derecha.length();
        if (espacios < 1) espacios = 1;
        return izquierda + " ".repeat(espacios) + derecha;
    }

    private static String fechaLegible(String iso) {
        if (iso == null || iso.length() < 16) return "-";
        String fecha = iso.substring(0, 10);
        String hora = iso.substring(11, 16);
        String[] partes = fecha.split("-");
        if (partes.length != 3) return iso;
        return partes[2] + "/" + partes[1] + " " + hora;
    }

    private void imprimir() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(new TicketPrintable());
        if (job.printDialog()) {
            try {
                job.print();
                Toast.exito(this, "Enviado a la impresora");
            } catch (PrinterException ex) {
                Toast.error(this, "No se pudo imprimir: " + ex.getMessage());
            }
        }
    }

    /** Dibuja el mismo texto del ticket sobre la página, línea por línea. */
    private class TicketPrintable implements Printable {
        @Override
        public int print(Graphics g, PageFormat pf, int pageIndex) {
            int altoLinea = 16;
            int lineasPorPagina = (int) (pf.getImageableHeight() / altoLinea);
            int desde = pageIndex * lineasPorPagina;
            if (desde >= lineas.size()) return NO_SUCH_PAGE;

            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            g2.setFont(FUENTE_TICKET);

            int y = altoLinea;
            for (int i = desde; i < Math.min(desde + lineasPorPagina, lineas.size()); i++) {
                g2.drawString(lineas.get(i), 0, y);
                y += altoLinea;
            }
            return PAGE_EXISTS;
        }
    }
}
