package com.ejemplo.inventario.modelo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ArticuloRequest(
        @NotBlank(message = "El codigo es obligatorio")
        @Size(max = 30, message = "El codigo no debe superar 30 caracteres")
        @Pattern(
                regexp = "^[a-zA-Z0-9-]+$",
                message = "El codigo solo puede contener letras, numeros y guiones"
        )
        String codigo,
        
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
        String nombre, 
        
        @NotBlank(message = "El deporte es obligatorio")
        @Size(max = 60, message = "El deporte no debe superar 60 caracteres")
        String deporte,
        
        @NotNull(message = "La descripcion debe estar presente")
        @Size(max = 500, message = "La descripcion no debe superar 500 caracteres")
        String descripcion,
                
        @NotNull(message = "La cantidad en un buen estado es obligatoria")                
        @Min(value = 0, message = "La cantidad en buen estado no puede ser negativa")
        @Max(value = 1000000, message = "La cantidad en buen estado no puede superar 1000000")
        Integer cantidadBuenEstado,
        
        @NotNull(message = "La cantidad en mantenimiento es obligatoria")
        @Min(value = 0, message = "La cantidad en mantenimiento no peude ser negativa")
        @Max(value = 1000000, message = "La cantidad en mantenimiento no puede superar 1000000")
        Integer cantidadMantenimiento,
                
        @NotNull(message = "La cantidad danada es obligatoria")
        @Min(value = 0, message = "La cantidad en danada no puede ser negativa")
        @Max(value = 1000000, message = "La cantidad danada no puede superar 1000000")
        Integer cantidadDanada,
        
        @NotNull(message = "La version es obligatoria")
        @Min(value = 0, message = "La version no puede ser negativa")
        Long version
        
        ){
    
}