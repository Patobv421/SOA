package com.proyecto.solicitudes.controlador;

import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.proyecto.solicitudes.modelo.ErrorRespuesta;
import com.proyecto.solicitudes.servicio.ErrorNegocioException;

// Aqui se convierten los errores en respuestas JSON con el codigo HTTP correcto
@RestControllerAdvice
public class ManejadorErrores {

	// Errores propios del servicio (404, 409, 422, 503)
	@ExceptionHandler(ErrorNegocioException.class)
	public ResponseEntity<ErrorRespuesta> manejarErrorNegocio(ErrorNegocioException e) {
		return responder(e.getEstado(), e.getMessage());
	}

	// Datos de entrada invalidos (cantidad 0, campos vacios, etc.)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorRespuesta> manejarValidacion(MethodArgumentNotValidException e) {
		String mensaje = e.getBindingResult().getFieldErrors().stream()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.collect(Collectors.joining("; "));
		return responder(HttpStatus.BAD_REQUEST, mensaje);
	}

	// JSON mal escrito o UUID con formato incorrecto en el cuerpo
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorRespuesta> manejarJsonInvalido(HttpMessageNotReadableException e) {
		return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es un JSON valido");
	}

	// UUID con formato incorrecto en la ruta
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorRespuesta> manejarIdInvalido(MethodArgumentTypeMismatchException e) {
		return responder(HttpStatus.BAD_REQUEST, "El id de la solicitud no tiene formato UUID");
	}

	// Pasa si dos peticiones con el mismo UUID llegan al mismo tiempo
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorRespuesta> manejarDuplicado(DataIntegrityViolationException e) {
		return responder(HttpStatus.CONFLICT, "La operacion ya se esta procesando, vuelve a intentar");
	}

	private ResponseEntity<ErrorRespuesta> responder(HttpStatus estado, String mensaje) {
		ErrorRespuesta error = new ErrorRespuesta(estado.value(), estado.getReasonPhrase(), mensaje);
		return ResponseEntity.status(estado).body(error);
	}

}
