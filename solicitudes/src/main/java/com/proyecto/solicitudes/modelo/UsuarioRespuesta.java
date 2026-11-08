package com.proyecto.solicitudes.modelo;

// Datos que regresa el servicio de Usuarios (Servicio 3) cuando le preguntamos por un usuario
public class UsuarioRespuesta {

	private Long id;
	private String nombre;
	private boolean habilitado;
	private int limiteSolicitudes;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public boolean isHabilitado() {
		return habilitado;
	}

	public void setHabilitado(boolean habilitado) {
		this.habilitado = habilitado;
	}

	public int getLimiteSolicitudes() {
		return limiteSolicitudes;
	}

	public void setLimiteSolicitudes(int limiteSolicitudes) {
		this.limiteSolicitudes = limiteSolicitudes;
	}

}
