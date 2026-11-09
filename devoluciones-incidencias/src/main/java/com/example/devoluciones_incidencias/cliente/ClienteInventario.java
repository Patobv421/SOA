package com.example.devoluciones_incidencias.cliente;

import com.example.devoluciones_incidencias.modelo.EstadoMaterial;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
public class ClienteInventario {

    private static final Logger log = LoggerFactory.getLogger(ClienteInventario.class);

    private final RestTemplate restTemplate;
    private final String urlInventario;

    public ClienteInventario(
            @Value("${servicios.inventario.url:http://127.0.0.1:9000/api/articulos}") String urlInventario,
            @Value("${servicios.inventario.timeout:4000}") int timeoutMs
    ) {
        this.urlInventario = urlInventario;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(factory);
    }

    public List<ArticuloInventarioDto> obtenerArticulos() {
        try {
            ResponseEntity<ArticuloInventarioDto[]> respuesta =
                    restTemplate.getForEntity(urlInventario, ArticuloInventarioDto[].class);

            if (respuesta.getBody() != null) {
                return Arrays.asList(respuesta.getBody());
            }
            return List.of();
        } catch (ResourceAccessException ex) {
            log.error("Fallo de conexion con el servicio de Inventario (Servicio 1): {}", ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de inventario deportivo no esta disponible o no respondio a tiempo"
            );
        } catch (HttpClientErrorException ex) {
            log.warn("Error devuelto por inventario al consultar articulos: {}", ex.getStatusCode());
            throw new ResponseStatusException(
                    ex.getStatusCode(),
                    "Error al consultar articulos en inventario: " + ex.getResponseBodyAsString()
            );
        }
    }

    public Optional<ArticuloInventarioDto> buscarPorCodigo(String codigo) {
        List<ArticuloInventarioDto> articulos = obtenerArticulos();
        return articulos.stream()
                .filter(a -> a.codigo().equalsIgnoreCase(codigo.trim()))
                .findFirst();
    }

    public Optional<ArticuloInventarioDto> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        try {
            ResponseEntity<ArticuloInventarioDto> respuesta =
                    restTemplate.getForEntity(urlInventario + "/" + id, ArticuloInventarioDto.class);
            return Optional.ofNullable(respuesta.getBody());
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("No se pudo consultar articulo {} por ID en inventario: {}", id, ex.getMessage());
            return Optional.empty();
        }
    }

    public void registrarAfectacionInventario(String codigoArticulo, EstadoMaterial estado) {
        // En BUEN_ESTADO el articulo se conserva optimo, no requiere reclasificacion en inventario
        if (estado == EstadoMaterial.BUEN_ESTADO) {
            return;
        }

        ArticuloInventarioDto articulo = buscarPorCodigo(codigoArticulo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El articulo con codigo " + codigoArticulo + " no existe en el catalogo de inventario"
                ));

        int buenEstado = articulo.cantidadBuenEstado();
        int mantenimiento = articulo.cantidadMantenimiento();
        int danada = articulo.cantidadDanada();

        if (buenEstado <= 0) {
            log.warn("El articulo {} no cuenta con existencias en buen estado para reclasificar", codigoArticulo);
        } else {
            buenEstado -= 1;
        }

        if (estado == EstadoMaterial.REQUIERE_MANTENIMIENTO) {
            mantenimiento += 1;
        } else if (estado == EstadoMaterial.DANADO_IRREPARABLE) {
            danada += 1;
        }

        ArticuloActualizacionRequest solicitudActualizacion = new ArticuloActualizacionRequest(
                articulo.codigo(),
                articulo.nombre(),
                articulo.deporte(),
                articulo.descripcion(),
                buenEstado,
                mantenimiento,
                danada,
                articulo.version()
        );

        String urlActualizar = urlInventario + "/" + articulo.id();

        try {
            restTemplate.put(urlActualizar, solicitudActualizacion);
            log.info("Inventario deportivo actualizado exitosamente para el articulo {}. Nuevo estado: {}", codigoArticulo, estado);
        } catch (ResourceAccessException ex) {
            log.error("No se pudo actualizar el inventario debido a desconexion: {}", ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo sincronizar con inventario deportivo porque el servicio no responde"
            );
        } catch (HttpClientErrorException ex) {
            log.error("Inventario rechazo la actualizacion: {}", ex.getStatusCode());
            throw new ResponseStatusException(
                    ex.getStatusCode(),
                    "El servicio de inventario rechazo la actualizacion del articulo"
            );
        }
    }
}
