package com.proyecto.solicitudes.repositorio;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyecto.solicitudes.modelo.Solicitud;

public interface SolicitudRepositorio extends JpaRepository<Solicitud, UUID> {

	// Cuenta cuantas solicitudes tiene un usuario en los estados indicados
	long countByIdUsuarioAndEstadoIn(Long idUsuario, List<String> estados);

}
