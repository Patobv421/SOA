package com.proyecto.usuarios.modelo;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Datos que manda Devoluciones para reportar una incidencia
public class IncidenciaPeticion {

	@NotNull(message = "es obligatorio")
	private UUID idOperacion;

	@NotNull(message = "es obligatoria (LEVE o GRAVE)")
	private Gravedad gravedad;

	@NotBlank(message = "es obligatoria")
	@Size(max = 255, message = "no puede pasar de 255 caracteres")
	private String descripcion;

	public UUID getIdOperacion() {
		return idOperacion;
	}

	public void setIdOperacion(UUID idOperacion) {
		this.idOperacion = idOperacion;
	}

	public Gravedad getGravedad() {
		return gravedad;
	}

	public void setGravedad(Gravedad gravedad) {
		this.gravedad = gravedad;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

}
