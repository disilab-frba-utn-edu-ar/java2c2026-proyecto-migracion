package com.arquita.legacy.dominio;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "DETALLE_FACTURA")
public class DetalleFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "DESCRIPCION")
    private String descripcion;

    @Column(name = "CANTIDAD")
    private double cantidad;

    @Column(name = "PRECIO_UNITARIO")
    private double precioUnitario;

    @Column(name = "ALICUOTA_IVA")
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
