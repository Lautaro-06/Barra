package com.barra.gui;

import java.awt.Color;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * UiTheme
 *
 * Paleta de colores, tipografías y pequeños helpers compartidos por toda la
 * GUI, para que las tres pantallas (Vender, Cocina, Catálogo) tengan una
 * misma cara en vez de que cada una use lo que Swing trae por defecto.
 */
public final class UiTheme {

    private UiTheme() {}

    public static final Color FONDO = new Color(0xF4, 0xF5, 0xF7);
    public static final Color SIDEBAR = new Color(0x21, 0x23, 0x2E);
    public static final Color TARJETA = Color.WHITE;
    public static final Color BORDE = new Color(0xE2, 0xE4, 0xE9);

    public static final Color ACENTO = new Color(0xFF, 0x6B, 0x35);        // naranja: acciones principales
    public static final Color ACENTO_OSCURO = new Color(0xE0, 0x55, 0x22);
    public static final Color EXITO = new Color(0x2E, 0xA0, 0x5C);         // verde: confirmar / entregar
    public static final Color EXITO_OSCURO = new Color(0x24, 0x83, 0x49);
    public static final Color INFO = new Color(0x33, 0x7A, 0xE0);          // azul: pedido "listo"
    public static final Color PELIGRO = new Color(0xD9, 0x3B, 0x3B);       // rojo: sin conexión / sin stock
    public static final Color MUTED = new Color(0x8A, 0x8F, 0x9C);

    public static final Color TEXTO = new Color(0x22, 0x24, 0x2B);

    public static final Font TITULO = new Font("SansSerif", Font.BOLD, 20);
    public static final Font SUBTITULO = new Font("SansSerif", Font.BOLD, 15);
    public static final Font TEXTO_BASE = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font TEXTO_NEGRITA = new Font("SansSerif", Font.BOLD, 13);
    public static final Font TOTAL = new Font("SansSerif", Font.BOLD, 22);

    private static final NumberFormat FORMATO_MONEDA =
            NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    /** Formatea un precio como "$ 1.800,00" en vez de un double pelado. */
    public static String moneda(double valor) {
        return FORMATO_MONEDA.format(valor);
    }

    /** Color asociado al estado de un pedido, usado como acento en las tarjetas de Cocina. */
    public static Color colorEstado(String estado) {
        return switch (estado) {
            case "listo" -> INFO;
            case "entregado" -> MUTED;
            default -> ACENTO; // en_preparacion
        };
    }

    public static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
