package com.example.devoluciones_incidencias.soap;

import com.example.devoluciones_incidencias.modelo.Devolucion;
import com.example.devoluciones_incidencias.modelo.EstadoMaterial;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Componente que demuestra el punto de transformacion entre modelos
 * REST/JSON y el formato SOAP/XML (Requisito 8 de la rubrica SOA).
 */
@Component
public class TransformadorSoapJson {

    public ConsultarIncidenciaResponse jsonToSoap(Devolucion devolucion) {
        boolean reposicion = devolucion.estadoMaterial() == EstadoMaterial.DANADO_IRREPARABLE;
        String dictamen = (reposicion)
                ? "Dano total registrado. Requiere adquisicion de nuevo activo."
                : "Inspeccion concluida sin necesidad de baja de activo.";

        return new ConsultarIncidenciaResponse(
                devolucion.folio(),
                devolucion.codigoArticulo(),
                devolucion.estadoMaterial().name(),
                devolucion.observaciones(),
                devolucion.montoSancion(),
                reposicion,
                dictamen
        );
    }

    public Map<String, Object> soapToJson(ConsultarIncidenciaResponse soapResponse) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("folio", soapResponse.getFolio());
        json.put("articulo", soapResponse.getCodigoArticulo());
        json.put("condicion", soapResponse.getEstadoMaterial());
        json.put("comentarios", soapResponse.getObservaciones());
        json.put("penalizacion", soapResponse.getMontoSancion());
        json.put("requiereReposicion", soapResponse.isRequiereReposicion());
        json.put("dictamenTecnico", soapResponse.getMensajeDictamen());
        json.put("protocoloOrigen", "SOAP_XML");
        json.put("protocoloDestino", "REST_JSON");
        return json;
    }
}

