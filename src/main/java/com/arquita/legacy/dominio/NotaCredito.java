package com.arquita.legacy.dominio;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "NOTA_CREDITO")
public class NotaCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "FACTURA_ORIGINAL_ID")
    private Long facturaOriginalId;

    @Column(name = "NUMERO")
    private Long numero;

    @Column(name = "PUNTO_VENTA")
    private Integer puntoVenta;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "FECHA")
    private Date fecha;

    @Column(name = "MOTIVO")
    private String motivo;

    @Column(name = "IMPORTE")
    private double importe;

    @Column(name = "CAE")
    private String cae;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CAE_VENCIMIENTO")
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
