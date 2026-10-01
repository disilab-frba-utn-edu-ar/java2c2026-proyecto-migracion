package com.arquita.legacy.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.arquita.caeclient.AutorizadorFiscalClient;
import com.arquita.legacy.dominio.Contribuyente;
import com.arquita.legacy.dominio.DetalleFactura;
import com.arquita.legacy.dominio.EstadoFactura;
import com.arquita.legacy.dominio.Factura;
import com.arquita.legacy.dominio.Moneda;
import com.arquita.legacy.dominio.NotaCredito;
import com.arquita.legacy.dominio.TipoComprobante;
import com.arquita.legacy.persistencia.ConexionOracleSingleton;
import com.arquita.legacy.persistencia.HibernateUtil;
import com.arquita.legacy.util.ArquitaUtils;
import com.arquita.legacy.util.Constantes;
import com.arquita.legacy.util.NotificacionEmailHelper;

/**
 * Antes era FacturaController + casi todo FacturacionManager + la mitad de
 * FacturaDAOImpl. Ahora es un unico servlet: cada metodo abre su propio
 * EntityManager y maneja su propia transaccion a mano (sin @Transactional,
 * sin AOP). El cliente del jar de CAE ya no es un bean de Spring, se
 * instancia directo en el campo.
 *
 * Mapeado a /api/facturas/*.
 */
