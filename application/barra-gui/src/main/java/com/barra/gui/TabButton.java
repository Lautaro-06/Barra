package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Botón de una sub-navegación horizontal sobre fondo claro (ver AdminPanel):
 * texto apagado en reposo, píldora naranja de fondo cuando está seleccionado.
 * Es el equivalente de NavButton pero para usar fuera del sidebar oscuro.
 */
public class TabButton extends JButton {

    private boolean seleccionado = false;

    public TabButton(String texto) {
        super(texto);
        setFont(UiTheme.TEXTO_NEGRITA);
        setForeground(UiTheme.MUTED);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    }

    public void setSeleccionado(boolean seleccionado) {
        this.seleccionado = seleccionado;
        setForeground(seleccionado ? Color.WHITE : UiTheme.MUTED);
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
