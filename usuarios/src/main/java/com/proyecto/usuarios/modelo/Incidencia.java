package com.proyecto.usuarios.modelo;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Tabla incidencia: danos o retrasos que reporta el servicio de Devoluciones.
// La llave es el UUID de la operacion, asi un reintento no registra dos veces la misma incidencia
@Entity
@Table(name = "incidencia")
public class Incidencia {

	@Id
	private UUID idOperacion;

	@Column(nullable = false)
	private Long idUsuario;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Gravedad gravedad;

	@Column(nullable = false, length = 255)
	private String descripcion;

	@Column(nullable = false)
	private LocalDateTime fecha;

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

	public LocalDateTime getFecha() {
		return fecha;
	}

	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

}
