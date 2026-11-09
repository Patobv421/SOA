package com.proyecto.usuarios.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proyecto.usuarios.modelo.EstadoUsuario;
import com.proyecto.usuarios.modelo.Usuario;
import com.proyecto.usuarios.modelo.UsuarioPeticion;
import com.proyecto.usuarios.modelo.UsuarioRespuesta;
import com.proyecto.usuarios.repositorio.UsuarioRepositorio;

// Reglas de negocio de los usuarios
@Service
public class UsuarioServicio {

	private final UsuarioRepositorio usuarioRepositorio;
	private final ReglasNegocio reglas;

	public UsuarioServicio(UsuarioRepositorio usuarioRepositorio, ReglasNegocio reglas) {
		this.usuarioRepositorio = usuarioRepositorio;
		this.reglas = reglas;
	}

	// Registra un usuario nuevo
	@Transactional
	public UsuarioRespuesta crear(UsuarioPeticion peticion) {
		String matricula = peticion.getMatricula().trim().toUpperCase();
		String correo = peticion.getCorreo().trim().toLowerCase();

		if (usuarioRepositorio.existsByMatricula(matricula)) {
			throw new ErrorNegocioException(HttpStatus.CONFLICT, "Ya existe un usuario con la matricula " + matricula);
		}
		if (usuarioRepositorio.existsByCorreo(correo)) {
			throw new ErrorNegocioException(HttpStatus.CONFLICT, "Ya existe un usuario con el correo " + correo);
		}
		// Requisito: no se registra a nadie con la credencial vencida
		if (peticion.getVigenciaCredencial().isBefore(LocalDate.now())) {
			throw new ErrorNegocioException(HttpStatus.UNPROCESSABLE_ENTITY, "La credencial ya esta vencida");
		}

		Usuario usuario = new Usuario();
		usuario.setMatricula(matricula);
		usuario.setNombre(peticion.getNombre().trim());
		usuario.setCorreo(correo);
		usuario.setTipo(peticion.getTipo());
		usuario.setVigenciaCredencial(peticion.getVigenciaCredencial());
		usuario.setEstado(EstadoUsuario.ACTIVO);
		usuario.setFechaRegistro(LocalDateTime.now());
		return aRespuesta(usuarioRepositorio.save(usuario));
	}

	// Actualiza los datos (tambien sirve para renovar la credencial)
	@Transactional
	public UsuarioRespuesta actualizar(Long id, UsuarioPeticion peticion) {
		Usuario usuario = buscarEntidad(id);
		String matricula = peticion.getMatricula().trim().toUpperCase();
		String correo = peticion.getCorreo().trim().toLowerCase();

		if (!usuario.getMatricula().equals(matricula) && usuarioRepositorio.existsByMatricula(matricula)) {
			throw new ErrorNegocioException(HttpStatus.CONFLICT, "Ya existe un usuario con la matricula " + matricula);
		}
		if (!usuario.getCorreo().equals(correo) && usuarioRepositorio.existsByCorreo(correo)) {
			throw new ErrorNegocioException(HttpStatus.CONFLICT, "Ya existe un usuario con el correo " + correo);
		}

		usuario.setMatricula(matricula);
		usuario.setNombre(peticion.getNombre().trim());
		usuario.setCorreo(correo);
		usuario.setTipo(peticion.getTipo());
		usuario.setVigenciaCredencial(peticion.getVigenciaCredencial());
		return aRespuesta(usuario);
	}

	// Consulta un usuario. Es la ruta que usa Solicitudes
	@Transactional
	public UsuarioRespuesta buscar(Long id) {
		return aRespuesta(buscarEntidad(id));
	}

	@Transactional
	public UsuarioRespuesta buscarPorMatricula(String matricula) {
		Usuario usuario = usuarioRepositorio.findByMatricula(matricula.trim().toUpperCase())
			.orElseThrow(() -> new ErrorNegocioException(HttpStatus.NOT_FOUND, "No existe el usuario con matricula " + matricula));
		revisarFinSuspension(usuario);
		return aRespuesta(usuario);
	}

