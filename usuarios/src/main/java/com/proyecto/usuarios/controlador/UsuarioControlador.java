package com.proyecto.usuarios.controlador;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.proyecto.usuarios.modelo.EstadoPeticion;
import com.proyecto.usuarios.modelo.UsuarioPeticion;
import com.proyecto.usuarios.modelo.UsuarioRespuesta;
import com.proyecto.usuarios.servicio.UsuarioServicio;

import jakarta.validation.Valid;

// Rutas REST del Servicio 3 (Usuarios)
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = {
	"http://localhost:5500",
	"http://127.0.0.1:5500",
	"http://localhost:9010",
	"http://127.0.0.1:9010",
	"*"
})
public class UsuarioControlador {

	private final UsuarioServicio usuarioServicio;

	public UsuarioControlador(UsuarioServicio usuarioServicio) {
		this.usuarioServicio = usuarioServicio;
	}

	// POST /api/usuarios -> registra un usuario
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UsuarioRespuesta crear(@Valid @RequestBody UsuarioPeticion peticion) {
		return usuarioServicio.crear(peticion);
	}

	// GET /api/usuarios -> lista todos
	@GetMapping
	public List<UsuarioRespuesta> listar() {
		return usuarioServicio.listar();
	}

	// GET /api/usuarios/{id} -> la usa Solicitudes para saber si esta habilitado y su limite
	@GetMapping("/{id}")
	public UsuarioRespuesta consultar(@PathVariable Long id) {
		return usuarioServicio.buscar(id);
	}

	// GET /api/usuarios/matricula/{matricula} -> para que la pagina busque por matricula
	@GetMapping("/matricula/{matricula}")
	public UsuarioRespuesta consultarPorMatricula(@PathVariable String matricula) {
		return usuarioServicio.buscarPorMatricula(matricula);
	}

	// PUT /api/usuarios/{id} -> actualiza datos o renueva la credencial
	@PutMapping("/{id}")
	public UsuarioRespuesta actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioPeticion peticion) {
		return usuarioServicio.actualizar(id, peticion);
	}

	// PATCH /api/usuarios/{id}/estado -> dar de baja o reactivar
	@PatchMapping("/{id}/estado")
	public UsuarioRespuesta cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoPeticion peticion) {
		return usuarioServicio.cambiarEstado(id, peticion.getEstado());
	}

}
