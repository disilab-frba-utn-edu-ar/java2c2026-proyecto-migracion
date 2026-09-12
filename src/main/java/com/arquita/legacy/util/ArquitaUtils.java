package com.arquita.legacy.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArquitaUtils {

    private static final Pattern PATRON_CUIT = Pattern.compile("^\\d{2}-?\\d{8}-?\\d{1}$");

    private ArquitaUtils() {
    }

    public static boolean esCuitValido(String cuit) {
        if (cuit == null) {
            return false;
        }
        String limpio = cuit.replace("-", "").trim();
        Matcher m = PATRON_CUIT.matcher(cuit);
        if (!m.matches() || limpio.length() != 11) {
            return false;
        }
        int[] multiplicadores = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            int digito = Character.getNumericValue(limpio.charAt(i));
            suma = suma + digito * multiplicadores[i];
        }
        int resto = suma % 11;
        int digitoVerificadorEsperado = 11 - resto;
        if (digitoVerificadorEsperado == 11) {
            digitoVerificadorEsperado = 0;
        }
        if (digitoVerificadorEsperado == 10) {
            return false;
        }
        int digitoVerificadorReal = Character.getNumericValue(limpio.charAt(10));
        return digitoVerificadorEsperado == digitoVerificadorReal;
    }

    public static double calcularIva(double neto, double alicuotaPorcentual) {
        return neto * (alicuotaPorcentual / 100.0);
    }

    public static double redondearDosDecimales(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public static String formatearFechaCorta(Date fecha) {
        if (fecha == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(fecha);
    }

    public static Date sumarDias(Date fecha, int dias) {
        long ms = fecha.getTime() + (dias * 24L * 60L * 60L * 1000L);
        return new Date(ms);
    }

    public static boolean escribirComprobanteEnDisco(String rutaArchivo, String contenido) {
        FileWriter fw = null;
        PrintWriter pw = null;
        try {
            fw = new FileWriter(rutaArchivo);
            pw = new PrintWriter(fw);
            pw.println(contenido);
            return true;
        } catch (IOException e) {
            System.out.println("No se pudo escribir el comprobante: " + e.getMessage());
            return false;
        } finally {
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static String armarJsonError(String mensaje) {
        return "{\"error\": \"" + mensaje.replace("\"", "'") + "\"}";
    }

    public static void log(String mensaje) {
        System.out.println("[ARQUITA-LEGACY] " + mensaje);
    }
}
