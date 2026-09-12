package com.arquita.legacy.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.arquita.legacy.dao.ContribuyenteDAO;
import com.arquita.legacy.dao.FacturaDAO;
import com.arquita.legacy.dao.NotaCreditoDAO;
import com.arquita.legacy.dao.PagoDAO;
import com.arquita.legacy.dominio.Contribuyente;
import com.arquita.legacy.dominio.DetalleFactura;
import com.arquita.legacy.dominio.EstadoFactura;
import com.arquita.legacy.dominio.Factura;
import com.arquita.legacy.dominio.Moneda;
import com.arquita.legacy.dominio.NotaCredito;
import com.arquita.legacy.dominio.Pago;
import com.arquita.legacy.util.ArquitaUtils;
import com.arquita.legacy.util.Constantes;

@Transactional
public class FacturacionManager {

    private FacturaDAO facturaDAO;
    private PagoDAO pagoDAO;
    private ContribuyenteDAO contribuyenteDAO;
    private NotaCreditoDAO notaCreditoDAO;

    private static final double COTIZACION_USD_HARDCODEADA = 1000.0;

    private static long contadorNumeroFactura = 1L;

    public void setFacturaDAO(FacturaDAO facturaDAO) {
        this.facturaDAO = facturaDAO;
    }

    public void setPagoDAO(PagoDAO pagoDAO) {
        this.pagoDAO = pagoDAO;
    }

    public void setContribuyenteDAO(ContribuyenteDAO contribuyenteDAO) {
        this.contribuyenteDAO = contribuyenteDAO;
    }

    public void setNotaCreditoDAO(NotaCreditoDAO notaCreditoDAO) {
        this.notaCreditoDAO = notaCreditoDAO;
    }

