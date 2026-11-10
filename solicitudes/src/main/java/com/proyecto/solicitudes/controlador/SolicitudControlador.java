package com.proyecto.solicitudes.controlador;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyecto.solicitudes.modelo.Solicitud;
import com.proyecto.solicitudes.modelo.SolicitudPeticion;
import com.proyecto.solicitudes.servicio.SolicitudServicio;

import jakarta.validation.Valid;

// Rutas REST del Servicio 2 (Solicitudes)
@RestController
@RequestMapping("/api/solicitudes")
@CrossOrigin(origins = {
	"http://localhost:5500",
	"http://127.0.0.1:5500",
	"http://localhost:9010",
	"http://127.0.0.1:9010",
	"*"
})
public class SolicitudControlador {

	private final SolicitudServicio solicitudServicio;

	public SolicitudControlador(SolicitudServicio solicitudServicio) {
		this.solicitudServicio = solicitudServicio;
	}

	// POST /api/solicitudes -> 201 si se creo, 200 si era un reintento con el mismo UUID
	@PostMapping
	public ResponseEntity<Solicitud> crear(@Valid @RequestBody SolicitudPeticion peticion) {
		boolean yaExistia = solicitudServicio.existe(peticion.getIdOperacion());
		Solicitud solicitud = solicitudServicio.crearSolicitud(peticion);

		if (yaExistia) {
			return ResponseEntity.ok(solicitud);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(solicitud);
	}

	// GET /api/solicitudes/{id} -> consulta una solicitud
	@GetMapping("/{id}")
	public Solicitud consultar(@PathVariable UUID id) {
		return solicitudServicio.buscarPorId(id);
	}

	// POST /api/solicitudes/{id}/confirmar -> asigna el material
	@PostMapping("/{id}/confirmar")
	public Solicitud confirmar(@PathVariable UUID id) {
		return solicitudServicio.confirmarSolicitud(id);
	}

	// POST /api/solicitudes/{id}/cancelar -> cancela y libera la reserva
	@PostMapping("/{id}/cancelar")
	public Solicitud cancelar(@PathVariable UUID id) {
		return solicitudServicio.cancelarSolicitud(id);
	}

}
