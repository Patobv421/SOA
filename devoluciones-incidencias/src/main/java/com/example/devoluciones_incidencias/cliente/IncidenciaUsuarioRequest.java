package com.example.devoluciones_incidencias.cliente;

import java.util.UUID;

public record IncidenciaUsuarioRequest(
        UUID idOperacion,
        String gravedad,
        String descripcion
) {
}

