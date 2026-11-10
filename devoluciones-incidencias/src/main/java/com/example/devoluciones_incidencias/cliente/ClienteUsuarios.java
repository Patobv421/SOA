package com.example.devoluciones_incidencias.cliente;

import com.example.devoluciones_incidencias.modelo.EstadoMaterial;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ClienteUsuarios {

    private static final Logger log = LoggerFactory.getLogger(ClienteUsuarios.class);

    private final RestTemplate restTemplate;
    private final String urlUsuarios;

    public ClienteUsuarios(
            @Value("${servicios.usuarios.url:http://127.0.0.1:8083/api/usuarios}") String urlUsuarios,
            @Value("${servicios.usuarios.timeout:4000}") int timeoutMs
    ) {
        this.urlUsuarios = urlUsuarios;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(factory);
    }

    public Optional<Object> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        try {
            ResponseEntity<Object> respuesta = restTemplate.getForEntity(urlUsuarios + "/" + id, Object.class);
            return Optional.ofNullable(respuesta.getBody());
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            log.warn("Error HTTP {} al consultar usuario {}", ex.getStatusCode(), id);
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("No se pudo consultar usuario {} en servicio de usuarios: {}", id, ex.getMessage());
            return Optional.empty();
        }
    }

    public void notificarIncidencia(Long idUsuario, UUID idOperacion, EstadoMaterial estado, String observaciones) {
        if (idUsuario == null || estado == EstadoMaterial.BUEN_ESTADO) {
            return;
        }

        String gravedad = (estado == EstadoMaterial.DANADO_IRREPARABLE) ? "GRAVE" : "LEVE";
        String detalle = (observaciones != null && !observaciones.isBlank())
                ? observaciones.trim()
                : "Incidencia por devolucion de material con estado: " + estado.name();

        IncidenciaUsuarioRequest peticion = new IncidenciaUsuarioRequest(idOperacion, gravedad, detalle);
        String urlDestino = urlUsuarios + "/" + idUsuario + "/incidencias";

        try {
            restTemplate.postForEntity(urlDestino, peticion, Object.class);
            log.info("Incidencia notificada al servicio de Usuarios para el usuario {}. Gravedad: {}", idUsuario, gravedad);
        } catch (HttpClientErrorException.NotFound ex) {
            log.error("El usuario {} no existe en el servicio de Usuarios: {}", idUsuario, ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El usuario con id " + idUsuario + " no existe en el sistema de usuarios"
            );
        } catch (HttpClientErrorException.Conflict ex) {
            log.warn("El reintento con UUID {} fue aceptado o detectado con conflicto por usuarios: {}", idOperacion, ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El UUID de operacion ya fue registrado en el servicio de usuarios con otros datos"
            );
        } catch (ResourceAccessException ex) {
            log.error("Fallo de conexion con el servicio de Usuarios (Servicio 3): {}", ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo sincronizar con el servicio de usuarios porque no responde o se agoto el tiempo de espera"
            );
        } catch (HttpClientErrorException ex) {
            log.error("El servicio de usuarios rechazo la incidencia: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ResponseStatusException(
                    ex.getStatusCode(),
                    "El servicio de usuarios rechazo la incidencia reportada"
            );
        }
    }
}

