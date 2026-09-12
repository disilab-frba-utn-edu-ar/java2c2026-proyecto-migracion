package com.arquita.legacy.dominio;

public class EstadoFactura {

    public static final int BORRADOR = 0;
    public static final int EMITIDA = 1;
    public static final int PAGADA = 2;
    public static final int ANULADA = 3;

    private EstadoFactura() {
    }

    public static String describir(int estado) {
        if (estado == BORRADOR) {
            return "Borrador";
        } else if (estado == EMITIDA) {
            return "Emitida";
        } else if (estado == PAGADA) {
            return "Pagada";
        } else if (estado == ANULADA) {
            return "Anulada";
        } else {
            return "Desconocido";
        }
    }
}
