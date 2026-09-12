package com.arquita.legacy.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.arquita.legacy.dao.FacturaDAO;
import com.arquita.legacy.dao.PagoDAO;
import com.arquita.legacy.dominio.Factura;
import com.arquita.legacy.dominio.Pago;
import com.arquita.legacy.service.FacturacionManager;
import com.arquita.legacy.util.ArquitaUtils;

@Controller
@RequestMapping("/pagos")
public class PagoController {

    private static final double LIMITE_EFECTIVO_SIN_AUTORIZACION = 500000.0;

    @Autowired
    private FacturacionManager facturacionManager;

    @Autowired
    private FacturaDAO facturaDAO;

    @Autowired
    private PagoDAO pagoDAO;

    @RequestMapping(value = "/registrar", method = RequestMethod.POST)
    public void registrar(@RequestParam Long facturaId,
                           @RequestParam double monto,
                           @RequestParam String medioPago,
                           HttpServletResponse response) throws IOException {

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

        Factura factura = facturaDAO.buscarPorId(facturaId);
        if (factura == null) {
            escribirJson(response, 404, ArquitaUtils.armarJsonError("Factura inexistente"));
            return;
        }

        List<Pago> pagosExistentes = pagoDAO.buscarPorFacturaId(facturaId);
        double montoYaPagado = 0;
        for (int i = 0; i < pagosExistentes.size(); i++) {
            montoYaPagado = montoYaPagado + pagosExistentes.get(i).getMonto();
        }
        if (montoYaPagado + monto > factura.getImporteTotal()) {
            escribirJson(response, 400, ArquitaUtils.armarJsonError(
                    "El pago supera el saldo pendiente de la factura"));
            return;
        }

        Pago pago = facturacionManager.registrarPago(facturaId, monto, medioPago);

        if (pago == null) {
            escribirJson(response, 400, ArquitaUtils.armarJsonError("No se pudo registrar el pago"));
            return;
        }

        String json = "{\"id\":" + pago.getId() + ",\"monto\":" + pago.getMonto()
                + ",\"estado\":\"" + pago.getEstado() + "\"}";
        escribirJson(response, 200, json);
    }

    private void escribirJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
    }
}
