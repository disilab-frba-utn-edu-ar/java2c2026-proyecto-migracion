package com.arquita.legacy.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import javax.sql.DataSource;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.transaction.annotation.Transactional;

import com.arquita.legacy.dominio.Pago;

@Transactional
public class PagoDAOImpl implements PagoDAO {

    private SessionFactory sessionFactory;
    private DataSource dataSource;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
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

    @Override
    public Pago registrarPagoViaProcedure(Long facturaId, double monto, String medioPago) {
        Connection conexion = null;
        CallableStatement statement = null;
        try {
            conexion = dataSource.getConnection();
            statement = conexion.prepareCall("{call PKG_FACTURACION.registrar_pago(?, ?, ?, ?)}");
            statement.setLong(1, facturaId);
            statement.setDouble(2, monto);
            statement.setString(3, medioPago);
            statement.registerOutParameter(4, Types.NUMERIC);
            statement.execute();

            Pago pago = new Pago();
            pago.setId(statement.getLong(4));
            pago.setFacturaId(facturaId);
            pago.setMonto(monto);
            pago.setMedioPago(medioPago);
            pago.setEstado("CONFIRMADO");
            return pago;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                }
            }
            if (conexion != null) {
                try {
                    conexion.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }
}
