package com.proyecto.solicitudes.servicio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.proyecto.solicitudes.modelo.ArticuloRespuesta;
import com.proyecto.solicitudes.modelo.Solicitud;
import com.proyecto.solicitudes.modelo.SolicitudPeticion;
import com.proyecto.solicitudes.modelo.UsuarioRespuesta;
import com.proyecto.solicitudes.repositorio.SolicitudRepositorio;

// Aqui estan las reglas de negocio de las solicitudes
@Service
public class SolicitudServicio {

	private final SolicitudRepositorio solicitudRepositorio;
	private final ClienteUsuarios clienteUsuarios;
	private final ClienteInventario clienteInventario;

	public SolicitudServicio(SolicitudRepositorio solicitudRepositorio, ClienteUsuarios clienteUsuarios,
			ClienteInventario clienteInventario) {
		this.solicitudRepositorio = solicitudRepositorio;
		this.clienteUsuarios = clienteUsuarios;
		this.clienteInventario = clienteInventario;
	}

	// Revisa si ya existe una solicitud con ese UUID
	public boolean existe(UUID idSolicitud) {
		return solicitudRepositorio.existsById(idSolicitud);
	}

	// Crea la solicitud: valida al usuario, revisa existencias y reserva en el inventario
	public Solicitud crearSolicitud(SolicitudPeticion peticion) {

		// Si el UUID ya existe es un reintento: no se crea otra, se regresa la misma
		Optional<Solicitud> existente = solicitudRepositorio.findById(peticion.getIdOperacion());
		if (existente.isPresent()) {
			Solicitud solicitudGuardada = existente.get();
			if (mismosDatos(solicitudGuardada, peticion)) {
				return solicitudGuardada;
			}
			throw new ErrorNegocioException(HttpStatus.CONFLICT, "El UUID ya se uso con datos diferentes");
		}

		// Regla 1: el usuario debe existir y estar habilitado (servicio de Usuarios)
		UsuarioRespuesta usuario = clienteUsuarios.obtenerUsuario(peticion.getIdUsuario());
		if (!usuario.isHabilitado()) {
			throw new ErrorNegocioException(HttpStatus.UNPROCESSABLE_ENTITY, "El usuario no esta habilitado para solicitar material");
		}

		// Regla 2: el usuario no puede pasar su limite de solicitudes activas
		long solicitudesActivas = solicitudRepositorio.countByIdUsuarioAndEstadoIn(
			peticion.getIdUsuario(), List.of(Solicitud.PENDIENTE, Solicitud.CONFIRMADA));
		if (solicitudesActivas >= usuario.getLimiteSolicitudes()) {
			throw new ErrorNegocioException(HttpStatus.UNPROCESSABLE_ENTITY, "El usuario alcanzo su limite de solicitudes");
		}

		// Regla 3: el articulo debe existir y tener existencias suficientes (servicio de Inventario)
		ArticuloRespuesta articulo = clienteInventario.obtenerArticulo(peticion.getIdArticulo());
		if (articulo.getCantidadBuenEstado() < peticion.getCantidad()) {
			throw new ErrorNegocioException(HttpStatus.UNPROCESSABLE_ENTITY, "No hay existencias suficientes del articulo");
		}

		// Se aparta el material en el inventario y despues se guarda la solicitud
		clienteInventario.reservar(peticion.getIdArticulo(), peticion.getCantidad());

		Solicitud solicitud = new Solicitud();
		solicitud.setIdSolicitud(peticion.getIdOperacion());
		solicitud.setIdUsuario(peticion.getIdUsuario());
		solicitud.setIdArticulo(peticion.getIdArticulo());
		solicitud.setCantidad(peticion.getCantidad());
		solicitud.setFechaUso(peticion.getFechaUso());
		solicitud.setEstado(Solicitud.PENDIENTE);
		solicitud.setFechaCreacion(LocalDateTime.now());
		return solicitudRepositorio.save(solicitud);
	}

	// Busca una solicitud por su id
	public Solicitud buscarPorId(UUID idSolicitud) {
		return solicitudRepositorio.findById(idSolicitud)
			.orElseThrow(() -> new ErrorNegocioException(HttpStatus.NOT_FOUND, "La solicitud no existe"));
	}

	// Asignacion: una solicitud pendiente pasa a confirmada
	public Solicitud confirmarSolicitud(UUID idSolicitud) {
		Solicitud solicitud = buscarPorId(idSolicitud);

		if (Solicitud.CONFIRMADA.equals(solicitud.getEstado())) {
			return solicitud;
		}
		if (Solicitud.CANCELADA.equals(solicitud.getEstado())) {
			throw new ErrorNegocioException(HttpStatus.CONFLICT, "La solicitud ya fue cancelada");
		}

		solicitud.setEstado(Solicitud.CONFIRMADA);
		return solicitudRepositorio.save(solicitud);
	}

	// Cancelacion: solo se puede cancelar si sigue pendiente, y se libera su reserva
	public Solicitud cancelarSolicitud(UUID idSolicitud) {
		Solicitud solicitud = buscarPorId(idSolicitud);

		// Si ya estaba cancelada no se vuelve a liberar nada
		if (Solicitud.CANCELADA.equals(solicitud.getEstado())) {
			return solicitud;
		}
		if (Solicitud.CONFIRMADA.equals(solicitud.getEstado())) {
			throw new ErrorNegocioException(HttpStatus.CONFLICT, "La solicitud ya fue confirmada y no se puede cancelar");
		}

		// Primero se libera en el inventario; si falla, la solicitud sigue pendiente
		clienteInventario.liberar(solicitud.getIdArticulo(), solicitud.getCantidad());

		solicitud.setEstado(Solicitud.CANCELADA);
		return solicitudRepositorio.save(solicitud);
	}

	// Compara los datos guardados con los de la nueva peticion
	private boolean mismosDatos(Solicitud solicitud, SolicitudPeticion peticion) {
		return solicitud.getIdUsuario().equals(peticion.getIdUsuario())
			&& solicitud.getIdArticulo().equals(peticion.getIdArticulo())
			&& solicitud.getCantidad().equals(peticion.getCantidad())
			&& solicitud.getFechaUso().equals(peticion.getFechaUso());
	}

}
