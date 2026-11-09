package com.example.devoluciones_incidencias.soap;

import com.example.devoluciones_incidencias.modelo.Devolucion;
import com.example.devoluciones_incidencias.modelo.EstadoMaterial;
import com.example.devoluciones_incidencias.servicio.DevolucionService;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class IncidenciaSoapEndpoint {

    private static final String NAMESPACE_URI = "http://ejemplo.com/devoluciones/soap";

    private final DevolucionService servicio;

    public IncidenciaSoapEndpoint(DevolucionService servicio) {
        this.servicio = servicio;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "ConsultarIncidenciaRequest")
    @ResponsePayload
    public ConsultarIncidenciaResponse consultarIncidencia(@RequestPayload ConsultarIncidenciaRequest request) {
        Devolucion devolucion = servicio.consultarPorFolio(request.getFolio());

        boolean reposicion = devolucion.estadoMaterial() == EstadoMaterial.DANADO_IRREPARABLE;
        String dictamen = switch (devolucion.estadoMaterial()) {
            case BUEN_ESTADO -> "El material fue devuelto en condiciones optimas. No hay incidencias.";
            case REQUIERE_MANTENIMIENTO -> "El material requiere servicio preventivo/correctivo antes de nuevo prestamo.";
            case DANADO_IRREPARABLE -> "Incidencia grave: equipo inutilizable. Requiere reposicion obligatoria y sancion monetaria.";
        };

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
}

