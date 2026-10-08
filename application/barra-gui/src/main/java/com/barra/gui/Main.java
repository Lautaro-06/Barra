package com.barra.gui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            // Look & feel nativo del sistema operativo para que los diálogos,
            // campos de texto, etc. no se vean con la cara genérica de Swing.
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Si falla, seguimos con el look and feel por defecto de Swing.
        }

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
