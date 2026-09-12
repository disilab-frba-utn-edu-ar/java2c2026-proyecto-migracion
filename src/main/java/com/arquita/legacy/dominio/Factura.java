package com.arquita.legacy.dominio;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "FACTURA")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "NUMERO")
    private Long numero;

    @Column(name = "PUNTO_VENTA")
    private Integer puntoVenta;

    @Column(name = "TIPO_COMPROBANTE")
    private int tipoComprobante;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "FECHA")
    private Date fecha;

    @Column(name = "CUIT_EMISOR")
    private String cuitEmisor;

    @Column(name = "CUIT_RECEPTOR")
    private String cuitReceptor;

    @Column(name = "IMPORTE_NETO")
    private double importeNeto;

    @Column(name = "IMPORTE_IVA")
    private double importeIva;

    @Column(name = "IMPORTE_TOTAL")
    private double importeTotal;

    @Column(name = "MONEDA")
    private String moneda = Moneda.ARS;

    @Column(name = "COTIZACION_AL_EMITIR")
    private Double cotizacionAlEmitir;

    @Column(name = "ESTADO")
    private int estado = EstadoFactura.BORRADOR;

    @Column(name = "CAE")
    private String cae;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CAE_VENCIMIENTO")
    private Date caeVencimiento;

    @OneToMany(cascade = CascadeType.ALL)
    @javax.persistence.JoinColumn(name = "FACTURA_ID")
    private List<DetalleFactura> detalles = new ArrayList<DetalleFactura>();

    public Factura() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(int tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public String getCuitEmisor() {
        return cuitEmisor;
    }

    public void setCuitEmisor(String cuitEmisor) {
        this.cuitEmisor = cuitEmisor;
    }

    public String getCuitReceptor() {
        return cuitReceptor;
    }

    public void setCuitReceptor(String cuitReceptor) {
        this.cuitReceptor = cuitReceptor;
    }

    public double getImporteNeto() {
        return importeNeto;
    }

    public void setImporteNeto(double importeNeto) {
        this.importeNeto = importeNeto;
    }

    public double getImporteIva() {
        return importeIva;
    }

    public void setImporteIva(double importeIva) {
        this.importeIva = importeIva;
    }

    public double getImporteTotal() {
        return importeTotal;
    }

    public void setImporteTotal(double importeTotal) {
        this.importeTotal = importeTotal;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public Double getCotizacionAlEmitir() {
        return cotizacionAlEmitir;
    }

    public void setCotizacionAlEmitir(Double cotizacionAlEmitir) {
        this.cotizacionAlEmitir = cotizacionAlEmitir;
    }

    public double getImporteTotalEnPesos() {
        if (Moneda.USD.equals(moneda) && cotizacionAlEmitir != null) {
            return importeTotal * cotizacionAlEmitir;
        }
        return importeTotal;
    }

    public int getEstado() {
        return estado;
    }

    public void setEstado(int estado) {
        this.estado = estado;
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

    public List<DetalleFactura> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleFactura> detalles) {
        this.detalles = detalles;
    }

    public double getImporteTotalRecalculado() {
        double total = 0;
        for (int i = 0; i < detalles.size(); i++) {
            DetalleFactura d = detalles.get(i);
            double subtotal = d.getCantidad() * d.getPrecioUnitario();
            double iva = subtotal * (d.getAlicuotaIva() / 100.0);
            total = total + subtotal + iva;
        }
        return total;
    }
}
