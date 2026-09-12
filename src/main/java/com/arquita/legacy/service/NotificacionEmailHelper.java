package com.arquita.legacy.service;

import com.arquita.legacy.util.ArquitaUtils;
import com.arquita.legacy.util.Constantes;

public class NotificacionEmailHelper {

    private NotificacionEmailHelper() {
    }

    public static void enviarNotificacionFactura(String destinatarioCuit, String asunto, String cuerpo) {
        ArquitaUtils.log("Conectando a " + Constantes.SMTP_HOST + " como " + Constantes.SMTP_USER + " ...");
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        ArquitaUtils.log("Email a contribuyente " + destinatarioCuit + " - asunto: " + asunto);
        ArquitaUtils.log("Cuerpo: " + cuerpo);
    }
}
