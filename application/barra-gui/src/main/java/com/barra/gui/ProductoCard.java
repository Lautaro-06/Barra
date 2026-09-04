package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Tarjeta de producto reutilizada por "Vender" y por la cuenta de una mesa:
 * muestra nombre/precio/stock, se ve apagada si no se puede vender (sin
 * stock o marcado "no disponible" desde Admin), y al tocarla dispara la
 * acción que le pases.
 */
public final class ProductoCard {

    private ProductoCard() {}

    public static JComponent crear(Producto p, Consumer<Producto> alTocar) {
        RoundedPanel tarjeta = new RoundedPanel(new BorderLayout(), 14);
        tarjeta.setPreferredSize(new Dimension(150, 92));
        tarjeta.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        boolean vendible = p.disponible && p.stock > 0;

        JLabel nombre = new JLabel("<html><body style='width:120px'>" + UiTheme.escapeHtml(p.nombre) + "</body></html>");
        nombre.setFont(UiTheme.TEXTO_NEGRITA);
        nombre.setForeground(UiTheme.TEXTO);
        nombre.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel precio = new JLabel(UiTheme.moneda(p.precio));
        precio.setFont(UiTheme.SUBTITULO);
        precio.setForeground(UiTheme.ACENTO_OSCURO);
        precio.setAlignmentX(Component.LEFT_ALIGNMENT);

        String estadoTxt;
        if (!p.disponible) {
            estadoTxt = "No disponible";
        } else if (p.stock <= 0) {
            estadoTxt = "Sin stock";
        } else {
            estadoTxt = "Stock: " + p.stock;
        }
        JLabel estadoLbl = new JLabel(estadoTxt);
        estadoLbl.setFont(UiTheme.TEXTO_BASE.deriveFont(11f));
        estadoLbl.setForeground(!vendible ? UiTheme.PELIGRO : (p.stock < 5 ? UiTheme.ACENTO_OSCURO : UiTheme.MUTED));
        estadoLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.add(nombre);
        textos.add(Box.createVerticalStrut(6));
        textos.add(precio);
        textos.add(estadoLbl);

        tarjeta.add(textos, BorderLayout.CENTER);

        if (!vendible) {
            tarjeta.setColorFondo(new Color(0xF2, 0xF2, 0xF4));
        } else {
            tarjeta.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            tarjeta.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    tarjeta.setColorFondo(new Color(0xFF, 0xF1, 0xE8));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    tarjeta.setColorFondo(UiTheme.TARJETA);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    alTocar.accept(p);
                }
            });
        }
        return tarjeta;
    }
}
