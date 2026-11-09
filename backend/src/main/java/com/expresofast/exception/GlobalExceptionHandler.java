package com.expresofast.exception;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Manejador global de errores. Todas las respuestas de error siguen el formato
 * RFC 7807 (Problem Details for HTTP APIs) mediante {@link ProblemDetail}.
 * Al extender {@link ResponseEntityExceptionHandler}, las excepciones estandar
 * de Spring MVC (JSON mal formado, parametros faltantes, etc.) tambien se
 * responden como application/problem+json.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<Map<String, String>> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(this::mapFieldError)
                .toList();

        ProblemDetail problema = ProblemDetailFactory.crear(
                HttpStatus.BAD_REQUEST,
                "validacion",
                "Error de validacion",
                "Uno o mas campos no cumplen las reglas de validacion.",
                obtenerRuta(request));
        problema.setProperty("errores", errores);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problema);
    }

    private Map<String, String> mapFieldError(FieldError fieldError) {
        Map<String, String> detalle = new LinkedHashMap<>();
        detalle.put("campo", fieldError.getField());
        detalle.put("mensaje", fieldError.getDefaultMessage());
        return detalle;
    }

    private String obtenerRuta(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return null;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, "recurso-no-encontrado", "Recurso no encontrado",
                ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ProblemDetail> handleInvalidStateTransition(
            InvalidStateTransitionException ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "transicion-invalida", "Transicion de estado invalida",
                ex.getMessage(), request);
    }

    @ExceptionHandler(CapacidadExcedidaException.class)
    public ResponseEntity<ProblemDetail> handleCapacidadExcedida(
            CapacidadExcedidaException ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "capacidad-excedida", "Capacidad del vehiculo excedida",
                ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "parametro-invalido", "Parametro invalido",
                ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, "recurso-duplicado", "Recurso duplicado",
                ex.getMessage(), request);
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<ProblemDetail> handleOperacionNoPermitida(
            OperacionNoPermitidaException ex, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, "operacion-no-permitida", "Operacion no permitida",
                ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return responder(HttpStatus.UNAUTHORIZED, "credenciales-invalidas", "No autorizado",
                "Usuario o contrasena incorrectos.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return responder(HttpStatus.FORBIDDEN, "acceso-denegado", "Acceso denegado",
                "No cuenta con los permisos necesarios para realizar esta accion.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Error inesperado procesando {} {}", request.getMethod(), request.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "error-interno", "Error interno del servidor",
                "Ocurrio un error inesperado en el servidor. Contacte al administrador.", request);
    }

    private ResponseEntity<ProblemDetail> responder(HttpStatus status, String tipo, String titulo,
            String detalle, HttpServletRequest request) {
        ProblemDetail problema = ProblemDetailFactory.crear(status, tipo, titulo, detalle,
                request.getRequestURI());
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problema);
    }
}
