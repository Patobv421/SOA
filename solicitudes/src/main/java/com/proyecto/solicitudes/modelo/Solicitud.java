package com.proyecto.solicitudes.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Esta clase representa la tabla "solicitudes" de la base de datos
@Entity
@Table(name = "solicitudes")
public class Solicitud {

	// Estados que puede tener una solicitud
	public static final String PENDIENTE = "PENDIENTE";
	public static final String CONFIRMADA = "CONFIRMADA";
	public static final String CANCELADA = "CANCELADA";

	// El id es el UUID de la operacion, lo manda el cliente para poder reintentar
	@Id
	@Column(name = "id_solicitud")
	private UUID idSolicitud;

	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@Column(name = "id_articulo", nullable = false)
	private Long idArticulo;

	private Integer cantidad;

	@Column(name = "fecha_uso", nullable = false)
	private LocalDate fechaUso;

	private String estado;

	@Column(name = "fecha_creacion")
	private LocalDateTime fechaCreacion;

	public UUID getIdSolicitud() {
		return idSolicitud;
	}

	public void setIdSolicitud(UUID idSolicitud) {
		this.idSolicitud = idSolicitud;
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

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

}
