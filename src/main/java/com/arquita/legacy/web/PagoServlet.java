package com.arquita.legacy.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.arquita.legacy.dominio.Factura;
import com.arquita.legacy.dominio.Pago;
import com.arquita.legacy.persistencia.ConexionOracleSingleton;
import com.arquita.legacy.persistencia.HibernateUtil;
import com.arquita.legacy.util.ArquitaUtils;

/**
 * Antes era PagoController + FacturacionManager.registrarPago + PagoDAO.
 * Ahora todo vive aca adentro: validaciones, consulta via EntityManager y
 * la llamada al procedure PKG_FACTURACION.registrar_pago via JDBC plano.
 *
 * Mapeado a /api/pagos/*.
 */
public class PagoServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final double LIMITE_EFECTIVO_SIN_AUTORIZACION = 500000.0;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if (!"/registrar".equals(pathInfo)) {
            escribirJson(response, 404, ArquitaUtils.armarJsonError("Recurso no encontrado"));
            return;
        }

        Long facturaId;
        double monto;
        try {
            facturaId = Long.parseLong(request.getParameter("facturaId"));
            monto = Double.parseDouble(request.getParameter("monto"));
        } catch (NumberFormatException e) {
            escribirJson(response, 400, ArquitaUtils.armarJsonError("facturaId y monto son obligatorios"));
            return;
        }
        String medioPago = request.getParameter("medioPago");

        if (monto <= 0) {
            escribirJson(response, 400, ArquitaUtils.armarJsonError("El monto debe ser mayor a cero"));
            return;
        }

        if ("EFECTIVO".equals(medioPago) && monto > LIMITE_EFECTIVO_SIN_AUTORIZACION) {
            escribirJson(response, 400, ArquitaUtils.armarJsonError(
                    "Pagos en efectivo mayores a " + LIMITE_EFECTIVO_SIN_AUTORIZACION
                            + " requieren autorizacion adicional"));
            return;
        }

        EntityManager em = HibernateUtil.getEntityManager();
        try {
            Factura factura = em.find(Factura.class, facturaId);
            if (factura == null) {
                escribirJson(response, 404, ArquitaUtils.armarJsonError("Factura inexistente"));
                return;
            }

            @SuppressWarnings("unchecked")
            List<Pago> pagosExistentes = em.createQuery(
                    "from Pago p where p.facturaId = :fid")
                    .setParameter("fid", facturaId)
                    .getResultList();

            double montoYaPagado = 0;
            for (int i = 0; i < pagosExistentes.size(); i++) {
                montoYaPagado = montoYaPagado + pagosExistentes.get(i).getMonto();
            }
            if (montoYaPagado + monto > factura.getImporteTotal()) {
                escribirJson(response, 400, ArquitaUtils.armarJsonError(
                        "El pago supera el saldo pendiente de la factura"));
                return;
            }
        } finally {
            em.close();
        }

        Pago pago;
        try {
            pago = registrarPagoViaProcedure(facturaId, monto, medioPago);
        } catch (RuntimeException e) {
            ArquitaUtils.log("No se pudo registrar el pago: " + e.getMessage());
            escribirJson(response, 400, ArquitaUtils.armarJsonError("No se pudo registrar el pago"));
            return;
        }

        String json = "{\"id\":" + pago.getId() + ",\"monto\":" + pago.getMonto()
                + ",\"estado\":\"" + pago.getEstado() + "\"}";
        escribirJson(response, 200, json);
    }

    /**
     * Llama al procedure PL/SQL que hace el trabajo pesado (valida estado,
     * inserta el pago y pasa la factura a PAGADA si corresponde). No hay
     * DataSource de Spring: la conexion sale del mismo singleton casero que
     * ya usaba FacturaDAOImpl.ejecutarConsultaSql. La conexion del driver
     * thin de Oracle viene con autoCommit=true por default, asi que el
     * procedure queda comiteado sin que nadie llame a commit() a mano.
     */
    private Pago registrarPagoViaProcedure(Long facturaId, double monto, String medioPago) {
        Connection conexion = null;
        CallableStatement statement = null;
        try {
            conexion = ConexionOracleSingleton.getInstancia().getConexion();
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
            // La conexion del singleton NO se cierra: la reutiliza el
            // proximo que la pida (ver ConexionOracleSingleton.getConexion()).
        }
    }

    private void escribirJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
    }
}
