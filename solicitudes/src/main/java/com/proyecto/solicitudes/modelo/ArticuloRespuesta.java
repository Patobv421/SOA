package com.proyecto.solicitudes.modelo;

// Datos que regresa el servicio de Inventario (Servicio 1) cuando le preguntamos por un articulo
public class ArticuloRespuesta {

	private Long id;
	private String nombre;
	private int cantidadDisponible;

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

	public int getCantidadDisponible() {
		return cantidadDisponible;
	}

	public void setCantidadDisponible(int cantidadDisponible) {
		this.cantidadDisponible = cantidadDisponible;
	}

}
