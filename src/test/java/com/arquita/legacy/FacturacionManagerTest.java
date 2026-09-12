package com.arquita.legacy;

import org.junit.Test;

import com.arquita.legacy.util.ArquitaUtils;

public class FacturacionManagerTest {

    @Test
    public void testValidacionCuit() {
        boolean resultado = ArquitaUtils.esCuitValido("20-12345678-6");
        System.out.println("CUIT valido? " + resultado);
    }
}
