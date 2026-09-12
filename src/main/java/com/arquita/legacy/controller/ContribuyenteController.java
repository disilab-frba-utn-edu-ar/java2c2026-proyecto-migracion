package com.arquita.legacy.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.arquita.legacy.dao.ContribuyenteDAO;
import com.arquita.legacy.dominio.Contribuyente;
import com.arquita.legacy.util.ArquitaUtils;

@Controller
@RequestMapping("/contribuyentes")
public class ContribuyenteController {

    @Autowired
    private ContribuyenteDAO contribuyenteDAO;

    @RequestMapping(value = "/buscar", method = RequestMethod.GET)
    public void buscar(@RequestParam String cuit, HttpServletResponse response) throws IOException {
        if (!ArquitaUtils.esCuitValido(cuit)) {
            escribirJson(response, 400, ArquitaUtils.armarJsonError("CUIT invalido"));
            return;
        }

        Contribuyente contribuyente = contribuyenteDAO.buscarPorCuit(cuit);
        if (contribuyente == null) {
            escribirJson(response, 404, ArquitaUtils.armarJsonError("Contribuyente no encontrado"));
            return;
        }

        String json = "{\"cuit\":\"" + contribuyente.getCuit() + "\","
                + "\"razonSocial\":\"" + contribuyente.getRazonSocial() + "\","
                + "\"condicionIva\":\"" + contribuyente.getCondicionIva() + "\"}";
        escribirJson(response, 200, json);
    }

    @RequestMapping(value = "/alta", method = RequestMethod.POST)
    public void alta(@RequestParam String cuit,
                      @RequestParam String razonSocial,
                      @RequestParam String condicionIva,
                      HttpServletResponse response) throws IOException {
        if (!ArquitaUtils.esCuitValido(cuit)) {
            escribirJson(response, 400, ArquitaUtils.armarJsonError("CUIT invalido"));
            return;
        }
        Contribuyente contribuyente = new Contribuyente();
        contribuyente.setCuit(cuit);
        contribuyente.setRazonSocial(razonSocial);
        contribuyente.setCondicionIva(condicionIva);
        contribuyenteDAO.guardar(contribuyente);
        escribirJson(response, 200, "{\"resultado\":\"ok\"}");
    }

    private void escribirJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
    }
}
