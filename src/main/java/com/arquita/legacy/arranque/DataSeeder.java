package com.arquita.legacy.arranque;

import java.util.List;

import javax.persistence.EntityManager;

import com.arquita.legacy.dominio.Contribuyente;
import com.arquita.legacy.persistencia.HibernateUtil;
import com.arquita.legacy.util.ArquitaUtils;

/**
 * Siembra de datos de demo. Antes era un bean de Spring con
 * init-method="cargarDatosDemo"; ahora lo dispara a mano ArranqueListener
 * (ServletContextListener) cuando arranca el contexto.
 */
public class DataSeeder {

    public void cargarDatosDemo() {
        crearSiNoExiste("30-71659554-0", "Emisor Demo SA (usar como cuitEmisor)", "RESPONSABLE_INSCRIPTO");
        crearSiNoExiste("20-12345678-6", "Comercial Demo SRL", "RESPONSABLE_INSCRIPTO");
        crearSiNoExiste("27-98765432-0", "Consultora Demo SA", "RESPONSABLE_INSCRIPTO");
        crearSiNoExiste("20-11111111-2", "Consumidor Final Demo", "CONSUMIDOR_FINAL");
        ArquitaUtils.log("Datos de demo cargados. Contribuyentes de prueba disponibles: "
                + "30-71659554-0 (emisor), 20-12345678-6, 27-98765432-0, 20-11111111-2");
    }

    private void crearSiNoExiste(String cuit, String razonSocial, String condicionIva) {
        // Cada alta corre en su propia transaccion, igual que antes: cada
        // llamada a crearSiNoExiste abre y cierra su propio EntityManager.
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            List<?> existentes = em.createQuery(
                    "from Contribuyente c where c.cuit = '" + cuit + "'").getResultList();
            if (!existentes.isEmpty()) {
                em.getTransaction().commit();
                return;
            }

            Contribuyente contribuyente = new Contribuyente();
            contribuyente.setCuit(cuit);
            contribuyente.setRazonSocial(razonSocial);
            contribuyente.setCondicionIva(condicionIva);
            em.persist(contribuyente);

            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            ArquitaUtils.log("No se pudo sembrar el contribuyente " + cuit + ": " + e.getMessage());
        } finally {
            em.close();
        }
    }
}
