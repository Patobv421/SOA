package com.proyecto.solicitudes.servicio;

import org.springframework.http.HttpStatus;

// Error que usamos cuando algo falla (regla de negocio, no existe, servicio apagado)
// Guarda el codigo HTTP que se va a responder
public class ErrorNegocioException extends RuntimeException {

	private final HttpStatus estado;

	public ErrorNegocioException(HttpStatus estado, String mensaje) {
		super(mensaje);
		this.estado = estado;
	}

	public HttpStatus getEstado() {
		return estado;
	}

}
