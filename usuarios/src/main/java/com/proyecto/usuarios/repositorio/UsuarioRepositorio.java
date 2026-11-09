package com.proyecto.usuarios.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.proyecto.usuarios.modelo.Usuario;

import jakarta.persistence.LockModeType;

// Consultas a la tabla usuario
public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

	boolean existsByMatricula(String matricula);

	boolean existsByCorreo(String correo);

	Optional<Usuario> findByMatricula(String matricula);

	// Bloquea la fila del usuario (SELECT ... FOR UPDATE) mientras se registra una incidencia,
	// para que dos incidencias al mismo tiempo no se pisen al calcular la suspension
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from Usuario u where u.id = :id")
	Optional<Usuario> buscarYBloquear(@Param("id") Long id);

}
