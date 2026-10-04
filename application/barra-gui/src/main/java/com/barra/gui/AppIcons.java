package com.barra.gui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * AppIcons
 *
 * Íconos propios dibujados con Java2D (siluetas vectoriales, nada de
 * emojis ni imágenes externas), para que la barra lateral y la ventana
 * tengan una identidad visual consistente en cualquier sistema operativo
 * y a cualquier resolución de pantalla.
 */
public final class AppIcons {

    private AppIcons() {}

    public static Icon vender(Color color) {
        return vender(color, 20);
    }

    public static Icon vender(Color color, int size) {
        return new VectorIcon(size, color) {
            @Override
            Shape construir(double s) {
                // Un ticket/recibo: cuadrado con el borde inferior en zigzag
                // (como el papel cortado de una impresora fiscal) y tres
                // líneas de "texto" recortadas adentro.
                double x0 = 0.20, x1 = 0.80, yTop = 0.14, yPico = 0.68, yValle = 0.82;
                Path2D ticket = new Path2D.Double();
                ticket.moveTo(x0 * s, yTop * s);
                ticket.lineTo(x1 * s, yTop * s);
                ticket.lineTo(x1 * s, yPico * s);

                int dientes = 4;
                double ancho = (x1 - x0) / dientes;
                for (int i = 0; i < dientes; i++) {
                    double xValle = (x1 - ancho * (i + 0.5)) * s;
                    double xPico = (x1 - ancho * (i + 1)) * s;
                    ticket.lineTo(xValle, yValle * s);
                    ticket.lineTo(xPico, yPico * s);
                }
                ticket.closePath();

                Area area = new Area(ticket);
                double margen = x0 + 0.09;
                double anchoLinea = (x1 - x0) - 0.18;
                area.subtract(new Area(new Rectangle2D.Double(margen * s, 0.28 * s, anchoLinea * s, 0.06 * s)));
                area.subtract(new Area(new Rectangle2D.Double(margen * s, 0.40 * s, anchoLinea * s, 0.06 * s)));
                area.subtract(new Area(new Rectangle2D.Double(margen * s, 0.52 * s, anchoLinea * s, 0.06 * s)));
                return area;
            }
        };
    }

    public static Icon cocina(Color color) {
        return cocina(color, 20);
    }

    public static Icon cocina(Color color, int size) {
        return new VectorIcon(size, color) {
            @Override
            Shape construir(double s) {
                // Gorro de cocinero: banda inferior + tres bultos superpuestos arriba.
                Area area = new Area(new RoundRectangle2D.Double(0.14 * s, 0.74 * s, 0.72 * s, 0.18 * s, 0.08 * s, 0.08 * s));
                area.add(new Area(new Ellipse2D.Double(0.14 * s, 0.36 * s, 0.36 * s, 0.36 * s)));
                area.add(new Area(new Ellipse2D.Double(0.36 * s, 0.14 * s, 0.36 * s, 0.44 * s)));
                area.add(new Area(new Ellipse2D.Double(0.58 * s, 0.36 * s, 0.36 * s, 0.36 * s)));
                area.add(new Area(new RoundRectangle2D.Double(0.20 * s, 0.62 * s, 0.60 * s, 0.16 * s, 0.06 * s, 0.06 * s)));
                return area;
            }
        };
    }

    public static Icon catalogo(Color color) {
        return catalogo(color, 20);
    }

    public static Icon catalogo(Color color, int size) {
        return new VectorIcon(size, color) {
            @Override
            Shape construir(double s) {
                Area area = new Area(new RoundRectangle2D.Double(0.17 * s, 0.20 * s, 0.66 * s, 0.60 * s, 0.15 * s, 0.15 * s));
                area.subtract(new Area(new Rectangle2D.Double(0.17 * s, 0.43 * s, 0.66 * s, 0.07 * s)));
                return area;
            }
        };
    }

    public static Icon mesas(Color color) {
        return mesas(color, 20);
    }

    public static Icon mesas(Color color, int size) {
        return new VectorIcon(size, color) {
            @Override
            Shape construir(double s) {
                // Mesa redonda vista desde arriba, con una silla (cuadrada)
                // en cada esquina - la lectura clásica de "plano de salón".
                Area area = new Area(new Ellipse2D.Double(0.28 * s, 0.28 * s, 0.44 * s, 0.44 * s));
                double lado = 0.20;
                double arco = 0.05;
                area.add(new Area(new RoundRectangle2D.Double(0.02 * s, 0.02 * s, lado * s, lado * s, arco * s, arco * s)));
                area.add(new Area(new RoundRectangle2D.Double(0.78 * s, 0.02 * s, lado * s, lado * s, arco * s, arco * s)));
                area.add(new Area(new RoundRectangle2D.Double(0.02 * s, 0.78 * s, lado * s, lado * s, arco * s, arco * s)));
                area.add(new Area(new RoundRectangle2D.Double(0.78 * s, 0.78 * s, lado * s, lado * s, arco * s, arco * s)));
                return area;
            }
        };
    }

    public static Icon admin(Color color) {
        return admin(color, 20);
    }

    public static Icon admin(Color color, int size) {
        return new VectorIcon(size, color) {
            @Override
            Shape construir(double s) {
                // Tres controles deslizantes (sliders), clásico ícono de "configuración".
                Area area = new Area();
                double[] filas = {0.22, 0.5, 0.78};
                double[] manijas = {0.62, 0.32, 0.72};
                double grosorLinea = 0.06;
                double radioManija = 0.13;
                for (int i = 0; i < filas.length; i++) {
                    double y = filas[i] * s;
                    area.add(new Area(new RoundRectangle2D.Double(0.12 * s, y - grosorLinea * s / 2, 0.76 * s, grosorLinea * s, grosorLinea * s, grosorLinea * s)));
                    double cx = manijas[i] * s;
                    area.add(new Area(new Ellipse2D.Double(cx - radioManija * s / 2, y - radioManija * s / 2, radioManija * s, radioManija * s)));
                }
                return area;
            }
        };
    }

    /** Logo de la marca: cuadrado redondeado con una "B" - se usa en el sidebar y como ícono de ventana. */
    public static Icon marca(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);

                g2.setColor(UiTheme.ACENTO);
                double arco = size * 0.28;
                g2.fill(new RoundRectangle2D.Double(0, 0, size, size, arco, arco));

                g2.setColor(Color.WHITE);
                Font fuente = UiTheme.TITULO.deriveFont(Font.BOLD, size * 0.55f);
                g2.setFont(fuente);
                FontMetrics fm = g2.getFontMetrics();
                String texto = "B";
                int tx = (size - fm.stringWidth(texto)) / 2;
                int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(texto, tx, ty);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    /** La misma marca, renderizada a una imagen para usar como ícono de la ventana/taskbar. */
    public static BufferedImage marcaComoImagen(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        marca(size).paintIcon(null, g2, 0, 0);
        g2.dispose();
        return img;
    }

    /** Ícono base: dibuja una silueta sólida (y opcionalmente un trazo extra) con antialiasing. */
    private abstract static class VectorIcon implements Icon {
        private final int size;
        private final Color color;

        VectorIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        abstract Shape construir(double size);

        void trazoExtra(Graphics2D g2, double size) {
            // los íconos que necesitan un trazo además de la silueta (ej. el
            // asa de la bolsa de "Vender") lo agregan acá.
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            g2.setColor(color);
            g2.fill(construir(size));
            g2.setStroke(new BasicStroke(Math.max(1.4f, size * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            trazoExtra(g2, size);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
