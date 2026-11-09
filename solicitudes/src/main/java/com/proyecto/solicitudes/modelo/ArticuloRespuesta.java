package com.proyecto.solicitudes.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Datos que regresa el servicio de Inventario (Servicio 1) cuando le preguntamos por un articulo
// Los campos que no usamos (por ejemplo cantidadTotal) se ignoran
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArticuloRespuesta {

	private Long id;
	private String codigo;
	private String nombre;
	private String deporte;
	private String descripcion;
	private int cantidadBuenEstado;
	private int cantidadMantenimiento;
	private int cantidadDanada;
	private Long version;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getDeporte() {
		return deporte;
	}

	public void setDeporte(String deporte) {
		this.deporte = deporte;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public int getCantidadBuenEstado() {
		return cantidadBuenEstado;
	}

	public void setCantidadBuenEstado(int cantidadBuenEstado) {
		this.cantidadBuenEstado = cantidadBuenEstado;
	}

	public int getCantidadMantenimiento() {
		return cantidadMantenimiento;
	}

	public void setCantidadMantenimiento(int cantidadMantenimiento) {
		this.cantidadMantenimiento = cantidadMantenimiento;
	}

	public int getCantidadDanada() {
		return cantidadDanada;
	}

	public void setCantidadDanada(int cantidadDanada) {
		this.cantidadDanada = cantidadDanada;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

}
