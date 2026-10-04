package com.barra.gui;

/** Refleja el ConfiguracionOut del backend Python (app/models.py). */
public class Configuracion {
    public final String nombreLocal;
    public final int umbralStockGlobal;
    public final boolean emailHabilitado;
    /** null = no está cargado (se usa el email del dueño, ver emailDestinoEfectivo). */
    public final String emailDestino;
    /** A dónde se mandan realmente los mails: emailDestino o, si falta, el email del dueño. */
    public final String emailDestinoEfectivo;
    public final String smtpHost;
    public final int smtpPort;
    public final String smtpUsuario;
    /** La contraseña nunca viaja al cliente: solo se sabe si ya hay una guardada. */
    public final boolean smtpPasswordConfigurada;
    public final boolean resumenDiarioHabilitado;
    public final String resumenDiarioHora;

    public Configuracion(String nombreLocal, int umbralStockGlobal, boolean emailHabilitado,
                         String emailDestino, String emailDestinoEfectivo, String smtpHost,
                         int smtpPort, String smtpUsuario, boolean smtpPasswordConfigurada,
                         boolean resumenDiarioHabilitado, String resumenDiarioHora) {
        this.nombreLocal = nombreLocal;
        this.umbralStockGlobal = umbralStockGlobal;
        this.emailHabilitado = emailHabilitado;
        this.emailDestino = emailDestino;
        this.emailDestinoEfectivo = emailDestinoEfectivo;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.smtpUsuario = smtpUsuario;
        this.smtpPasswordConfigurada = smtpPasswordConfigurada;
        this.resumenDiarioHabilitado = resumenDiarioHabilitado;
        this.resumenDiarioHora = resumenDiarioHora;
    }
}