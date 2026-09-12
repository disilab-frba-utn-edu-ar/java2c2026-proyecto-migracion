package com.arquita.legacy.dominio;

public class TipoComprobante {

    public static final int FACTURA_A = 1;
    public static final int FACTURA_B = 6;
    public static final int FACTURA_C = 11;
    public static final int NOTA_CREDITO_B = 8;
    public static final int NOTA_DEBITO_B = 7;

    private TipoComprobante() {
    }

    public static String letra(int tipo) {
        switch (tipo) {
            case FACTURA_A:
                return "A";
            case FACTURA_B:
                return "B";
            case FACTURA_C:
                return "C";
            case NOTA_CREDITO_B:
                return "NC-B";
            case NOTA_DEBITO_B:
                return "ND-B";
            default:
                return "?";
        }
    }
}
