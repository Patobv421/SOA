package com.proyecto.solicitudes.modelo;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Datos que manda el cliente para crear una solicitud
public class SolicitudPeticion {

	@NotNull(message = "El idOperacion (UUID) es obligatorio")
	private UUID idOperacion;

	@NotNull(message = "El idUsuario es obligatorio")
	@Positive(message = "El idUsuario debe ser mayor a 0")
	private Long idUsuario;

	@NotNull(message = "El idArticulo es obligatorio")
	@Positive(message = "El idArticulo debe ser mayor a 0")
	private Long idArticulo;

	@NotNull(message = "La cantidad es obligatoria")
	@Min(value = 1, message = "La cantidad minima es 1")
	private Integer cantidad;

	@NotNull(message = "La fechaUso es obligatoria")
	@FutureOrPresent(message = "La fechaUso no puede ser una fecha pasada")
	private LocalDate fechaUso;

	public UUID getIdOperacion() {
		return idOperacion;
	}

	public void setIdOperacion(UUID idOperacion) {
		this.idOperacion = idOperacion;
	}

	public Long getIdUsuario() {
		return idUsuario;
	}

	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	public Long getIdArticulo() {
		return idArticulo;
	}

	public void setIdArticulo(Long idArticulo) {
		this.idArticulo = idArticulo;
	}

	public Integer getCantidad() {
		return cantidad;
	}

	public void setCantidad(Integer cantidad) {
		this.cantidad = cantidad;
	}

	public LocalDate getFechaUso() {
		return fechaUso;
	}

	public void setFechaUso(LocalDate fechaUso) {
		this.fechaUso = fechaUso;
	}

}
