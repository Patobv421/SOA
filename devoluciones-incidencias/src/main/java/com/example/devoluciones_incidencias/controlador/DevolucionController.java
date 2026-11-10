package com.example.devoluciones_incidencias.controlador;

import com.example.devoluciones_incidencias.modelo.Devolucion;
import com.example.devoluciones_incidencias.modelo.DevolucionRequest;
import com.example.devoluciones_incidencias.servicio.DevolucionService;
import com.example.devoluciones_incidencias.soap.ConsultarIncidenciaRequest;
import com.example.devoluciones_incidencias.soap.ConsultarIncidenciaResponse;
import com.example.devoluciones_incidencias.soap.IncidenciaSoapEndpoint;
import com.example.devoluciones_incidencias.soap.TransformadorSoapJson;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devoluciones")
@CrossOrigin(origins = {
        "http://localhost:5500",
        "http://127.0.0.1:5500",
        "*"
})
public class DevolucionController {

    private final DevolucionService servicio;
    private final IncidenciaSoapEndpoint soapEndpoint;
    private final TransformadorSoapJson transformadorSoap;

    public DevolucionController(
            DevolucionService servicio,
            IncidenciaSoapEndpoint soapEndpoint,
            TransformadorSoapJson transformadorSoap
    ) {
        this.servicio = servicio;
        this.soapEndpoint = soapEndpoint;
        this.transformadorSoap = transformadorSoap;
    }

    @GetMapping
    public List<Devolucion> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public Devolucion consultar(@PathVariable("id") Long id) {
        return servicio.consultar(id);
    }

    @GetMapping("/folio/{folio}")
    public Devolucion consultarPorFolio(@PathVariable("folio") String folio) {
        return servicio.consultarPorFolio(folio);
    }

    @GetMapping("/inventario")
    public List<com.example.devoluciones_incidencias.cliente.ArticuloInventarioDto> listarArticulosInventario() {
        return servicio.listarArticulosInventario();
    }

    @GetMapping("/inventario/{id}")
    public com.example.devoluciones_incidencias.cliente.ArticuloInventarioDto consultarArticuloInventario(@PathVariable("id") Long id) {
        return servicio.consultarArticuloInventario(id);
    }

    @GetMapping("/usuarios/{id}")
    public Object consultarUsuario(@PathVariable("id") Long id) {
        return servicio.consultarUsuario(id);
    }

    @PostMapping
    public ResponseEntity<Devolucion> registrar(@Valid @RequestBody DevolucionRequest solicitud) {
        Devolucion creada = servicio.registrar(solicitud);
        URI ubicacion = URI.create("/api/devoluciones/" + creada.id());
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @PutMapping("/{id}/cancelar")
    public Devolucion cancelar(
            @PathVariable("id") Long id,
            @RequestParam("version") Long version
    ) {
        return servicio.cancelar(id, version);
    }

    /**
     * Endpoint de transformacion:
     * llama a la operacion del endpoint SOAP y transforma la respuesta XML a JSON.
     */
    @GetMapping("/transformacion-soap/{folio}")
    public Map<String, Object> consultarViaSoapYTransformarAJson(@PathVariable("folio") String folio) {
        ConsultarIncidenciaRequest soapRequest = new ConsultarIncidenciaRequest(folio);
        ConsultarIncidenciaResponse soapResponse = soapEndpoint.consultarIncidencia(soapRequest);
        return transformadorSoap.soapToJson(soapResponse);
    }
}

