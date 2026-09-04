package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Botón de la barra lateral: transparente en reposo, con un resaltado
 * redondeado cuando la pantalla correspondiente es la que está activa -
 * para que se sepa de un vistazo en cuál de las tres pantallas se está
 * parado, algo que la GUI no mostraba antes.
 */
public class NavButton extends JButton {

    private static final Color TEXTO_INACTIVO = new Color(0xC7, 0xC9, 0xD3);

    private boolean seleccionado = false;

    public NavButton(String texto, Icon icono) {
        super(texto, icono);
        setHorizontalAlignment(SwingConstants.LEFT);
        setIconTextGap(10);
        setForeground(TEXTO_INACTIVO);
        setFont(UiTheme.TEXTO_NEGRITA);
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
    }

    public void setSeleccionado(boolean seleccionado) {
        this.seleccionado = seleccionado;
        setForeground(seleccionado ? Color.WHITE : TEXTO_INACTIVO);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (seleccionado) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(UiTheme.ACENTO);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
