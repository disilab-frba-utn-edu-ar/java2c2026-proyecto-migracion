package com.arquita.legacy.dao;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.transaction.annotation.Transactional;

import com.arquita.legacy.dominio.Pago;

@Transactional
public class PagoDAOImpl implements PagoDAO {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Pago buscarPorId(Long id) {
        Session session = sessionFactory.getCurrentSession();
        return (Pago) session.get(Pago.class, id);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Pago> buscarPorFacturaId(Long facturaId) {
        Session session = sessionFactory.getCurrentSession();
        Query query = session.createQuery("from Pago p where p.facturaId = :fid");
        query.setLong("fid", facturaId);
        return query.list();
    }

    @Override
    public void guardar(Pago pago) {
        Session session = sessionFactory.getCurrentSession();
        session.save(pago);
    }
}
