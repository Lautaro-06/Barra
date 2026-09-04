package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Botón "pill" con color plano e igual look en cualquier sistema operativo.
 * Swing por defecto ignora setBackground() en varios Look&Feel nativos
 * (Windows, GTK), así que este botón pinta su propio fondo redondeado.
 */
public class RoundButton extends JButton {

    private final Color base;
    private final Color hover;
    private Color actual;

    public RoundButton(String texto, Color base, Color hover) {
        super(texto);
        this.base = base;
        this.hover = hover;
        this.actual = base;

        setForeground(Color.WHITE);
        setFont(UiTheme.TEXTO_NEGRITA);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                actual = hover;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                actual = base;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isEnabled() ? actual : UiTheme.MUTED);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
        g2.dispose();
        super.paintComponent(g);
    }
}
