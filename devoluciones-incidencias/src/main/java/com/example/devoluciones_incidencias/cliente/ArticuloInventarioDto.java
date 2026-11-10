package com.example.devoluciones_incidencias.cliente;

public record ArticuloInventarioDto(
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
) {
}

