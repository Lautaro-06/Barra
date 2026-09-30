package com.barra.gui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Box;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Admin > Configuración: lo que hace que la misma app sirva para cualquier
 * local - nombre, datos del dueño y la configuración de email (alertas de
 * stock y resumen diario).
 *
 * Los datos se cargan en el formulario una sola vez (y de nuevo después de
 * cada guardado): MainWindow refresca cada 4s, y si se pisaran los campos
 * en cada refresco se borraría lo que el usuario está escribiendo.
 */
public class AdminConfiguracionPanel extends JPanel {

    private final ApiClient api;
    private final Runnable alCambiar;
    private final JTextField nombreLocalField = campoTexto();

    private final JTextField nombreDuenoField = campoTexto();
    private final JTextField emailDuenoField = campoTexto();
    private final JTextField telefonoField = campoTexto();

    private final JTextField umbralGlobalField = campoTexto();
    private final JCheckBox emailHabilitadoCheck = new JCheckBox("Enviar alertas de stock bajo por email");
    private final JTextField emailDestinoField = campoTexto();
    private final JLabel emailDestinoAyuda = ayuda("");
    private final JTextField smtpHostField = campoTexto();
    private final JTextField smtpPortField = campoTexto();
    private final JTextField smtpUsuarioField = campoTexto();
    private final JPasswordField smtpPasswordField = new JPasswordField();
    private final JLabel smtpPasswordAyuda = ayuda("");
    private final JCheckBox resumenHabilitadoCheck = new JCheckBox("Enviar resumen diario de ventas por email");
    private final JTextField resumenHoraField = campoTexto();

    private final RoundButton probarEmailBtn = new RoundButton("Enviar email de prueba", UiTheme.INFO,
            UiTheme.INFO.darker());
    private final RoundButton enviarResumenBtn = new RoundButton("Enviar resumen ahora", UiTheme.INFO,
            UiTheme.INFO.darker());
    private boolean configuracionCargada = false;
    private boolean adminCargado = false;

    public AdminConfiguracionPanel(ApiClient api, Runnable alCambiar) {
        super();
        this.api = api;
        this.alCambiar = alCambiar;
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(4, 0, 16, 0));

        // ---- Local ----

        form.add(titulo("Configuración del local", 0));
        form.add(etiqueta("Nombre del local", 16));
        form.add(nombreLocalField);
        form.add(ayuda("Aparece en el sidebar, en el título de la ventana y en el ticket."));
        form.add(boton("Guardar", this::guardar));

        // ---- Datos del dueño (para alertas y resumen diario por email) ----

        form.add(titulo("Datos del dueño", 24));
        form.add(etiqueta("Nombre del dueño", 16));
        form.add(nombreDuenoField);
        form.add(etiqueta("Email del dueño", 12));
        form.add(emailDuenoField);
        form.add(etiqueta("Teléfono (opcional)", 12));
        form.add(telefonoField);
        form.add(ayuda("Si no cargás un email de destino abajo, las alertas y el resumen diario llegan a este email."));
        form.add(boton("Guardar datos del dueño", this::guardarDueno));

        // ---- Stock y emails ----

        form.add(titulo("Alertas y resumen por email", 24));
        form.add(etiqueta("Umbral de stock bajo (global)", 16));
        umbralGlobalField.setMaximumSize(new Dimension(120, 34));
        form.add(umbralGlobalField);
        form.add(ayuda("Se usa para los productos que no tienen un umbral propio."));

        form.add(checkBox(emailHabilitadoCheck));
        form.add(checkBox(resumenHabilitadoCheck));
        form.add(etiqueta("Hora del resumen diario (HH:MM)", 8));
        resumenHoraField.setMaximumSize(new Dimension(120, 34));
        form.add(resumenHoraField);

        form.add(etiqueta("Email de destino (opcional)", 16));
        form.add(emailDestinoField);
        form.add(emailDestinoAyuda);