	@Transactional
	public List<UsuarioRespuesta> listar() {
		return usuarioRepositorio.findAll().stream()
			.peek(this::revisarFinSuspension)
			.map(this::aRespuesta)
			.toList();
	}

	// Cambio manual de estado: solo ACTIVO o BAJA.
	// La suspension solo la pone el sistema cuando llegan incidencias
	@Transactional
	public UsuarioRespuesta cambiarEstado(Long id, EstadoUsuario nuevoEstado) {
		Usuario usuario = buscarEntidad(id);

		if (nuevoEstado == EstadoUsuario.SUSPENDIDO) {
			throw new ErrorNegocioException(HttpStatus.UNPROCESSABLE_ENTITY,
				"La suspension no se pone a mano, se aplica por incidencias");
		}
		if (usuario.getEstado() == EstadoUsuario.SUSPENDIDO && nuevoEstado == EstadoUsuario.ACTIVO) {
			throw new ErrorNegocioException(HttpStatus.UNPROCESSABLE_ENTITY,
				"El usuario esta suspendido hasta " + usuario.getSuspendidoHasta() + " y no se puede reactivar antes");
		}

		usuario.setEstado(nuevoEstado);
		usuario.setSuspendidoHasta(null);
		return aRespuesta(usuario);
	}

	// Busca el usuario y, si su suspension ya vencio, lo regresa a ACTIVO
	Usuario buscarEntidad(Long id) {
		Usuario usuario = usuarioRepositorio.findById(id)
			.orElseThrow(() -> new ErrorNegocioException(HttpStatus.NOT_FOUND, "El usuario " + id + " no existe"));
		revisarFinSuspension(usuario);
		return usuario;
	}

	void revisarFinSuspension(Usuario usuario) {
		if (usuario.getEstado() == EstadoUsuario.SUSPENDIDO
				&& usuario.getSuspendidoHasta() != null
				&& LocalDate.now().isAfter(usuario.getSuspendidoHasta())) {
			usuario.setEstado(EstadoUsuario.ACTIVO);
			usuario.setSuspendidoHasta(null);
		}
	}

	// Arma la respuesta y decide si el usuario puede pedir material
	UsuarioRespuesta aRespuesta(Usuario usuario) {
		UsuarioRespuesta respuesta = new UsuarioRespuesta();
		respuesta.setId(usuario.getId());
		respuesta.setMatricula(usuario.getMatricula());
		respuesta.setNombre(usuario.getNombre());
		respuesta.setCorreo(usuario.getCorreo());
		respuesta.setTipo(usuario.getTipo());
		respuesta.setEstado(usuario.getEstado());
		respuesta.setVigenciaCredencial(usuario.getVigenciaCredencial());
		respuesta.setSuspendidoHasta(usuario.getSuspendidoHasta());
		respuesta.setLimiteSolicitudes(reglas.limiteSolicitudes(usuario.getTipo()));

		String motivo = motivoNoHabilitado(usuario);
		respuesta.setHabilitado(motivo == null);
		respuesta.setMotivo(motivo == null ? "Puede solicitar material" : motivo);
		return respuesta;
	}

	// Regresa por que no puede pedir material, o null si si puede
	private String motivoNoHabilitado(Usuario usuario) {
		if (usuario.getEstado() == EstadoUsuario.BAJA) {
			return "El usuario esta dado de baja";
		}
		if (usuario.getEstado() == EstadoUsuario.SUSPENDIDO) {
			return "El usuario esta suspendido hasta " + usuario.getSuspendidoHasta();
		}
		if (usuario.getVigenciaCredencial().isBefore(LocalDate.now())) {
			return "La credencial vencio el " + usuario.getVigenciaCredencial();
		}
		return null;
	}

}
