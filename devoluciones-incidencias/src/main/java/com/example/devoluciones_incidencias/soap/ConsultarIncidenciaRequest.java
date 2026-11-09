package com.example.devoluciones_incidencias.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ConsultarIncidenciaRequest", namespace = "http://ejemplo.com/devoluciones/soap")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConsultarIncidenciaRequest {

    @XmlElement(namespace = "http://ejemplo.com/devoluciones/soap", required = true)
    private String folio;

    public ConsultarIncidenciaRequest() {
    }

    public ConsultarIncidenciaRequest(String folio) {
        this.folio = folio;
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }
}

