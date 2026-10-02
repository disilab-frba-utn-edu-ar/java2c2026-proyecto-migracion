package com.arquita.legacy.dominio;

import java.io.Serializable;
import java.util.Date;

/**
 * POJO mapeado por XML: ver src/main/resources/mapeo/Pago.hbm.xml.
 * Sin anotaciones de persistencia a proposito -- asi se mapeaba antes de JPA.
 */
public class Pago implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long facturaId;

    private Date fecha;

    private double monto;

    private String medioPago;

    private String estado;

    public Pago() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFacturaId() {
        return facturaId;
    }

    public void setFacturaId(Long facturaId) {
        this.facturaId = facturaId;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getMedioPago() {
        return medioPago;
    }

    public void setMedioPago(String medioPago) {
        this.medioPago = medioPago;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
