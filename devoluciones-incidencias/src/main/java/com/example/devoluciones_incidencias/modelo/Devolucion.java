package com.example.devoluciones_incidencias.modelo;

import java.math.BigDecimal;
import java.time.Instant;

public record Devolucion(
        Long id,
        String folio,
        String idSolicitud,
        String codigoArticulo,
        Long idUsuario,
        Instant fechaDevolucion,
        EstadoMaterial estadoMaterial,
        String observaciones,
        BigDecimal montoSancion,
        Long version
) {
}

