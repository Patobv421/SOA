package com.proyecto.usuarios.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Tabla usuario: alumnos y docentes que pueden pedir material deportivo
@Entity
@Table(name = "usuario")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 20)
	private String matricula;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(nullable = false, unique = true, length = 120)
	private String correo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private TipoUsuario tipo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private EstadoUsuario estado;

	// Fecha hasta la que es valida la credencial escolar. Si ya paso, no puede pedir material
	@Column(nullable = false)
	private LocalDate vigenciaCredencial;

	// Solo tiene valor cuando el usuario esta SUSPENDIDO
	private LocalDate suspendidoHasta;

	@Column(nullable = false)
	private LocalDateTime fechaRegistro;

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

	public LocalDateTime getFechaRegistro() {
		return fechaRegistro;
	}

	public void setFechaRegistro(LocalDateTime fechaRegistro) {
		this.fechaRegistro = fechaRegistro;
	}

}
