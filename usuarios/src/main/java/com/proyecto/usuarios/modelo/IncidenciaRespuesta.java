package com.proyecto.usuarios.modelo;

import java.time.LocalDate;
import java.util.UUID;

// Lo que regresa POST /api/usuarios/{id}/incidencias: la incidencia y como quedo el usuario
public class IncidenciaRespuesta {

	private UUID idOperacion;
	private Long idUsuario;
	private Gravedad gravedad;
	private String descripcion;
	private EstadoUsuario estadoUsuario;
	private LocalDate suspendidoHasta;

	public IncidenciaRespuesta(Incidencia incidencia, Usuario usuario) {
		this.idOperacion = incidencia.getIdOperacion();
		this.idUsuario = incidencia.getIdUsuario();
		this.gravedad = incidencia.getGravedad();
		this.descripcion = incidencia.getDescripcion();
		this.estadoUsuario = usuario.getEstado();
		this.suspendidoHasta = usuario.getSuspendidoHasta();
	}

	public UUID getIdOperacion() {
		return idOperacion;
	}

	public Long getIdUsuario() {
		return idUsuario;
	}

	public Gravedad getGravedad() {
		return gravedad;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public EstadoUsuario getEstadoUsuario() {
		return estadoUsuario;
	}

	public LocalDate getSuspendidoHasta() {
		return suspendidoHasta;
	}

}
