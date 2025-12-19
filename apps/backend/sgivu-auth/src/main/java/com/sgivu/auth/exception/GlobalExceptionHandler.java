package com.sgivu.auth.exception;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.ResourceAccessException;

/**
 * Manejo centralizado de excepciones expuestas por el Authorization Server.
 *
 * <p>Devuelve respuestas JSON consistentes hacia los clientes front (Angular) y otros
 * microservicios, evitando páginas HTML y explicitando si el problema es de validación de datos o
 * de disponibilidad del servicio de usuarios.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final String TIMESTAMP_KEY = "timestamp";
  private static final String STATUS_KEY = "status";
  private static final String ERROR_KEY = "error";
  private static final String MESSAGE_KEY = "message";
  private static final String PATH_KEY = "path";
  private static final String AUTH_PATH = "/auth";

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Captura violaciones de constraints (ej. claves únicas de clientes OIDC) y responde de forma
   * determinística para el portal de ventas.
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> handleConstraintViolationException(
      ConstraintViolationException ex) {
    logger.error("Se ha producido una violación de restricción: {}", ex.getMessage(), ex);

    Map<String, Object> response =
        Map.of(
            TIMESTAMP_KEY, LocalDateTime.now(),
            STATUS_KEY, HttpStatus.CONFLICT.value(),
            ERROR_KEY, "Se ha producido una violación de restricción. Verificar campos únicos.",
            MESSAGE_KEY, ex.getMessage(),
            PATH_KEY, AUTH_PATH);

    return new ResponseEntity<>(response, HttpStatus.CONFLICT);
  }

  /**
   * Responde cuando el Authorization Server no puede comunicarse con servicios satélite (ej.
   * sgivu-user). Se mantiene el mismo cuerpo para que Angular pueda mostrar mensajes claros.
   */
  @ExceptionHandler(ServiceUnavailableException.class)
  public ResponseEntity<Map<String, Object>> handleServiceUnavailableException(
      ServiceUnavailableException ex) {
    logger.error("Servicio No Disponible: {}", ex.getMessage(), ex);

    Map<String, Object> response =
        Map.of(
            TIMESTAMP_KEY, LocalDateTime.now(),
            STATUS_KEY, HttpStatus.SERVICE_UNAVAILABLE.value(),
            ERROR_KEY, "Servicio No Disponible",
            MESSAGE_KEY, ex.getMessage(),
            PATH_KEY, AUTH_PATH);

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }

  /**
   * Captura fallos de red (timeouts, DNS) hacia otros microservicios, preservando la semántica de
   * servicio no disponible para que los clientes apliquen reintentos o fallback.
   */
  @ExceptionHandler(ResourceAccessException.class)
  public ResponseEntity<Map<String, Object>> handleResourceAccessException(
      ResourceAccessException ex) {
    logger.error("Error de acceso al recurso: {}", ex.getMessage(), ex);

    Map<String, Object> response =
        Map.of(
            TIMESTAMP_KEY,
            LocalDateTime.now(),
            STATUS_KEY,
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            ERROR_KEY,
            "Servicio No Disponible",
            MESSAGE_KEY,
            "No se puede conectar al servicio de usuario. Inténtelo más tarde.",
            PATH_KEY,
            AUTH_PATH);

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }
}
