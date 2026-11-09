package com.proyecto.usuarios.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.proyecto.usuarios.modelo.TipoUsuario;

// Valores de las reglas. Se leen de application.properties (reglas.*)
@Component
public class ReglasNegocio {

	@Value("${reglas.limite-alumno}")
	private int limiteAlumno;

	@Value("${reglas.limite-docente}")
	private int limiteDocente;

	@Value("${reglas.dias-suspension-grave}")
	private int diasSuspensionGrave;

	@Value("${reglas.leves-para-suspender}")
	private int levesParaSuspender;

	@Value("${reglas.dias-suspension-leves}")
	private int diasSuspensionLeves;

	// Solicitudes activas permitidas segun el tipo de usuario
	public int limiteSolicitudes(TipoUsuario tipo) {
		if (tipo == TipoUsuario.DOCENTE) {
			return limiteDocente;
		}
		return limiteAlumno;
	}

	public int getDiasSuspensionGrave() {
		return diasSuspensionGrave;
	}

	public int getLevesParaSuspender() {
		return levesParaSuspender;
	}

	public int getDiasSuspensionLeves() {
		return diasSuspensionLeves;
	}

}
