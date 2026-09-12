package com.arquita.legacy.arranque;

import com.arquita.legacy.dao.ContribuyenteDAO;
import com.arquita.legacy.dominio.Contribuyente;
import com.arquita.legacy.util.ArquitaUtils;

public class DataSeeder {

    private ContribuyenteDAO contribuyenteDAO;

    public void setContribuyenteDAO(ContribuyenteDAO contribuyenteDAO) {
        this.contribuyenteDAO = contribuyenteDAO;
    }

    public void cargarDatosDemo() {
        crearSiNoExiste("30-71659554-0", "Emisor Demo SA (usar como cuitEmisor)", "RESPONSABLE_INSCRIPTO");
        crearSiNoExiste("20-12345678-6", "Comercial Demo SRL", "RESPONSABLE_INSCRIPTO");
        crearSiNoExiste("27-98765432-0", "Consultora Demo SA", "RESPONSABLE_INSCRIPTO");
        crearSiNoExiste("20-11111111-2", "Consumidor Final Demo", "CONSUMIDOR_FINAL");
        ArquitaUtils.log("Datos de demo cargados. Contribuyentes de prueba disponibles: "
                + "30-71659554-0 (emisor), 20-12345678-6, 27-98765432-0, 20-11111111-2");
    }

    private void crearSiNoExiste(String cuit, String razonSocial, String condicionIva) {
        if (contribuyenteDAO.buscarPorCuit(cuit) != null) {
            return;
        }
        Contribuyente contribuyente = new Contribuyente();
        contribuyente.setCuit(cuit);
        contribuyente.setRazonSocial(razonSocial);
        contribuyente.setCondicionIva(condicionIva);
        contribuyenteDAO.guardar(contribuyente);
    }
}
