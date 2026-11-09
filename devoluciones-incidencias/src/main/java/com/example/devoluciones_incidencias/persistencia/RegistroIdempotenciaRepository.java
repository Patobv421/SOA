package com.example.devoluciones_incidencias.persistencia;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroIdempotenciaRepository extends JpaRepository<RegistroIdempotenciaEntity, UUID> {
}