        form.add(etiqueta("Servidor SMTP", 4));
        form.add(smtpHostField);
        form.add(ayuda("Ej: smtp.gmail.com"));
        form.add(etiqueta("Puerto SMTP", 4));
        smtpPortField.setMaximumSize(new Dimension(120, 34));
        form.add(smtpPortField);
        form.add(ayuda("587 (STARTTLS) o 465 (SSL)."));
        form.add(etiqueta("Usuario SMTP", 4));
        form.add(smtpUsuarioField);
        form.add(etiqueta("Contraseña SMTP", 12));
        estilizar(smtpPasswordField);
        form.add(smtpPasswordField);
        form.add(smtpPasswordAyuda);
        form.add(boton("Guardar configuración de email", this::guardarEmail));
        form.add(ayuda("Guardá primero: la prueba usa la configuración guardada."));
        probarEmailBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        probarEmailBtn.addActionListener(e -> probarEmail());
        form.add(probarEmailBtn);
        form.add(Box.createVerticalStrut(8));
        enviarResumenBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        enviarResumenBtn.addActionListener(e -> enviarResumen());
        form.add(enviarResumenBtn);
        form.add(ayuda("Manda ya el resumen de las últimas 24hs (el automático se sigue mandando a su hora)."));

        JScrollPane scroll = new JScrollPane(form);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    public void setConfiguracion(Configuracion config) {
        if (!nombreLocalField.getText().equals(config.nombreLocal) && !nombreLocalField.isFocusOwner()) {
            nombreLocalField.setText(config.nombreLocal);
        }
        if (!configuracionCargada) {
            cargarConfiguracionEmail(config);
            configuracionCargada = true;
        }
    }

    public void setAdmin(Admin admin) {
        if (adminCargado)
            return;
        adminCargado = true;
        nombreDuenoField.setText(admin.nombreDueno);
        emailDuenoField.setText(admin.emailDueno);
        telefonoField.setText(admin.telefono == null ? "" : admin.telefono);
    }

    /** MainWindow lo usa para pedir /admin una sola vez, no en cada refresco. */
    public boolean necesitaAdmin() {
        return !adminCargado;
    }

    private void cargarConfiguracionEmail(Configuracion config) {
        umbralGlobalField.setText(String.valueOf(config.umbralStockGlobal));
        emailHabilitadoCheck.setSelected(config.emailHabilitado);
        resumenHabilitadoCheck.setSelected(config.resumenDiarioHabilitado);
        resumenHoraField.setText(config.resumenDiarioHora);
        emailDestinoField.setText(config.emailDestino == null ? "" : config.emailDestino);
        emailDestinoAyuda.setText(config.emailDestinoEfectivo == null
                ? "Sin destino: cargá uno acá o el email del dueño."
                : "Los mails van a llegar a: " + config.emailDestinoEfectivo);
        smtpHostField.setText(config.smtpHost == null ? "" : config.smtpHost);
        smtpPortField.setText(String.valueOf(config.smtpPort));
        smtpUsuarioField.setText(config.smtpUsuario == null ? "" : config.smtpUsuario);
        smtpPasswordField.setText("");
        smtpPasswordAyuda.setText(config.smtpPasswordConfigurada
                ? "Ya hay una contraseña guardada. Dejalo vacío para no cambiarla."
                : "Todavía no hay contraseña guardada.");
    }

    private void guardar() {
        String nombre = nombreLocalField.getText().trim();
        if (nombre.isEmpty()) {
            Toast.error(this, "El nombre del local no puede estar vacío");
            return;
        }
        try {
            api.actualizarConfiguracion(nombre);
            alCambiar.run();
            Toast.exito(this, "Configuración guardada");
        } catch (Exception ex) {
            Toast.error(this, "No se pudo guardar: " + ex.getMessage());
        }
    }

    private void guardarDueno() {
        String nombre = nombreDuenoField.getText().trim();
        String email = emailDuenoField.getText().trim();
        String telefono = telefonoField.getText().trim();
        if (nombre.isEmpty()) {
            Toast.error(this, "El nombre del dueño no puede estar vacío");
            return;
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            Toast.error(this, "El email del dueño no es válido");
            return;
        }
        try {
            api.actualizarAdmin(nombre, email, telefono.isEmpty() ? null : telefono);
            // El destino efectivo puede haber cambiado (si no hay email_destino propio).
            cargarConfiguracionEmail(api.obtenerConfiguracion());
            Toast.exito(this, "Datos del dueño guardados");
        } catch (Exception ex) {
            Toast.error(this, "No se pudo guardar: " + ex.getMessage());
        }
    }