    public Factura crearFactura(String cuitEmisor, String cuitReceptor, int tipoComprobante,
                                 List<DetalleFactura> detalles, String moneda) {

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

        Contribuyente receptor = contribuyenteDAO.buscarPorCuit(cuitReceptor);
        if (receptor == null) {
            receptor = new Contribuyente();
            receptor.setCuit(cuitReceptor);
            receptor.setRazonSocial("Consumidor Final");
            receptor.setCondicionIva("CONSUMIDOR_FINAL");
            contribuyenteDAO.guardar(receptor);
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

        // TODO: falta integrar esto con un sistema externo (a definir).
        factura.setCae(generarCaeLocalDeMentira(cuitEmisor));
        factura.setCaeVencimiento(ArquitaUtils.sumarDias(new Date(), 10));
        factura.setEstado(EstadoFactura.EMITIDA);

        boolean guardadoOk = guardarConReintentos(factura);
        if (!guardadoOk) {
            ArquitaUtils.log("No se pudo guardar la factura luego de "
                    + Constantes.MAX_REINTENTOS_GUARDADO + " intentos.");
            return null;
        }

        String contenidoComprobante = "FACTURA " + com.arquita.legacy.dominio.TipoComprobante.letra(tipoComprobante)
                + " Nro " + factura.getNumero() + " - CAE " + factura.getCae();
        ArquitaUtils.escribirComprobanteEnDisco(
                "factura-" + factura.getNumero() + ".txt", contenidoComprobante);

        NotificacionEmailHelper.enviarNotificacionFactura(
                cuitReceptor,
                "Nueva factura emitida",
                "Se emitio la factura " + factura.getNumero() + " por $" + total);

        return factura;
    }

    private String generarCaeLocalDeMentira(String cuitEmisor) {
        return "CAE" + cuitEmisor.replace("-", "")
                + String.valueOf(System.currentTimeMillis()).substring(5);
    }

    private synchronized long generarProximoNumero() {
        long numero = contadorNumeroFactura;
        contadorNumeroFactura = contadorNumeroFactura + 1;
        return numero;
    }

    private boolean guardarConReintentos(Factura factura) {
        int intentos = 0;
        while (intentos < Constantes.MAX_REINTENTOS_GUARDADO) {
            try {
                facturaDAO.guardar(factura);
                return true;
            } catch (Exception e) {
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

    public Pago registrarPago(Long facturaId, double monto, String medioPago) {
        Factura factura = facturaDAO.buscarPorId(facturaId);
        if (factura == null) {
            ArquitaUtils.log("Factura inexistente: " + facturaId);
            return null;
        }

        switch (factura.getEstado()) {
            case EstadoFactura.BORRADOR:
                ArquitaUtils.log("No se puede pagar una factura en borrador.");
                return null;
            case EstadoFactura.ANULADA:
                ArquitaUtils.log("No se puede pagar una factura anulada.");
                return null;
            case EstadoFactura.PAGADA:
                ArquitaUtils.log("La factura ya estaba pagada, se registra pago igual (posible duplicado).");
                break;
            case EstadoFactura.EMITIDA:
                break;
            default:
                ArquitaUtils.log("Estado de factura desconocido: " + factura.getEstado());
                return null;
        }

        Pago pago = new Pago();
        pago.setFacturaId(facturaId);
        pago.setFecha(new Date());
        pago.setMonto(monto);
        pago.setMedioPago(medioPago);
        pago.setEstado("CONFIRMADO");

        pagoDAO.guardar(pago);

        if (montoCubreTotal(facturaId, factura.getImporteTotal())) {
            factura.setEstado(EstadoFactura.PAGADA);
            facturaDAO.actualizar(factura);
        }

        return pago;
    }

    private boolean montoCubreTotal(Long facturaId, double importeTotal) {
        List<Pago> pagos = pagoDAO.buscarPorFacturaId(facturaId);
        double acumulado = 0;
        for (int i = 0; i < pagos.size(); i++) {
            acumulado = acumulado + pagos.get(i).getMonto();
        }
        return acumulado >= importeTotal;
    }

    public Factura anularFactura(Long facturaId) {
        Factura factura = facturaDAO.buscarPorId(facturaId);
        if (factura == null) {
            return null;
        }
        if (factura.getEstado() == EstadoFactura.PAGADA) {
            ArquitaUtils.log("Factura ya pagada: se emite nota de credito en vez de anular directamente.");
            emitirNotaCredito(facturaId, "Anulacion de factura pagada");
        }
        factura.setEstado(EstadoFactura.ANULADA);
        facturaDAO.actualizar(factura);
        return factura;
    }

    public NotaCredito emitirNotaCredito(Long facturaId, String motivo) {
        Factura facturaOriginal = facturaDAO.buscarPorId(facturaId);
        if (facturaOriginal == null) {
            ArquitaUtils.log("No existe la factura " + facturaId + " para emitir nota de credito.");
            return null;
        }

        NotaCredito notaCredito = new NotaCredito();
        notaCredito.setFacturaOriginalId(facturaId);
        notaCredito.setFecha(new Date());
        notaCredito.setPuntoVenta(facturaOriginal.getPuntoVenta());
        notaCredito.setNumero(generarProximoNumero());
        notaCredito.setMotivo(motivo);
        notaCredito.setImporte(facturaOriginal.getImporteTotal());
        // TODO: falta integrar esto con un sistema externo (a definir).
        notaCredito.setCae(generarCaeLocalDeMentira(facturaOriginal.getCuitEmisor()));
        notaCredito.setCaeVencimiento(ArquitaUtils.sumarDias(new Date(), 10));

        notaCreditoDAO.guardar(notaCredito);

        NotificacionEmailHelper.enviarNotificacionFactura(
                facturaOriginal.getCuitReceptor(),
                "Nota de credito emitida",
                "Se emitio una nota de credito por la factura " + facturaOriginal.getNumero()
                        + ". Motivo: " + motivo);

        return notaCredito;
    }

    public double calcularTotalFacturadoPorContribuyente(String cuit) {
        List<Factura> facturas = facturaDAO.buscarPorCuitReceptor(cuit);
        double total = 0;
        for (int i = 0; i < facturas.size(); i++) {
            Factura f = facturas.get(i);
            if (f.getEstado() != EstadoFactura.ANULADA) {
                total = total + f.getImporteTotal();
            }
        }
        return total;
    }

    public void enviarRecordatoriosDePago() {
        List<Factura> vencidas = facturaDAO.buscarFacturasVencidasHastaHoy();
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
