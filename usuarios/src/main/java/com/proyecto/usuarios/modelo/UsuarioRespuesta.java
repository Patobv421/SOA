package com.proyecto.usuarios.modelo;

import java.time.LocalDate;

// Lo que regresa GET /api/usuarios/{id}.
// Solicitudes lee "habilitado" y "limiteSolicitudes"; los demas campos son informativos
public class UsuarioRespuesta {

	private Long id;
	private String matricula;
	private String nombre;
	private String correo;
	private TipoUsuario tipo;
	private EstadoUsuario estado;
	private LocalDate vigenciaCredencial;
	private LocalDate suspendidoHasta;
	private boolean habilitado;
	private String motivo;
	private int limiteSolicitudes;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getMatricula() {
		return matricula;
	}

	public void setMatricula(String matricula) {
		this.matricula = matricula;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getCorreo() {
		return correo;
	}

	public void setCorreo(String correo) {
		this.correo = correo;
	}

	public TipoUsuario getTipo() {
		return tipo;
	}

	public void setTipo(TipoUsuario tipo) {
		this.tipo = tipo;
	}

	public EstadoUsuario getEstado() {
		return estado;
	}

	public void setEstado(EstadoUsuario estado) {
		this.estado = estado;
	}

	public LocalDate getVigenciaCredencial() {
		return vigenciaCredencial;
	}

	public void setVigenciaCredencial(LocalDate vigenciaCredencial) {
		this.vigenciaCredencial = vigenciaCredencial;
	}

	public LocalDate getSuspendidoHasta() {
		return suspendidoHasta;
	}

	public void setSuspendidoHasta(LocalDate suspendidoHasta) {
		this.suspendidoHasta = suspendidoHasta;
	}

	public boolean isHabilitado() {
		return habilitado;
	}

	public void setHabilitado(boolean habilitado) {
		this.habilitado = habilitado;
	}

	public String getMotivo() {
		return motivo;
	}

	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}

	public int getLimiteSolicitudes() {
		return limiteSolicitudes;
	}

	public void setLimiteSolicitudes(int limiteSolicitudes) {
		this.limiteSolicitudes = limiteSolicitudes;
	}

}
