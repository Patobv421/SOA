package com.proyecto.solicitudes.servicio;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.proyecto.solicitudes.modelo.ArticuloRespuesta;

// Se comunica con el servicio de Inventario deportivo por REST
@Component
public class ClienteInventario {

	private final RestClient restClient;

	public ClienteInventario(@Value("${servicios.inventario.url}") String url,
			@Value("${servicios.tiempo-espera-segundos}") int segundosEspera) {
		this.restClient = ConfiguracionRestClient.crear(url, segundosEspera);
	}

	// Pregunta al inventario por un articulo y sus existencias
	public ArticuloRespuesta obtenerArticulo(Long idArticulo) {
		try {
			ArticuloRespuesta articulo = restClient.get()
				.uri("/api/articulos/{id}", idArticulo)
				.retrieve()
				.body(ArticuloRespuesta.class);
			if (articulo == null) {
				throw new ErrorNegocioException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de inventario regreso una respuesta vacia");
			}
			return articulo;
		} catch (HttpClientErrorException.NotFound e) {
			throw new ErrorNegocioException(HttpStatus.NOT_FOUND, "El articulo " + idArticulo + " no existe");
		} catch (RestClientException e) {
			throw new ErrorNegocioException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de inventario no esta disponible");
		}
	}

	// Le pide al inventario que aparte la cantidad para esta solicitud
	public void reservar(UUID idSolicitud, Long idArticulo, int cantidad) {
		try {
			restClient.post()
				.uri("/api/articulos/{id}/reservar", idArticulo)
				.body(Map.of("idSolicitud", idSolicitud.toString(), "cantidad", cantidad))
				.retrieve()
				.toBodilessEntity();
		} catch (HttpClientErrorException e) {
			// El inventario rechazo la reserva (por ejemplo, ya no hay existencias)
			throw new ErrorNegocioException(HttpStatus.UNPROCESSABLE_ENTITY, "El inventario no pudo reservar el articulo");
		} catch (RestClientException e) {
			throw new ErrorNegocioException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de inventario no esta disponible");
		}
	}

	// Le pide al inventario que libere SOLO lo que se aparto para esta solicitud
	public void liberar(UUID idSolicitud, Long idArticulo, int cantidad) {
		try {
			restClient.post()
				.uri("/api/articulos/{id}/liberar", idArticulo)
				.body(Map.of("idSolicitud", idSolicitud.toString(), "cantidad", cantidad))
				.retrieve()
				.toBodilessEntity();
		} catch (HttpClientErrorException e) {
			throw new ErrorNegocioException(HttpStatus.UNPROCESSABLE_ENTITY, "El inventario no pudo liberar la reserva");
		} catch (RestClientException e) {
			throw new ErrorNegocioException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de inventario no esta disponible");
		}
	}

}
