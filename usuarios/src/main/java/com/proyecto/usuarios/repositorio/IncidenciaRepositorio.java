package com.proyecto.usuarios.repositorio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyecto.usuarios.modelo.Gravedad;
import com.proyecto.usuarios.modelo.Incidencia;

// Consultas a la tabla incidencia
public interface IncidenciaRepositorio extends JpaRepository<Incidencia, UUID> {

	// Cuantas incidencias de cierta gravedad tiene el usuario desde una fecha
	long countByIdUsuarioAndGravedadAndFechaAfter(Long idUsuario, Gravedad gravedad, LocalDateTime desde);

	List<Incidencia> findByIdUsuarioOrderByFechaDesc(Long idUsuario);

}
