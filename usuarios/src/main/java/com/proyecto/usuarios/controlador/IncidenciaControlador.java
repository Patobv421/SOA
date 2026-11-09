package com.proyecto.usuarios.controlador;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyecto.usuarios.modelo.Incidencia;
import com.proyecto.usuarios.modelo.IncidenciaPeticion;
import com.proyecto.usuarios.modelo.IncidenciaRespuesta;
import com.proyecto.usuarios.servicio.IncidenciaServicio;

import jakarta.validation.Valid;

// Rutas para las incidencias que manda Devoluciones (Servicio 4)
@RestController
@RequestMapping("/api/usuarios/{id}/incidencias")
public class IncidenciaControlador {

	private final IncidenciaServicio incidenciaServicio;

	public IncidenciaControlador(IncidenciaServicio incidenciaServicio) {
		this.incidenciaServicio = incidenciaServicio;
	}

	// POST -> 201 si es nueva, 200 si es un reintento con el mismo UUID
	@PostMapping
	public ResponseEntity<IncidenciaRespuesta> registrar(@PathVariable Long id,
			@Valid @RequestBody IncidenciaPeticion peticion) {
		boolean yaExistia = incidenciaServicio.existe(peticion.getIdOperacion());
		IncidenciaRespuesta respuesta = incidenciaServicio.registrar(id, peticion);

		if (yaExistia) {
			return ResponseEntity.ok(respuesta);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
	}

	@GetMapping
	public List<Incidencia> listar(@PathVariable Long id) {
		return incidenciaServicio.listar(id);
	}

}
