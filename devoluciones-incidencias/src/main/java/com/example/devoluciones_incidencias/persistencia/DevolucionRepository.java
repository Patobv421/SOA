package com.example.devoluciones_incidencias.persistencia;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DevolucionRepository extends JpaRepository<DevolucionEntity, Long> {
    Optional<DevolucionEntity> findByFolio(String folio);
    boolean existsByFolio(String folio);
    boolean existsByIdSolicitud(String idSolicitud);
}

