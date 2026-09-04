package com.barra.gui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/**
 * Panel con esquinas redondeadas y color de fondo propio: sirve de base
 * para las "tarjetas" de producto y de pedido, para que la GUI no se vea
 * como una ventana Swing por defecto de hace veinte años.
 */
public class RoundedPanel extends JPanel {

    private final int arco;
    private Color colorFondo = UiTheme.TARJETA;
    private Color colorBorde = UiTheme.BORDE;

    public RoundedPanel(LayoutManager layout, int arco) {
        super(layout);
        this.arco = arco;
        setOpaque(false);
    }

    public void setColorFondo(Color c) {
        this.colorFondo = c;
        repaint();
    }

    public void setColorBorde(Color c) {
        this.colorBorde = c;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(colorFondo);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arco, arco);
        g2.setColor(colorBorde);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arco, arco);
        g2.dispose();
        super.paintComponent(g);
    }
}
