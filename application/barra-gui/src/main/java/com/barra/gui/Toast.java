package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Window;

/**
 * Notificación flotante no bloqueante ("toast"): aparece arriba a la
 * derecha de la ventana y se esfuma sola a los pocos segundos. Reemplaza
 * los JOptionPane para avisos de éxito/error, que rompían por completo la
 * estética del resto de la GUI (ícono de café, botones grises de Swing).
 */
public final class Toast {

    private Toast() {}

    public static void exito(Component ancla, String mensaje) {
        mostrar(ancla, mensaje, UiTheme.EXITO);
    }

    public static void error(Component ancla, String mensaje) {
        mostrar(ancla, mensaje, UiTheme.PELIGRO);
    }

    private static void mostrar(Component ancla, String mensaje, Color color) {
        Window ventana = SwingUtilities.getWindowAncestor(ancla);
        if (ventana == null) return;

        JLayeredPane capas = SwingUtilities.getRootPane(ventana).getLayeredPane();

        RoundedPanel toast = new RoundedPanel(new BorderLayout(), 10);
        toast.setColorFondo(color);
        toast.setColorBorde(color.darker());
        toast.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel label = new JLabel(mensaje);
        label.setForeground(Color.WHITE);
        label.setFont(UiTheme.TEXTO_NEGRITA);
        toast.add(label, BorderLayout.CENTER);

        FontMetrics fm = label.getFontMetrics(label.getFont());
        int ancho = Math.min(360, fm.stringWidth(mensaje) + 70);
        int alto = 44;
        toast.setBounds(ventana.getWidth() - ancho - 24, 24, ancho, alto);

        capas.add(toast, JLayeredPane.POPUP_LAYER);
        capas.repaint(toast.getBounds());

        Timer ocultar = new Timer(3000, e -> {
            capas.remove(toast);
            capas.repaint();
        });
        ocultar.setRepeats(false);
        ocultar.start();
    }
}
