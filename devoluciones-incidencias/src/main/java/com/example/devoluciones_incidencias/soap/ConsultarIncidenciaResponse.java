package com.example.devoluciones_incidencias.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;

@XmlRootElement(name = "ConsultarIncidenciaResponse", namespace = "http://ejemplo.com/devoluciones/soap")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConsultarIncidenciaResponse {

    @XmlElement(namespace = "http://ejemplo.com/devoluciones/soap")
    private String folio;

    @XmlElement(namespace = "http://ejemplo.com/devoluciones/soap")
    private String codigoArticulo;

    @XmlElement(namespace = "http://ejemplo.com/devoluciones/soap")
    private String estadoMaterial;

    @XmlElement(namespace = "http://ejemplo.com/devoluciones/soap")
    private String observaciones;

    @XmlElement(namespace = "http://ejemplo.com/devoluciones/soap")
    private BigDecimal montoSancion;

    @XmlElement(namespace = "http://ejemplo.com/devoluciones/soap")
    private boolean requiereReposicion;

    @XmlElement(namespace = "http://ejemplo.com/devoluciones/soap")
    private String mensajeDictamen;

    public ConsultarIncidenciaResponse() {
    }

    public ConsultarIncidenciaResponse(
            String folio,
            String codigoArticulo,
            String estadoMaterial,
            String observaciones,
            BigDecimal montoSancion,
            boolean requiereReposicion,
            String mensajeDictamen
    ) {
        this.folio = folio;
        this.codigoArticulo = codigoArticulo;
        this.estadoMaterial = estadoMaterial;
        this.observaciones = observaciones;
        this.montoSancion = montoSancion;
        this.requiereReposicion = requiereReposicion;
        this.mensajeDictamen = mensajeDictamen;
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public String getCodigoArticulo() {
        return codigoArticulo;
    }

    public void setCodigoArticulo(String codigoArticulo) {
        this.codigoArticulo = codigoArticulo;
    }

    public String getEstadoMaterial() {
        return estadoMaterial;
    }

    public void setEstadoMaterial(String estadoMaterial) {
        this.estadoMaterial = estadoMaterial;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public BigDecimal getMontoSancion() {
        return montoSancion;
    }

    public void setMontoSancion(BigDecimal montoSancion) {
        this.montoSancion = montoSancion;
    }

    public boolean isRequiereReposicion() {
        return requiereReposicion;
    }

    public void setRequiereReposicion(boolean requiereReposicion) {
        this.requiereReposicion = requiereReposicion;
    }

    public String getMensajeDictamen() {
        return mensajeDictamen;
    }

    public void setMensajeDictamen(String mensajeDictamen) {
        this.mensajeDictamen = mensajeDictamen;
    }
}

