package com.example.devoluciones_incidencias.servicio;

import com.example.devoluciones_incidencias.cliente.ClienteInventario;
import com.example.devoluciones_incidencias.cliente.ClienteUsuarios;
import com.example.devoluciones_incidencias.modelo.Devolucion;
import com.example.devoluciones_incidencias.modelo.DevolucionRequest;
import com.example.devoluciones_incidencias.modelo.EstadoMaterial;
import com.example.devoluciones_incidencias.persistencia.DevolucionEntity;
import com.example.devoluciones_incidencias.persistencia.DevolucionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
public class DevolucionService {

    private final DevolucionRepository repositorio;
    private final IdempotenciaService idempotenciaService;
    private final ClienteInventario clienteInventario;
    private final ClienteUsuarios clienteUsuarios;
    private final ObjectMapper objectMapper;

    public DevolucionService(
            DevolucionRepository repositorio,
            IdempotenciaService idempotenciaService,
            ClienteInventario clienteInventario,
            ClienteUsuarios clienteUsuarios,
            ObjectMapper objectMapper) {
        this.repositorio = repositorio;
        this.idempotenciaService = idempotenciaService;
        this.clienteInventario = clienteInventario;
        this.clienteUsuarios = clienteUsuarios;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<Devolucion> listar() {
        return repositorio.findAll(Sort.by("id"))
                .stream()
                .map(this::convertir)
                .toList();
    }

    @Transactional(readOnly = true)
    public Devolucion consultar(Long id) {
        DevolucionEntity entidad = buscarPorId(id);
        return convertir(entidad);
    }

    @Transactional(readOnly = true)
    public Devolucion consultarPorFolio(String folio) {
        return repositorio.findByFolio(folio)
                .map(this::convertir)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe una devolucion con el folio " + folio));
    }

    public List<com.example.devoluciones_incidencias.cliente.ArticuloInventarioDto> listarArticulosInventario() {
        return clienteInventario.obtenerArticulos();
    }

    public com.example.devoluciones_incidencias.cliente.ArticuloInventarioDto consultarArticuloInventario(Long id) {
        return clienteInventario.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Articulo no encontrado en inventario"));
    }

    public Object consultarUsuario(Long id) {
        return clienteUsuarios.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @Transactional
    public Devolucion registrar(DevolucionRequest solicitud) {
        // Version debe ser 0 al crear
        if (!Long.valueOf(0L).equals(solicitud.version())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Para registrar una devolucion, la version debe ser 0");
        }

        UUID claveUuid = UUID.fromString(solicitud.claveIdempotencia());
        String huella = idempotenciaService.calcularHuella(solicitud);
        // System.out.println("DEBUG huella calculada: " + huella);

        // Verificar si la peticion ya se habia procesado previamente
        Optional<String> respuestaPrevia = idempotenciaService.verificarOIniciar(claveUuid, huella);
        if (respuestaPrevia.isPresent()) {
            try {
                return objectMapper.readValue(respuestaPrevia.get(), Devolucion.class);
            } catch (Exception e) {
                return repositorio.findByFolio("DEV-" + claveUuid.toString().substring(0, 8).toUpperCase())
                        .map(this::convertir)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Error al recuperar la respuesta previa"));
            }
        }

        try {
            String folio = "DEV-" + claveUuid.toString().substring(0, 8).toUpperCase();

            // Cuota de penalizacion acordada: si el material es irreparable y no traia
            // monto, se fija en $250
            BigDecimal sancionFinal = solicitud.montoSancion();
            if (solicitud.estadoMaterial() == EstadoMaterial.DANADO_IRREPARABLE
                    && sancionFinal.compareTo(BigDecimal.ZERO) == 0) {
                sancionFinal = new BigDecimal("250.00");
            }

            DevolucionEntity entidad = new DevolucionEntity(
                    folio,
                    solicitud.idSolicitud(),
                    solicitud.codigoArticulo().trim().toUpperCase(),
                    solicitud.idUsuario(),
                    Instant.now(),
                    solicitud.estadoMaterial(),
                    solicitud.observaciones().trim(),
                    sancionFinal);

            DevolucionEntity guardada = repositorio.saveAndFlush(entidad);

            // Comunicacion SOA REST con Inventario Deportivo (Servicio 1)
            clienteInventario.registrarAfectacionInventario(
                    guardada.getCodigoArticulo(),
                    guardada.getEstadoMaterial());

            // Si se reportaron danos o mantenimiento, notificar al servicio de Usuarios
            // (Servicio 3)
            if (guardada.getEstadoMaterial() != EstadoMaterial.BUEN_ESTADO) {
                clienteUsuarios.notificarIncidencia(
                        guardada.getIdUsuario(),
                        claveUuid,
                        guardada.getEstadoMaterial(),
                        guardada.getObservaciones());
            }

            Devolucion resultado = convertir(guardada);

            // Guardar respuesta para reintentos idempotentes con el mismo UUID
            try {
                String jsonRespuesta = objectMapper.writeValueAsString(resultado);
                idempotenciaService.registrarResultado(claveUuid, HttpStatus.CREATED.value(), jsonRespuesta);
            } catch (Exception ex) {
                // No abortar transaccion si solo falla el cache de serializacion
            }

            return resultado;

        } catch (Exception ex) {
            idempotenciaService.registrarFallo(claveUuid, HttpStatus.BAD_REQUEST.value(), ex.getMessage());
            throw ex;
        }
    }

    @Transactional
    public Devolucion cancelar(Long id, Long version) {
        DevolucionEntity entidad = buscarPorId(id);

        if (!Objects.equals(entidad.getVersion(), version)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La devolucion fue modificada previamente. Consulta su version actual e intenta nuevamente");
        }

        entidad.actualizarObservaciones("[CANCELADA] " + entidad.getObservaciones());
        DevolucionEntity guardada = repositorio.saveAndFlush(entidad);
        return convertir(guardada);
    }

    private DevolucionEntity buscarPorId(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador debe ser mayor que 0");
        }

        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe una devolucion con el identificador " + id));
    }

    private Devolucion convertir(DevolucionEntity entidad) {
        return new Devolucion(
                entidad.getId(),
                entidad.getFolio(),
                entidad.getIdSolicitud(),
                entidad.getCodigoArticulo(),
                entidad.getIdUsuario(),
                entidad.getFechaDevolucion(),
                entidad.getEstadoMaterial(),
                entidad.getObservaciones(),
                entidad.getMontoSancion(),
                entidad.getVersion());
    }
}
