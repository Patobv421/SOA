package com.ejemplo.inventario.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticuloRepository
        extends JpaRepository<ArticuloEntity, Long> {
    
}