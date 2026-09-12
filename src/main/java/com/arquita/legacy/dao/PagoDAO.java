package com.arquita.legacy.dao;

import java.util.List;

import com.arquita.legacy.dominio.Pago;

public interface PagoDAO {

    Pago buscarPorId(Long id);

    List<Pago> buscarPorFacturaId(Long facturaId);

    void guardar(Pago pago);
}
