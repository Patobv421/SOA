package com.proyecto.usuarios.modelo;

import jakarta.validation.constraints.NotNull;

// Cuerpo de PATCH /api/usuarios/{id}/estado
public class EstadoPeticion {

	@NotNull(message = "es obligatorio (ACTIVO o BAJA)")
	private EstadoUsuario estado;

	public EstadoUsuario getEstado() {
		return estado;
	}

	public void setEstado(EstadoUsuario estado) {
		this.estado = estado;
	}

}
