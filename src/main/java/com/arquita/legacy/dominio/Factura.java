package com.arquita.legacy.dominio;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * POJO mapeado por XML: ver src/main/resources/mapeo/Factura.hbm.xml.
 * Sin anotaciones de persistencia a proposito -- asi se mapeaba antes de JPA.
 */
public class Factura implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long numero;

    private Integer puntoVenta;

    private int tipoComprobante;

    private Date fecha;

    private String cuitEmisor;

    private String cuitReceptor;

    private double importeNeto;

    private double importeIva;

    private double importeTotal;

    private String moneda = Moneda.ARS;

    private Double cotizacionAlEmitir;

    private int estado = EstadoFactura.BORRADOR;

    private String cae;

    private Date caeVencimiento;

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
