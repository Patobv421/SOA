package com.proyecto.usuarios.modelo;

import java.time.LocalDateTime;

// Formato del JSON que se regresa cuando hay un error
public class ErrorRespuesta {

	private LocalDateTime fecha;
	private int codigo;
	private String error;
	private String mensaje;

	public ErrorRespuesta(int codigo, String error, String mensaje) {
		this.fecha = LocalDateTime.now();
		this.codigo = codigo;
		this.error = error;
		this.mensaje = mensaje;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public int getCodigo() {
		return codigo;
	}

	public String getError() {
		return error;
	}

	public String getMensaje() {
		return mensaje;
	}

}
