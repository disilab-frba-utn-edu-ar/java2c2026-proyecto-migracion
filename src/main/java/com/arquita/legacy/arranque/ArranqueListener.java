package com.arquita.legacy.arranque;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.arquita.legacy.persistencia.HibernateUtil;
import com.arquita.legacy.util.ArquitaUtils;

/**
 * Reemplaza al ContextLoaderListener de Spring: al arrancar la aplicacion
 * fuerza la construccion de la EntityManagerFactory (asi el primer request
 * no paga el costo de arrancar Hibernate) y dispara la carga de datos de
 * demo. Al bajar, cierra la fabrica.
 *
 * Declarado a mano en web.xml, sin @WebListener.
 */
public class ArranqueListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ArquitaUtils.log("Arrancando Arquita Legacy: inicializando Hibernate...");
        HibernateUtil.getEntityManagerFactory();

        new DataSeeder().cargarDatosDemo();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ArquitaUtils.log("Deteniendo Arquita Legacy: cerrando Hibernate...");
        HibernateUtil.cerrarFabrica();
    }
}
