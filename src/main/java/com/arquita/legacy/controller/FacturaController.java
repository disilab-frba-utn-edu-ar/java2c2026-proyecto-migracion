package com.arquita.legacy.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.arquita.legacy.dao.ContribuyenteDAO;
import com.arquita.legacy.dao.FacturaDAO;
import com.arquita.legacy.dominio.Contribuyente;
import com.arquita.legacy.dominio.DetalleFactura;
import com.arquita.legacy.dominio.Factura;
import com.arquita.legacy.dominio.Moneda;
import com.arquita.legacy.dominio.NotaCredito;
import com.arquita.legacy.dominio.TipoComprobante;
import com.arquita.legacy.service.FacturacionManager;
import com.arquita.legacy.util.ArquitaUtils;
import com.arquita.legacy.util.Constantes;

@Controller
@RequestMapping("/facturas")
public class FacturaController {

    private static final double LIMITE_IMPORTE_SIN_REVISION = 1000000.0;

    @Autowired
    private FacturacionManager facturacionManager;

    @Autowired
    private FacturaDAO facturaDAO;

    @Autowired
    private ContribuyenteDAO contribuyenteDAO;

    @RequestMapping(value = "/crear", method = RequestMethod.POST)
    public void crear(@RequestParam String cuitEmisor,
                       @RequestParam String cuitReceptor,
                       @RequestParam int tipoComprobante,
                       @RequestParam String descripcionItem,
                       @RequestParam double cantidad,
                       @RequestParam double precioUnitario,
                       @RequestParam(required = false, defaultValue = Moneda.ARS) String moneda,
                       HttpServletResponse response) throws IOException {

        if (cuitEmisor == null || cuitEmisor.trim().length() == 0) {
            escribirJson(response, 400, ArquitaUtils.armarJsonError("cuitEmisor es obligatorio"));
            return;
        }

        Contribuyente emisor = contribuyenteDAO.buscarPorCuit(cuitEmisor);

        if (tipoComprobante == TipoComprobante.FACTURA_A) {
            if (emisor == null || !"RESPONSABLE_INSCRIPTO".equals(emisor.getCondicionIva())) {
                escribirJson(response, 400,
                        ArquitaUtils.armarJsonError("Solo un responsable inscripto puede emitir Factura A"));
                return;
            }
        }

        double alicuotaIva = Constantes.ALICUOTA_IVA_GENERAL;
        if (emisor != null && "MONOTRIBUTO".equals(emisor.getCondicionIva())) {
            alicuotaIva = 0.0;
        }

        double totalEstimado = cantidad * precioUnitario * (1 + alicuotaIva / 100.0);
        if (totalEstimado > LIMITE_IMPORTE_SIN_REVISION) {
            Contribuyente receptor = contribuyenteDAO.buscarPorCuit(cuitReceptor);
            if (receptor == null) {
                escribirJson(response, 400, ArquitaUtils.armarJsonError(
                        "Importes superiores a " + LIMITE_IMPORTE_SIN_REVISION
                                + " requieren que el receptor ya este dado de alta"));
                return;
            }
        }

        List<DetalleFactura> detalles = new ArrayList<DetalleFactura>();
        detalles.add(new DetalleFactura(descripcionItem, cantidad, precioUnitario, alicuotaIva));

        Factura factura = facturacionManager.crearFactura(
                cuitEmisor, cuitReceptor, tipoComprobante, detalles, moneda);

        if (factura == null) {
            escribirJson(response, 400, ArquitaUtils.armarJsonError("No se pudo crear la factura"));
            return;
        }

        String json = "{"
                + "\"id\": " + factura.getId() + ", "
                + "\"numero\": " + factura.getNumero() + ", "
                + "\"moneda\": \"" + factura.getMoneda() + "\", "
                + "\"total\": " + factura.getImporteTotal() + ", "
                + "\"totalEnPesos\": " + factura.getImporteTotalEnPesos() + ", "
                + "\"cae\": \"" + factura.getCae() + "\""
                + "}";
        escribirJson(response, 200, json);
    }

    @RequestMapping(value = "/nota-credito", method = RequestMethod.POST)
    public void emitirNotaCredito(@RequestParam Long facturaId,
                                   @RequestParam String motivo,
                                   HttpServletResponse response) throws IOException {
        Factura facturaOriginal = facturaDAO.buscarPorId(facturaId);
        if (facturaOriginal == null) {
            escribirJson(response, 404, ArquitaUtils.armarJsonError("Factura inexistente"));
            return;
        }
        if (motivo == null || motivo.trim().length() < 5) {
            escribirJson(response, 400,
                    ArquitaUtils.armarJsonError("El motivo de la nota de credito debe tener al menos 5 caracteres"));
            return;
        }

        NotaCredito notaCredito = facturacionManager.emitirNotaCredito(facturaId, motivo);
        if (notaCredito == null) {
            escribirJson(response, 400, ArquitaUtils.armarJsonError("No se pudo emitir la nota de credito"));
            return;
        }
        String json = "{"
                + "\"numero\": " + notaCredito.getNumero() + ", "
                + "\"importe\": " + notaCredito.getImporte() + ", "
                + "\"cae\": \"" + notaCredito.getCae() + "\""
                + "}";
        escribirJson(response, 200, json);
    }

    @RequestMapping(value = "/por-receptor", method = RequestMethod.GET)
    public void listarPorReceptor(@RequestParam String cuit, HttpServletResponse response) throws IOException {
        List<Factura> facturas = facturaDAO.buscarPorCuitReceptor(cuit);

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
    }

    @RequestMapping(value = "/anular", method = RequestMethod.POST)
    public void anular(@RequestParam Long facturaId, HttpServletResponse response) throws IOException {
        Factura factura = facturacionManager.anularFactura(facturaId);
        if (factura == null) {
            escribirJson(response, 400, ArquitaUtils.armarJsonError("No se pudo anular la factura"));
            return;
        }
        escribirJson(response, 200, "{\"estado\": \"anulada\"}");
    }

    private void escribirJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
    }
}
