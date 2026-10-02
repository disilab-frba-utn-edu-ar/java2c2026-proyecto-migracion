package com.arquita.legacy.persistencia;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * El "EntityManager armado a mano" de toda la vida: sin inyeccion de
 * dependencias, sin contenedor, un singleton estatico que fabrica la
 * EntityManagerFactory la primera vez que alguien la pide y la reutiliza
 * para el resto del ciclo de vida de la aplicacion.
 *
 * Cada servlet llama a getEntityManager() al principio del metodo y es
 * responsable de cerrarlo el mismo (ver ArranqueListener, que dispara la
 * creacion de la fabrica al arrancar el contexto y la cierra al bajar).
 */
public class HibernateUtil {

    private static final String UNIDAD_DE_PERSISTENCIA = "arquitaPU";

    // Lazy init sin "synchronized" a proposito: en un arranque con mas de
    // un hilo esto podria crear la fabrica dos veces. Es la misma mala
    // practica que ya tenia ConexionOracleSingleton.
    private static EntityManagerFactory entityManagerFactory;

    private HibernateUtil() {
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        if (entityManagerFactory == null) {
            entityManagerFactory = Persistence.createEntityManagerFactory(UNIDAD_DE_PERSISTENCIA);
        }
        return entityManagerFactory;
    }

    public static EntityManager getEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    public static void cerrarFabrica() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
        entityManagerFactory = null;
    }
}