    private void guardarEmail() {
        int umbral;
        int puerto;
        try {
            umbral = Integer.parseInt(umbralGlobalField.getText().trim());
            puerto = Integer.parseInt(smtpPortField.getText().trim());
        } catch (NumberFormatException ex) {
            Toast.error(this, "El umbral y el puerto tienen que ser números");
            return;
        }
        if (umbral < 0) {
            Toast.error(this, "El umbral no puede ser negativo");
            return;
        }
        String hora = resumenHoraField.getText().trim();
        if (!hora.matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
            Toast.error(this, "La hora del resumen tiene que tener formato HH:MM (ej: 23:00)");
            return;
        }

        Map<String, Object> cambios = new LinkedHashMap<>();
        cambios.put("umbral_stock_global", umbral);
        cambios.put("email_habilitado", emailHabilitadoCheck.isSelected());
        cambios.put("resumen_diario_habilitado", resumenHabilitadoCheck.isSelected());
        cambios.put("resumen_diario_hora", hora);
        // Vacío = el backend lo guarda como null (se usa el email del dueño).
        cambios.put("email_destino", emailDestinoField.getText().trim());
        cambios.put("smtp_host", smtpHostField.getText().trim());
        cambios.put("smtp_port", puerto);
        cambios.put("smtp_usuario", smtpUsuarioField.getText().trim());
        String password = new String(smtpPasswordField.getPassword());
        if (!password.isEmpty()) {
            // Solo se manda si se escribió una nueva: omitirla deja la guardada.
            cambios.put("smtp_password", password);
        }

        try {
            cargarConfiguracionEmail(api.actualizarConfiguracion(cambios));
            alCambiar.run();
            Toast.exito(this, "Configuración de email guardada");
        } catch (Exception ex) {
            Toast.error(this, "No se pudo guardar: " + ex.getMessage());
        }
    }

        private void probarEmail() {
        enviarEnSegundoPlano(probarEmailBtn, api::probarEmail, "Email de prueba enviado a ");
    }

    private void enviarResumen() {
        enviarEnSegundoPlano(enviarResumenBtn, api::enviarResumenDiario, "Resumen enviado a ");
    }

    /**
     * Corre un envío de email en un SwingWorker: hablar con el servidor
     * SMTP puede tardar varios segundos, y hacerlo en el hilo de Swing
     * congelaría toda la ventana mientras tanto. envio devuelve la
     * dirección a la que se mandó.
     */
    private void enviarEnSegundoPlano(RoundButton boton, Callable<String> envio, String mensajeExito) {
        String textoOriginal = boton.getText();
        boton.setEnabled(false);
        boton.setText("Enviando...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return envio.call();
            }

            @Override
            protected void done() {
                boton.setEnabled(true);
                boton.setText(textoOriginal);
                try {
                    Toast.exito(AdminConfiguracionPanel.this, mensajeExito + get());
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    Toast.error(AdminConfiguracionPanel.this, "No se pudo enviar: " + causa.getMessage());
                }
            }
        }.execute();
    }

    // ---------- Helpers de armado del formulario ----------

    private static JLabel titulo(String texto, int margenArriba) {
        JLabel label = new JLabel(texto);
        label.setFont(UiTheme.SUBTITULO);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(margenArriba, 0, 0, 0));
        return label;
    }

    private static JLabel etiqueta(String texto, int margenArriba) {
        JLabel label = new JLabel(texto);
        label.setFont(UiTheme.TEXTO_BASE.deriveFont(11f));
        label.setForeground(UiTheme.MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(margenArriba, 0, 4, 0));
        return label;
    }

    private static JLabel ayuda(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(UiTheme.TEXTO_BASE.deriveFont(11f));
        label.setForeground(UiTheme.MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(6, 0, 16, 0));
        return label;
    }

    private static JTextField campoTexto() {
        JTextField field = new JTextField();
        estilizar(field);
        return field;
    }

    private static void estilizar(JTextField field) {
        field.setFont(UiTheme.TEXTO_BASE);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(320, 34));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDE),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
    }

    private static JComponent checkBox(JCheckBox check) {
        check.setOpaque(false);
        check.setFont(UiTheme.TEXTO_BASE);
        check.setAlignmentX(Component.LEFT_ALIGNMENT);
        check.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        return check;
    }

    private static RoundButton boton(String texto, Runnable accion) {
        RoundButton btn = new RoundButton(texto, UiTheme.ACENTO, UiTheme.ACENTO_OSCURO);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addActionListener(e -> accion.run());
        return btn;
    }
}