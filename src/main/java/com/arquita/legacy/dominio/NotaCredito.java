package com.arquita.legacy.dominio;

import java.io.Serializable;
import java.util.Date;

/**
 * POJO mapeado por XML: ver src/main/resources/mapeo/NotaCredito.hbm.xml.
 * Sin anotaciones de persistencia a proposito -- asi se mapeaba antes de JPA.
 */
public class NotaCredito implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long facturaOriginalId;

    private Long numero;

    private Integer puntoVenta;

    private Date fecha;

    private String motivo;

    private double importe;

    private String cae;

    private Date caeVencimiento;

    public NotaCredito() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFacturaOriginalId() {
        return facturaOriginalId;
    }

    public void setFacturaOriginalId(Long facturaOriginalId) {
        this.facturaOriginalId = facturaOriginalId;
    }

    public Long getNumero() {
        return numero;
    }

    public void setNumero(Long numero) {
        this.numero = numero;
    }

    public Integer getPuntoVenta() {
        return puntoVenta;
    }

    public void setPuntoVenta(Integer puntoVenta) {
        this.puntoVenta = puntoVenta;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public double getImporte() {
        return importe;
    }

    public void setImporte(double importe) {
        this.importe = importe;
    }

    public String getCae() {
        return cae;
    }

    public void setCae(String cae) {
        this.cae = cae;
    }

    public Date getCaeVencimiento() {
        return caeVencimiento;
    }

    public void setCaeVencimiento(Date caeVencimiento) {
        this.caeVencimiento = caeVencimiento;
    }
}
