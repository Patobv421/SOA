package com.proyecto.usuarios.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proyecto.usuarios.modelo.EstadoUsuario;
import com.proyecto.usuarios.modelo.Gravedad;
import com.proyecto.usuarios.modelo.Incidencia;
import com.proyecto.usuarios.modelo.IncidenciaPeticion;
import com.proyecto.usuarios.modelo.IncidenciaRespuesta;
import com.proyecto.usuarios.modelo.Usuario;
import com.proyecto.usuarios.repositorio.IncidenciaRepositorio;
import com.proyecto.usuarios.repositorio.UsuarioRepositorio;

// Recibe las incidencias de Devoluciones y decide si el usuario se suspende:
// - Una incidencia GRAVE lo suspende de inmediato
// - Juntar varias LEVES en 30 dias tambien lo suspende
@Service
public class IncidenciaServicio {

	private final IncidenciaRepositorio incidenciaRepositorio;
	private final UsuarioRepositorio usuarioRepositorio;
	private final UsuarioServicio usuarioServicio;
	private final ReglasNegocio reglas;

	public IncidenciaServicio(IncidenciaRepositorio incidenciaRepositorio, UsuarioRepositorio usuarioRepositorio,
			UsuarioServicio usuarioServicio, ReglasNegocio reglas) {
		this.incidenciaRepositorio = incidenciaRepositorio;
		this.usuarioRepositorio = usuarioRepositorio;
		this.usuarioServicio = usuarioServicio;
		this.reglas = reglas;
	}

	public boolean existe(UUID idOperacion) {
		return incidenciaRepositorio.existsById(idOperacion);
	}

	@Transactional
	public IncidenciaRespuesta registrar(Long idUsuario, IncidenciaPeticion peticion) {
		Usuario usuario = usuarioRepositorio.buscarYBloquear(idUsuario)
			.orElseThrow(() -> new ErrorNegocioException(HttpStatus.NOT_FOUND, "El usuario " + idUsuario + " no existe"));

		// Si el UUID ya existe es un reintento: se regresa lo mismo sin volver a sancionar
		Optional<Incidencia> existente = incidenciaRepositorio.findById(peticion.getIdOperacion());
		if (existente.isPresent()) {
			Incidencia guardada = existente.get();
			if (!guardada.getIdUsuario().equals(idUsuario) || guardada.getGravedad() != peticion.getGravedad()) {
				throw new ErrorNegocioException(HttpStatus.CONFLICT, "El UUID ya se uso con datos diferentes");
			}
			return new IncidenciaRespuesta(guardada, usuario);
		}

		usuarioServicio.revisarFinSuspension(usuario);

		Incidencia incidencia = new Incidencia();
		incidencia.setIdOperacion(peticion.getIdOperacion());
		incidencia.setIdUsuario(idUsuario);
		incidencia.setGravedad(peticion.getGravedad());
		incidencia.setDescripcion(peticion.getDescripcion().trim());
		incidencia.setFecha(LocalDateTime.now());
		incidenciaRepositorio.save(incidencia);

		aplicarSancion(usuario, peticion.getGravedad());
		return new IncidenciaRespuesta(incidencia, usuario);
	}

	@Transactional(readOnly = true)
	public List<Incidencia> listar(Long idUsuario) {
		if (!usuarioRepositorio.existsById(idUsuario)) {
			throw new ErrorNegocioException(HttpStatus.NOT_FOUND, "El usuario " + idUsuario + " no existe");
		}
		return incidenciaRepositorio.findByIdUsuarioOrderByFechaDesc(idUsuario);
	}

	private void aplicarSancion(Usuario usuario, Gravedad gravedad) {
		// A un usuario dado de baja se le guarda la incidencia pero no cambia su estado
		if (usuario.getEstado() == EstadoUsuario.BAJA) {
			return;
		}

		int dias = 0;
		if (gravedad == Gravedad.GRAVE) {
			dias = reglas.getDiasSuspensionGrave();
		} else {
			long leves = incidenciaRepositorio.countByIdUsuarioAndGravedadAndFechaAfter(
				usuario.getId(), Gravedad.LEVE, LocalDateTime.now().minusDays(30));
			if (leves >= reglas.getLevesParaSuspender()) {
				dias = reglas.getDiasSuspensionLeves();
			}
		}
		if (dias == 0) {
			return;
		}

		LocalDate hasta = LocalDate.now().plusDays(dias);
		// Si ya tenia una suspension mas larga, se respeta la mas larga
		if (usuario.getSuspendidoHasta() != null && usuario.getSuspendidoHasta().isAfter(hasta)) {
			hasta = usuario.getSuspendidoHasta();
		}
		usuario.setEstado(EstadoUsuario.SUSPENDIDO);
		usuario.setSuspendidoHasta(hasta);
	}

}
