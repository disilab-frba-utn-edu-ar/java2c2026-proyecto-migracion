package com.arquita.legacy.dao;

import java.util.List;

import com.arquita.legacy.dominio.Factura;

public interface FacturaDAO {

    Factura buscarPorId(Long id);

    List<Factura> buscarPorCuitReceptor(String cuit);

    List<Factura> buscarTodas();

    List<Factura> buscarFacturasVencidasHastaHoy();

    void guardar(Factura factura);

    void actualizar(Factura factura);

    void eliminar(Long id);

    List<Factura> ejecutarConsultaSql(String sqlWhereClause);
}
