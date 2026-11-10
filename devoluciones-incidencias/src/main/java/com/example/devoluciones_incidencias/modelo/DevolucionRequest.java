package com.example.devoluciones_incidencias.modelo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record DevolucionRequest(
        @NotBlank(message = "La clave de idempotencia (UUID) es obligatoria")
        @Pattern(
                regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "La clave de idempotencia debe ser un UUID valido (ejemplo: 123e4567-e89b-12d3-a456-426614174000)"
        )
        String claveIdempotencia,

        @NotBlank(message = "El identificador de la solicitud es obligatorio")
        @Size(max = 64, message = "El identificador de la solicitud no debe superar 64 caracteres")
        String idSolicitud,

        @NotBlank(message = "El codigo del articulo es obligatorio")
        @Size(max = 30, message = "El codigo del articulo no debe superar 30 caracteres")
        String codigoArticulo,

        @NotNull(message = "El identificador del usuario es obligatorio")
        @Min(value = 1, message = "El identificador del usuario debe ser mayor que 0")
        Long idUsuario,

        @NotNull(message = "El estado del material es obligatorio")
        EstadoMaterial estadoMaterial,

        @NotNull(message = "Las observaciones deben estar presentes")
        @Size(max = 500, message = "Las observaciones no deben superar 500 caracteres")
        String observaciones,

        @NotNull(message = "El monto de sancion es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El monto de sancion no puede ser negativo")
        BigDecimal montoSancion,

        @NotNull(message = "La version es obligatoria")
        @Min(value = 0, message = "La version no puede ser negativa")
        Long version
) {
}

