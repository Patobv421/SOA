package com.ejemplo.inventario.modelo;

public record Articulo(
        Long id,
        String codigo,
        String nombre,
        String deporte,
        String descripcion,
        Integer cantidadBuenEstado,
        Integer cantidadMantenimiento,
        Integer cantidadDanada,
        Long cantidadTotal,
        Long version
        ){
    
}