package com.arquita.legacy.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.arquita.legacy.dominio.Contribuyente;
import com.arquita.legacy.persistencia.HibernateUtil;
import com.arquita.legacy.util.ArquitaUtils;

/**
 * Antes era ContribuyenteController + ContribuyenteDAO. Ahora es un unico
 * servlet plano, declarado a mano en web.xml (sin @WebServlet), que abre
 * su propio EntityManager y maneja su propia transaccion por request.
 *
 * Mapeado a /api/contribuyentes/*; el recurso se resuelve con getPathInfo().
 */
public class ContribuyenteServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if (!"/buscar".equals(pathInfo)) {
            escribirJson(response, 404, ArquitaUtils.armarJsonError("Recurso no encontrado"));
            return;
        }

        String cuit = request.getParameter("cuit");

        if (!ArquitaUtils.esCuitValido(cuit)) {
            escribirJson(response, 400, ArquitaUtils.armarJsonError("CUIT invalido"));
            return;
        }

        EntityManager em = HibernateUtil.getEntityManager();
        try {
            // Concatenacion de strings para armar la consulta, a proposito:
            // asi era costumbre antes de los parametros nombrados (y asi es
            // vulnerable a inyeccion HQL).
            @SuppressWarnings("unchecked")
            List<Contribuyente> resultados = em.createQuery(
                    "from Contribuyente c where c.cuit = '" + cuit + "'").getResultList();

            Contribuyente contribuyente = resultados.isEmpty() ? null : resultados.get(0);

            if (contribuyente == null) {
                escribirJson(response, 404, ArquitaUtils.armarJsonError("Contribuyente no encontrado"));
                return;
            }

            String json = "{\"cuit\":\"" + contribuyente.getCuit() + "\","
                    + "\"razonSocial\":\"" + contribuyente.getRazonSocial() + "\","
                    + "\"condicionIva\":\"" + contribuyente.getCondicionIva() + "\"}";
            escribirJson(response, 200, json);
        } finally {
            em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if (!"/alta".equals(pathInfo)) {
            escribirJson(response, 404, ArquitaUtils.armarJsonError("Recurso no encontrado"));
            return;
        }

        String cuit = request.getParameter("cuit");
        String razonSocial = request.getParameter("razonSocial");
        String condicionIva = request.getParameter("condicionIva");

        if (!ArquitaUtils.esCuitValido(cuit)) {
            escribirJson(response, 400, ArquitaUtils.armarJsonError("CUIT invalido"));
            return;
        }

        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            // Sin chequeo de duplicados, igual que antes: dos altas con el
            // mismo CUIT generan dos filas distintas.
            Contribuyente contribuyente = new Contribuyente();
            contribuyente.setCuit(cuit);
            contribuyente.setRazonSocial(razonSocial);
            contribuyente.setCondicionIva(condicionIva);
            em.persist(contribuyente);

            em.getTransaction().commit();
            escribirJson(response, 200, "{\"resultado\":\"ok\"}");
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            ArquitaUtils.log("No se pudo dar de alta el contribuyente: " + e.getMessage());
            escribirJson(response, 400, ArquitaUtils.armarJsonError("No se pudo dar de alta el contribuyente"));
        } finally {
            em.close();
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