public class FacturaServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final double LIMITE_IMPORTE_SIN_REVISION = 1000000.0;
    private static final double COTIZACION_USD_HARDCODEADA = 1000.0;

    // Contador de numeracion en memoria: arranca en 1 en cada deploy y lo
    // comparten facturas y notas de credito, tal como antes en
    // FacturacionManager. No esta respaldado por la base.
    private static long contadorNumeroFactura = 1L;

    private final AutorizadorFiscalClient autorizadorFiscalClient = new AutorizadorFiscalClient();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if ("/por-receptor".equals(pathInfo)) {
            porReceptor(request, response);
        } else {
            escribirJson(response, 404, ArquitaUtils.armarJsonError("Recurso no encontrado"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if ("/crear".equals(pathInfo)) {
            crear(request, response);
        } else if ("/nota-credito".equals(pathInfo)) {
            notaCredito(request, response);
        } else if ("/anular".equals(pathInfo)) {
            anular(request, response);
        } else {
            escribirJson(response, 404, ArquitaUtils.armarJsonError("Recurso no encontrado"));
        }
    }

    // ------------------------------------------------------------------
    // POST /crear
    // ------------------------------------------------------------------

    private void crear(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String cuitEmisor = request.getParameter("cuitEmisor");
        String cuitReceptor = request.getParameter("cuitReceptor");
        String descripcionItem = request.getParameter("descripcionItem");
        String moneda = request.getParameter("moneda");
        if (moneda == null || moneda.trim().length() == 0) {
            moneda = Moneda.ARS;
        }

        int tipoComprobante;
        double cantidad;
        double precioUnitario;
        try {
            tipoComprobante = Integer.parseInt(request.getParameter("tipoComprobante"));
            cantidad = Double.parseDouble(request.getParameter("cantidad"));
            precioUnitario = Double.parseDouble(request.getParameter("precioUnitario"));
        } catch (NumberFormatException e) {
            escribirJson(response, 400, ArquitaUtils.armarJsonError(
                    "tipoComprobante, cantidad y precioUnitario son obligatorios y numericos"));
            return;
        }

        if (cuitEmisor == null || cuitEmisor.trim().length() == 0) {
            escribirJson(response, 400, ArquitaUtils.armarJsonError("cuitEmisor es obligatorio"));
            return;
        }

        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Contribuyente emisor = buscarContribuyentePorCuit(em, cuitEmisor);

            if (tipoComprobante == TipoComprobante.FACTURA_A) {
                if (emisor == null || !"RESPONSABLE_INSCRIPTO".equals(emisor.getCondicionIva())) {
                    em.getTransaction().commit();
                    escribirJson(response, 400, ArquitaUtils.armarJsonError(
                            "Solo un responsable inscripto puede emitir Factura A"));
                    return;
                }
            }

            double alicuotaIva = Constantes.ALICUOTA_IVA_GENERAL;
            if (emisor != null && "MONOTRIBUTO".equals(emisor.getCondicionIva())) {
                alicuotaIva = 0.0;
            }

            double totalEstimado = cantidad * precioUnitario * (1 + alicuotaIva / 100.0);
            if (totalEstimado > LIMITE_IMPORTE_SIN_REVISION) {
                Contribuyente receptorPrevio = buscarContribuyentePorCuit(em, cuitReceptor);
                if (receptorPrevio == null) {
                    em.getTransaction().commit();
                    escribirJson(response, 400, ArquitaUtils.armarJsonError(
                            "Importes superiores a " + LIMITE_IMPORTE_SIN_REVISION
                                    + " requieren que el receptor ya este dado de alta"));
                    return;
                }
            }

            List<DetalleFactura> detalles = new ArrayList<DetalleFactura>();
            detalles.add(new DetalleFactura(descripcionItem, cantidad, precioUnitario, alicuotaIva));

            Factura factura = construirYGuardarFactura(em, cuitEmisor, cuitReceptor, tipoComprobante, detalles, moneda);

            if (factura == null) {
                em.getTransaction().commit();
                escribirJson(response, 400, ArquitaUtils.armarJsonError("No se pudo crear la factura"));
                return;
            }

            em.getTransaction().commit();

            String json = "{"
                    + "\"id\": " + factura.getId() + ", "
                    + "\"numero\": " + factura.getNumero() + ", "
                    + "\"moneda\": \"" + factura.getMoneda() + "\", "
                    + "\"total\": " + factura.getImporteTotal() + ", "
                    + "\"totalEnPesos\": " + factura.getImporteTotalEnPesos() + ", "
                    + "\"cae\": \"" + factura.getCae() + "\""
                    + "}";
            escribirJson(response, 200, json);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            ArquitaUtils.log("Error inesperado creando factura: " + e.getMessage());
            escribirJson(response, 400, ArquitaUtils.armarJsonError("No se pudo crear la factura"));
        } finally {
            em.close();
        }
    }

    /**
     * Equivalente a FacturacionManager.crearFactura: re-valida ambos CUIT
     * (el controller original solo validaba que cuitEmisor no viniera
     * vacio), da de alta el receptor como "Consumidor Final" si no existia,
     * calcula neto/iva/total, pide el CAE al .jar vendorizado, guarda con
     * reintentos, escribe el comprobante en disco y dispara el mail falso.
     */
    private Factura construirYGuardarFactura(EntityManager em, String cuitEmisor, String cuitReceptor,
                                              int tipoComprobante, List<DetalleFactura> detalles, String moneda) {
        if (!ArquitaUtils.esCuitValido(cuitEmisor)) {
            ArquitaUtils.log("CUIT emisor invalido: " + cuitEmisor);
            return null;
        }
        if (!ArquitaUtils.esCuitValido(cuitReceptor)) {
            ArquitaUtils.log("CUIT receptor invalido: " + cuitReceptor);
            return null;
        }
        if (detalles == null || detalles.isEmpty()) {
            ArquitaUtils.log("La factura no tiene detalles, se rechaza.");
            return null;
        }

        Contribuyente receptor = buscarContribuyentePorCuit(em, cuitReceptor);
        if (receptor == null) {
            receptor = new Contribuyente();
            receptor.setCuit(cuitReceptor);
            receptor.setRazonSocial("Consumidor Final");
            receptor.setCondicionIva("CONSUMIDOR_FINAL");
            em.persist(receptor);
        }

        double neto = 0;
        double iva = 0;
        for (int i = 0; i < detalles.size(); i++) {
            DetalleFactura d = detalles.get(i);
            double subtotalItem = d.getCantidad() * d.getPrecioUnitario();
            double ivaItem = ArquitaUtils.calcularIva(subtotalItem, d.getAlicuotaIva());
            neto = neto + subtotalItem;
            iva = iva + ivaItem;
        }
        neto = ArquitaUtils.redondearDosDecimales(neto);
        iva = ArquitaUtils.redondearDosDecimales(iva);
        double total = ArquitaUtils.redondearDosDecimales(neto + iva);

        Factura factura = new Factura();
        factura.setCuitEmisor(cuitEmisor);
        factura.setCuitReceptor(cuitReceptor);
        factura.setTipoComprobante(tipoComprobante);
        factura.setFecha(new Date());
        factura.setPuntoVenta(1);
        factura.setNumero(generarProximoNumero());
        factura.setImporteNeto(neto);
        factura.setImporteIva(iva);
        factura.setImporteTotal(total);
        factura.setEstado(EstadoFactura.BORRADOR);
        factura.setDetalles(detalles);
        factura.setMoneda(moneda == null ? Moneda.ARS : moneda);

        if (Moneda.USD.equals(factura.getMoneda())) {
            factura.setCotizacionAlEmitir(COTIZACION_USD_HARDCODEADA);
        }

        factura.setCae(autorizadorFiscalClient.autorizar(cuitEmisor, factura.getNumero()));
        factura.setCaeVencimiento(ArquitaUtils.sumarDias(new Date(), 10));
        factura.setEstado(EstadoFactura.EMITIDA);

        boolean guardadoOk = guardarConReintentos(em, factura);
        if (!guardadoOk) {
            ArquitaUtils.log("No se pudo guardar la factura luego de "
                    + Constantes.MAX_REINTENTOS_GUARDADO + " intentos.");
            return null;
        }

        String contenidoComprobante = "FACTURA " + TipoComprobante.letra(tipoComprobante)
                + " Nro " + factura.getNumero() + " - CAE " + factura.getCae();
        ArquitaUtils.escribirComprobanteEnDisco(
                "factura-" + factura.getNumero() + ".txt", contenidoComprobante);

        NotificacionEmailHelper.enviarNotificacionFactura(
                cuitReceptor,
                "Nueva factura emitida",
                "Se emitio la factura " + factura.getNumero() + " por $" + total);

        return factura;
    }

    private static synchronized long generarProximoNumero() {
        long numero = contadorNumeroFactura;
        contadorNumeroFactura = contadorNumeroFactura + 1;
        return numero;
    }

    /**
     * Mismo patron fragil de siempre: em.persist() no siempre dispara el
     * INSERT (Hibernate lo puede diferir hasta el flush/commit), asi que
     * este try/catch no necesariamente atrapa errores reales de guardado.
     * Se deja tal cual estaba en FacturacionManager.guardarConReintentos.
     */
    private boolean guardarConReintentos(EntityManager em, Factura factura) {
        int intentos = 0;
        while (intentos < Constantes.MAX_REINTENTOS_GUARDADO) {
            try {
                em.persist(factura);
                return true;
            } catch (RuntimeException e) {
                intentos++;
                ArquitaUtils.log("Fallo el guardado (intento " + intentos + "): " + e.getMessage());
                try {
                    Thread.sleep(Constantes.ESPERA_ENTRE_REINTENTOS_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // POST /nota-credito
    // ------------------------------------------------------------------

    private void notaCredito(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long facturaId;
        try {
            facturaId = Long.parseLong(request.getParameter("facturaId"));
        } catch (NumberFormatException e) {
            escribirJson(response, 400, ArquitaUtils.armarJsonError("facturaId es obligatorio"));
            return;
        }
        String motivo = request.getParameter("motivo");

        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Factura facturaOriginal = em.find(Factura.class, facturaId);
            if (facturaOriginal == null) {
                em.getTransaction().commit();
                escribirJson(response, 404, ArquitaUtils.armarJsonError("Factura inexistente"));
                return;
            }
            if (motivo == null || motivo.trim().length() < 5) {
                em.getTransaction().commit();
                escribirJson(response, 400, ArquitaUtils.armarJsonError(
                        "El motivo de la nota de credito debe tener al menos 5 caracteres"));
                return;
            }

            NotaCredito notaCredito = emitirNotaCredito(em, facturaOriginal, motivo);

            em.getTransaction().commit();

            String json = "{"
                    + "\"numero\": " + notaCredito.getNumero() + ", "
                    + "\"importe\": " + notaCredito.getImporte() + ", "
                    + "\"cae\": \"" + notaCredito.getCae() + "\""
                    + "}";
            escribirJson(response, 200, json);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            ArquitaUtils.log("No se pudo emitir la nota de credito: " + e.getMessage());
            escribirJson(response, 400, ArquitaUtils.armarJsonError("No se pudo emitir la nota de credito"));
        } finally {
            em.close();
        }
    }

    private NotaCredito emitirNotaCredito(EntityManager em, Factura facturaOriginal, String motivo) {
        NotaCredito notaCredito = new NotaCredito();
        notaCredito.setFacturaOriginalId(facturaOriginal.getId());
        notaCredito.setFecha(new Date());
        notaCredito.setPuntoVenta(facturaOriginal.getPuntoVenta());
        notaCredito.setNumero(generarProximoNumero());
        notaCredito.setMotivo(motivo);
        notaCredito.setImporte(facturaOriginal.getImporteTotal());
        notaCredito.setCae(autorizadorFiscalClient.autorizar(facturaOriginal.getCuitEmisor(), notaCredito.getNumero()));
        notaCredito.setCaeVencimiento(ArquitaUtils.sumarDias(new Date(), 10));

        em.persist(notaCredito);

        // No cambia el estado de la factura original: una nota de credito
        // emitida por este camino conviene con la que emite el procedure
        // PL/SQL al anular, pero son dos caminos independientes (ver
        // db/init/02_procedures.sql).
        NotificacionEmailHelper.enviarNotificacionFactura(
                facturaOriginal.getCuitReceptor(),
                "Nota de credito emitida",
                "Se emitio una nota de credito por la factura " + facturaOriginal.getNumero()
                        + ". Motivo: " + motivo);

        return notaCredito;
    }

    // ------------------------------------------------------------------
    // POST /anular
    // ------------------------------------------------------------------

    private void anular(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long facturaId;
        try {
            facturaId = Long.parseLong(request.getParameter("facturaId"));
        } catch (NumberFormatException e) {
            escribirJson(response, 400, ArquitaUtils.armarJsonError("facturaId es obligatorio"));
            return;
        }

        try {
            Long notaCreditoId = anularConProcedure(facturaId);

            // Se abre el EntityManager recien ahora, despues de que el
            // procedure (que usa su propia conexion) ya comiteo el UPDATE,
            // para no arriesgarse a leer un estado desactualizado.
            Factura factura;
            EntityManager em = HibernateUtil.getEntityManager();
            try {
                factura = em.find(Factura.class, facturaId);
            } finally {
                em.close();
            }

            if (factura == null) {
                escribirJson(response, 400, ArquitaUtils.armarJsonError("No se pudo anular la factura"));
                return;
            }

            if (notaCreditoId != null) {
                NotificacionEmailHelper.enviarNotificacionFactura(
                        factura.getCuitReceptor(),
                        "Nota de credito emitida",
                        "Se emitio una nota de credito por la factura " + factura.getNumero()
                                + ". Motivo: Anulacion de factura pagada");
            }

            escribirJson(response, 200, "{\"estado\": \"anulada\"}");
        } catch (RuntimeException e) {
            ArquitaUtils.log("No se pudo anular la factura: " + e.getMessage());
            escribirJson(response, 400, ArquitaUtils.armarJsonError("No se pudo anular la factura"));
        }
    }

    /**
     * Sin DataSource de Spring: la conexion sale del mismo singleton casero
     * que ya usaba FacturaDAOImpl.ejecutarConsultaSql. El driver thin de
     * Oracle entrega la conexion con autoCommit=true, asi que el UPDATE que
     * hace el procedure queda comiteado sin que nadie llame a commit() aca.
     */
    private Long anularConProcedure(Long facturaId) {
        Connection conexion = null;
        CallableStatement statement = null;
        try {
            conexion = ConexionOracleSingleton.getInstancia().getConexion();
            statement = conexion.prepareCall("{call PKG_FACTURACION.anular_factura(?, ?)}");
            statement.setLong(1, facturaId);
            statement.registerOutParameter(2, Types.NUMERIC);
            statement.execute();
            long notaCreditoId = statement.getLong(2);
            return statement.wasNull() ? null : notaCreditoId;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // GET /por-receptor
    // ------------------------------------------------------------------

    private void porReceptor(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String cuit = request.getParameter("cuit");

        EntityManager em = HibernateUtil.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Factura> facturas = em.createQuery(
                    "from Factura f where f.cuitReceptor = '" + cuit + "'").getResultList();

            double totalAcumulado = 0;
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < facturas.size(); i++) {
                Factura f = facturas.get(i);
                if (i > 0) {
                    json.append(",");
                }
                totalAcumulado = totalAcumulado + f.getImporteTotalEnPesos();
                json.append("{\"numero\":").append(f.getNumero())
                        .append(",\"total\":").append(f.getImporteTotal())
                        .append(",\"estado\":").append(f.getEstado())
                        .append("}");
            }
            json.append("]");

            if (totalAcumulado > LIMITE_IMPORTE_SIN_REVISION) {
                ArquitaUtils.log("Contribuyente " + cuit + " supera el limite acumulado: " + totalAcumulado);
            }

            escribirJson(response, 200, json.toString());
        } finally {
            em.close();
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Contribuyente buscarContribuyentePorCuit(EntityManager em, String cuit) {
        @SuppressWarnings("unchecked")
        List<Contribuyente> resultados = em.createQuery(
                "from Contribuyente c where c.cuit = '" + cuit + "'").getResultList();
        if (resultados.isEmpty()) {
            return null;
        }
        return resultados.get(0);
    }

    private void escribirJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
    }

    // ------------------------------------------------------------------
    // Codigo muerto heredado de FacturacionManager/FacturaDAOImpl: no lo
    // alcanza ningun endpoint hoy, se conserva tal cual para el ejercicio
    // de migracion (ver README.md).
    // ------------------------------------------------------------------

    @SuppressWarnings("unused")
    private List<Factura> buscarFacturasVencidasHastaHoy() {
        String sql = "SELECT ID, NUMERO, PUNTO_VENTA, TIPO_COMPROBANTE, ESTADO "
                + "FROM FACTURA WHERE CAE_VENCIMIENTO < SYSDATE AND ESTADO = "
                + EstadoFactura.EMITIDA;
        return ejecutarConsultaSql(sql.substring(sql.indexOf("WHERE") + 6));
    }

    @SuppressWarnings("unused")
    private List<Factura> ejecutarConsultaSql(String sqlWhereClause) {
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

    @SuppressWarnings("unused")
    private double calcularTotalFacturadoPorContribuyente(EntityManager em, String cuit) {
        @SuppressWarnings("unchecked")
        List<Factura> facturas = em.createQuery(
                "from Factura f where f.cuitReceptor = '" + cuit + "'").getResultList();
        double total = 0;
        for (int i = 0; i < facturas.size(); i++) {
            Factura f = facturas.get(i);
            if (f.getEstado() != EstadoFactura.ANULADA) {
                total = total + f.getImporteTotal();
            }
        }
        return total;
    }

    @SuppressWarnings("unused")
    private void enviarRecordatoriosDePago() {
        List<Factura> vencidas = buscarFacturasVencidasHastaHoy();
        List<Thread> hilosLanzados = new ArrayList<Thread>();
        for (int i = 0; i < vencidas.size(); i++) {
            final Factura f = vencidas.get(i);
            Thread hilo = new Thread(new Runnable() {
                @Override
                public void run() {
                    NotificacionEmailHelper.enviarNotificacionFactura(
                            f.getCuitReceptor(),
                            "Recordatorio de pago",
                            "La factura " + f.getNumero() + " esta vencida.");
                }
            });
            hilo.start();
            hilosLanzados.add(hilo);
        }
        ArquitaUtils.log("Se lanzaron " + hilosLanzados.size() + " hilos de recordatorio sin pool.");
    }
}
