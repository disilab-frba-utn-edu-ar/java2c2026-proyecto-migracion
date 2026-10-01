package com.arquita.legacy.dominio;

import java.io.Serializable;

/**
 * POJO mapeado por XML: ver src/main/resources/mapeo/Contribuyente.hbm.xml.
 * Sin anotaciones de persistencia a proposito -- asi se mapeaba antes de JPA.
 */
public class Contribuyente implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String cuit;

    private String razonSocial;

    private String condicionIva;

    private String domicilioFiscal;

    private int activo = 1;

    public Contribuyente() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCuit() {
        return cuit;
    }

    public void setCuit(String cuit) {
        this.cuit = cuit;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getCondicionIva() {
        return condicionIva;
    }

    public void setCondicionIva(String condicionIva) {
        this.condicionIva = condicionIva;
    }

    public String getDomicilioFiscal() {
        return domicilioFiscal;
    }

    public void setDomicilioFiscal(String domicilioFiscal) {
        this.domicilioFiscal = domicilioFiscal;
    }

    public int getActivo() {
        return activo;
    }

    public void setActivo(int activo) {
        this.activo = activo;
    }
}
