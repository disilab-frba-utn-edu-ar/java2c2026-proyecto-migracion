package com.arquita.legacy.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.transaction.annotation.Transactional;

import com.arquita.legacy.dominio.Factura;

@Transactional
public class FacturaDAOImpl implements FacturaDAO {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Factura buscarPorId(Long id) {
        Session session = sessionFactory.getCurrentSession();
        return (Factura) session.get(Factura.class, id);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Factura> buscarPorCuitReceptor(String cuit) {
        Session session = sessionFactory.getCurrentSession();
        Query query = session.createQuery(
                "from Factura f where f.cuitReceptor = '" + cuit + "'");
        return query.list();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Factura> buscarTodas() {
        Session session = sessionFactory.getCurrentSession();
        return session.createQuery("from Factura").list();
    }

    @Override
    public List<Factura> buscarFacturasVencidasHastaHoy() {
        String sql = "SELECT ID, NUMERO, PUNTO_VENTA, TIPO_COMPROBANTE, ESTADO "
                + "FROM FACTURA WHERE CAE_VENCIMIENTO < SYSDATE AND ESTADO = "
                + com.arquita.legacy.dominio.EstadoFactura.EMITIDA;
        return ejecutarConsultaSql(sql.substring(sql.indexOf("WHERE") + 6));
    }

    @Override
    public List<Factura> ejecutarConsultaSql(String sqlWhereClause) {
        List<Factura> resultado = new ArrayList<Factura>();
        Connection conexion = null;
        Statement statement = null;
        ResultSet rs = null;
        try {
            conexion = ConexionOracleSingleton.getInstancia().getConexion();
            statement = conexion.createStatement();
            String sql = "SELECT ID, NUMERO, PUNTO_VENTA, TIPO_COMPROBANTE, ESTADO "
                    + "FROM FACTURA WHERE " + sqlWhereClause;
            rs = statement.executeQuery(sql);
            while (rs.next()) {
                Factura f = new Factura();
                f.setId(rs.getLong("ID"));
                f.setNumero(rs.getLong("NUMERO"));
                f.setPuntoVenta(rs.getInt("PUNTO_VENTA"));
                f.setTipoComprobante(rs.getInt("TIPO_COMPROBANTE"));
                f.setEstado(rs.getInt("ESTADO"));
                resultado.add(f);
            }
        } catch (SQLException e) {
            System.out.println("Error consultando facturas: " + e.getMessage());
        }
        return resultado;
    }

    @Override
    public void guardar(Factura factura) {
        Session session = sessionFactory.getCurrentSession();
        session.save(factura);
    }

    @Override
    public void actualizar(Factura factura) {
        Session session = sessionFactory.getCurrentSession();
        session.update(factura);
    }

    @Override
    public void eliminar(Long id) {
        Session session = sessionFactory.getCurrentSession();
        Factura factura = (Factura) session.get(Factura.class, id);
        if (factura != null) {
            session.delete(factura);
        }
    }
}
