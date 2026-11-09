package com.proyecto.solicitudes.servicio;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.proyecto.solicitudes.modelo.ArticuloRespuesta;

// Se comunica con el servicio de Inventario deportivo por REST
// Usa sus rutas: GET /api/articulos/{id} y PUT /api/articulos/{id}
@Component
public class ClienteInventario {

	// Veces que se intenta de nuevo si otro proceso modifico el articulo al mismo tiempo
	private static final int INTENTOS = 3;

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

	// Reservar es restar la cantidad de las piezas en buen estado
	public void reservar(Long idArticulo, int cantidad) {
		cambiarCantidad(idArticulo, -cantidad);
	}

	// Liberar es devolver la cantidad a las piezas en buen estado
	public void liberar(Long idArticulo, int cantidad) {
		cambiarCantidad(idArticulo, cantidad);
	}

	// El inventario no tiene rutas de reservar ni liberar, asi que se lee el articulo
	// y se manda con el PUT la nueva cantidad en buen estado junto con su version
	private void cambiarCantidad(Long idArticulo, int diferencia) {
		for (int intento = 1; intento <= INTENTOS; intento++) {

			ArticuloRespuesta articulo = obtenerArticulo(idArticulo);

			int nuevaCantidad = articulo.getCantidadBuenEstado() + diferencia;
			if (nuevaCantidad < 0) {
				throw new ErrorNegocioException(HttpStatus.UNPROCESSABLE_ENTITY, "No hay existencias suficientes del articulo");
			}

			// El inventario pide todos estos campos y no acepta campos de mas
			Map<String, Object> datos = new LinkedHashMap<>();
			datos.put("codigo", articulo.getCodigo());
			datos.put("nombre", articulo.getNombre());
			datos.put("deporte", articulo.getDeporte());
			datos.put("descripcion", articulo.getDescripcion());
			datos.put("cantidadBuenEstado", nuevaCantidad);
			datos.put("cantidadMantenimiento", articulo.getCantidadMantenimiento());
			datos.put("cantidadDanada", articulo.getCantidadDanada());
			datos.put("version", articulo.getVersion());

			try {
				restClient.put()
					.uri("/api/articulos/{id}", idArticulo)
					.body(datos)
					.retrieve()
					.toBodilessEntity();
				return;
			} catch (HttpClientErrorException.Conflict e) {
				// Otro proceso cambio el articulo: se vuelve a leer y se intenta de nuevo
			} catch (HttpClientErrorException e) {
				throw new ErrorNegocioException(HttpStatus.UNPROCESSABLE_ENTITY, "El inventario rechazo el cambio de existencias");
			} catch (RestClientException e) {
				throw new ErrorNegocioException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de inventario no esta disponible");
			}
		}

		throw new ErrorNegocioException(HttpStatus.CONFLICT, "El articulo se esta modificando, vuelve a intentar");
	}

}
