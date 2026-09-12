package com.arquita.legacy.dao;

import com.arquita.legacy.dominio.Contribuyente;

public interface ContribuyenteDAO {

    Contribuyente buscarPorCuit(String cuit);

    void guardar(Contribuyente contribuyente);
}
