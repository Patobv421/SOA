package com.proyecto.usuarios.modelo;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Datos que llegan para registrar o actualizar un usuario
public class UsuarioPeticion {

	@NotBlank(message = "es obligatoria")
	@Pattern(regexp = "^[A-Za-z0-9]{5,20}$", message = "debe tener de 5 a 20 letras o numeros")
	private String matricula;

	@NotBlank(message = "es obligatorio")
	@Size(max = 120, message = "no puede pasar de 120 caracteres")
	private String nombre;

	@NotBlank(message = "es obligatorio")
	@Email(message = "no tiene un formato valido")
	private String correo;

	@NotNull(message = "es obligatorio (ALUMNO o DOCENTE)")
	private TipoUsuario tipo;

	@NotNull(message = "es obligatoria")
	private LocalDate vigenciaCredencial;

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

	public LocalDate getVigenciaCredencial() {
		return vigenciaCredencial;
	}

	public void setVigenciaCredencial(LocalDate vigenciaCredencial) {
		this.vigenciaCredencial = vigenciaCredencial;
	}

}
