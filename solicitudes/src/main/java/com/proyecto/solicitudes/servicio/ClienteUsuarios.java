package com.proyecto.solicitudes.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.proyecto.solicitudes.modelo.UsuarioRespuesta;

// Se comunica con el servicio de Usuarios por REST
@Component
public class ClienteUsuarios {

	private final RestClient restClient;

	public ClienteUsuarios(@Value("${servicios.usuarios.url}") String url,
			@Value("${servicios.tiempo-espera-segundos}") int segundosEspera) {
		this.restClient = ConfiguracionRestClient.crear(url, segundosEspera);
	}

	// Pregunta al servicio de Usuarios por los datos de un usuario
	public UsuarioRespuesta obtenerUsuario(Long idUsuario) {
		try {
			UsuarioRespuesta usuario = restClient.get()
				.uri("/api/usuarios/{id}", idUsuario)
				.retrieve()
				.body(UsuarioRespuesta.class);
			if (usuario == null) {
				throw new ErrorNegocioException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de usuarios regreso una respuesta vacia");
			}
			return usuario;
		} catch (HttpClientErrorException.NotFound e) {
			throw new ErrorNegocioException(HttpStatus.NOT_FOUND, "El usuario " + idUsuario + " no existe");
		} catch (RestClientException e) {
			// Servicio apagado, tiempo de espera agotado o error del otro servicio
			throw new ErrorNegocioException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de usuarios no esta disponible");
		}
	}

}
