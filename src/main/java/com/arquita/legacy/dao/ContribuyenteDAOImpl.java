package com.arquita.legacy.dao;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.transaction.annotation.Transactional;

import com.arquita.legacy.dominio.Contribuyente;

@Transactional
public class ContribuyenteDAOImpl implements ContribuyenteDAO {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Contribuyente buscarPorCuit(String cuit) {
        Session session = sessionFactory.getCurrentSession();
        Query query = session.createQuery("from Contribuyente c where c.cuit = '" + cuit + "'");
        List<?> resultados = query.list();
        if (resultados.isEmpty()) {
            return null;
        }
        return (Contribuyente) resultados.get(0);
    }

    @Override
    public void guardar(Contribuyente contribuyente) {
        Session session = sessionFactory.getCurrentSession();
        session.save(contribuyente);
    }
}
