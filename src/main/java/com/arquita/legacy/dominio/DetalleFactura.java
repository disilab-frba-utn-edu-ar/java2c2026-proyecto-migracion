package com.arquita.legacy.dominio;

import java.io.Serializable;

/**
 * POJO mapeado por XML: ver src/main/resources/mapeo/DetalleFactura.hbm.xml.
 * Sin anotaciones de persistencia a proposito -- asi se mapeaba antes de JPA.
 */
public class DetalleFactura implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String descripcion;

    private double cantidad;

    private double precioUnitario;

    private double alicuotaIva;

    public DetalleFactura() {
    }

    public DetalleFactura(String descripcion, double cantidad, double precioUnitario, double alicuotaIva) {
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.alicuotaIva = alicuotaIva;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getCantidad() {
        return cantidad;
    }

    public void setCantidad(double cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public double getAlicuotaIva() {
        return alicuotaIva;
    }

    public void setAlicuotaIva(double alicuotaIva) {
        this.alicuotaIva = alicuotaIva;
    }
}
