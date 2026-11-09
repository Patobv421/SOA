package com.proyecto.usuarios.controlador;

import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.proyecto.usuarios.modelo.ErrorRespuesta;
import com.proyecto.usuarios.servicio.ErrorNegocioException;

// Aqui se convierten los errores en respuestas JSON con el codigo HTTP correcto
@RestControllerAdvice
public class ManejadorErrores {

	// Errores propios del servicio (404, 409, 422, 503)
	@ExceptionHandler(ErrorNegocioException.class)
	public ResponseEntity<ErrorRespuesta> manejarErrorNegocio(ErrorNegocioException e) {
		return responder(e.getEstado(), e.getMessage());
	}

	// Datos de entrada invalidos (campos vacios, correo mal escrito, etc.)
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
		return responder(HttpStatus.BAD_REQUEST, "Un parametro de la ruta no tiene el formato correcto");
	}

	// Pasa si dos peticiones iguales llegan al mismo tiempo (matricula o UUID repetido)
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorRespuesta> manejarDuplicado(DataIntegrityViolationException e) {
		return responder(HttpStatus.CONFLICT, "El dato ya existe o la operacion ya se esta procesando, vuelve a intentar");
	}

	private ResponseEntity<ErrorRespuesta> responder(HttpStatus estado, String mensaje) {
		ErrorRespuesta error = new ErrorRespuesta(estado.value(), estado.getReasonPhrase(), mensaje);
		return ResponseEntity.status(estado).body(error);
	}

}
