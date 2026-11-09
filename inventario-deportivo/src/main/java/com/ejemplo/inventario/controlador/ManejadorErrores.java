package com.ejemplo.inventario.controlador;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class ManejadorErrores {
    
    private static final Logger log =
            LoggerFactory.getLogger(ManejadorErrores.class);
    
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> manejarEstado(
            ResponseStatusException error
    ) {
        String mensaje = error.getReason() !=null
                ? error.getReason()
                : "No se pudo completar la solicitud";
        
        return respuesta(error.getStatusCode().value(), mensaje);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(
            MethodArgumentNotValidException error
    ) {
        Map<String, String> campos = new LinkedHashMap<>();
        
        error.getBindingResult().getFieldErrors().forEach(
                campo -> campos.putIfAbsent(
                        campo.getField(),
                        campo.getDefaultMessage()
                )
        );
        
        Map<String, Object> cuerpo = crearCuerpo(
                HttpStatus.BAD_REQUEST.value(),
                "Revisa los datos enviados"
        );
        
        cuerpo.put("campos", campos);
        
        return ResponseEntity.badRequest().body(cuerpo);
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> manejarJson(
            HttpMessageNotReadableException error
    ) {
        return respuesta(
                HttpStatus.BAD_REQUEST.value(),
                "El JSON es invalido, contiene campos desconocidos "
                    + "o valores con un tipo incorrecto"
        );
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> manejarTipo(
            MethodArgumentTypeMismatchException error
    ) {
        return respuesta(
                HttpStatus.BAD_REQUEST.value(),
                "El paramentro " + error.getName()
                    + " tiene un formato incorrecto"
        );
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> manejarIntegridad(
            DataIntegrityViolationException error
    ) {
        log.warn("Se rechazo una operacion por una restriccion de inetgridad");
        
        return respuesta(
                HttpStatus.CONFLICT.value(),
                "Los datos incumplen una restriccion de la abse de datos. "
                    + "Comprueba que el codigo no este repetido "
                    +"y que los valores sean validos"
        );
    }
    
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> manejarConcurrencia(
            OptimisticLockingFailureException error
    ) {
        return respuesta(
                HttpStatus.CONFLICT.value(),
                "Otro proceso modifico el articulo. "
                    + "Consulta su version actual e intenta nuevamente"
        );
    }
    
    @ExceptionHandler({
        DataAccessResourceFailureException.class,
        CannotCreateTransactionException.class
    })
    
    public ResponseEntity<Map<String, Object>> manejarConexion(
            Exception error
    ) {
        log.error(
                "No se pudo acceder a la base de datos. Tipo de error: {}",
                error.getClass().getSimpleName()
        );
        
        return respuesta (
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "No se pudo acceder a la base de datos. Intenta mas tarde"
        );
    }
    
    private ResponseEntity<Map<String, Object>> respuesta(
            int estado,
            String mensaje
    ) {
        return ResponseEntity.status(estado)
                .body(crearCuerpo(estado, mensaje));
    }
    
    private Map<String, Object> crearCuerpo(
            int estado,
            String mensaje
    ) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        
        cuerpo.put("fecha", Instant.now().toString());
        cuerpo.put("estado", estado);
        cuerpo.put("mensaje", mensaje);
        
        return cuerpo;
    }
}