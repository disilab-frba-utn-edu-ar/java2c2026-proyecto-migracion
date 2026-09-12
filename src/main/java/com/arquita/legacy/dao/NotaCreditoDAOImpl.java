package com.arquita.legacy.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.transaction.annotation.Transactional;

import com.arquita.legacy.dominio.NotaCredito;

@Transactional
public class NotaCreditoDAOImpl implements NotaCreditoDAO {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void guardar(NotaCredito notaCredito) {
        Session session = sessionFactory.getCurrentSession();
        session.save(notaCredito);
    }
}
