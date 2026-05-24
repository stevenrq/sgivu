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

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final String TIMESTAMP_KEY = "timestamp";
  private static final String STATUS_KEY = "status";
  private static final String ERROR_KEY = "error";
  private static final String MESSAGE_KEY = "message";
  private static final String PATH_KEY = "path";
  private static final String AUTH_PATH = "/auth";

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> handleConstraintViolationException(
      ConstraintViolationException ex) {
    logger.error("Constraint violation occurred: {}", ex.getMessage(), ex);

    Map<String, Object> response =
        Map.of(
            TIMESTAMP_KEY,
            LocalDateTime.now(),
            STATUS_KEY,
            HttpStatus.CONFLICT.value(),
            ERROR_KEY,
            "Se ha producido una violación de restricción. Verificar campos únicos.",
            PATH_KEY,
            AUTH_PATH);

    return new ResponseEntity<>(response, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(ServiceUnavailableException.class)
  public ResponseEntity<Map<String, Object>> handleServiceUnavailableException(
      ServiceUnavailableException ex) {
    logger.error("Service unavailable: {}", ex.getMessage(), ex);

    Map<String, Object> response =
        Map.of(
            TIMESTAMP_KEY,
            LocalDateTime.now(),
            STATUS_KEY,
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            ERROR_KEY,
            "Servicio No Disponible",
            MESSAGE_KEY,
            "El servicio no está disponible. Inténtelo más tarde.",
            PATH_KEY,
            AUTH_PATH);

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }

  @ExceptionHandler(ResourceAccessException.class)
  public ResponseEntity<Map<String, Object>> handleResourceAccessException(
      ResourceAccessException ex) {
    logger.error("Resource access error: {}", ex.getMessage(), ex);

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
