package com.example.devoluciones_incidencias.cliente;

public record ArticuloActualizacionRequest(
        String codigo,
        String nombre,
        String deporte,
        String descripcion,
        Integer cantidadBuenEstado,
        Integer cantidadMantenimiento,
        Integer cantidadDanada,
        Long version
) {
}

