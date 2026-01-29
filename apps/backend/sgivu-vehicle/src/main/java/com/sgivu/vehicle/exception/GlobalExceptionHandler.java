package com.sgivu.vehicle.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String MESSAGE_KEY = "message";
  private static final String DETAILS_KEY = "details";
  private static final String STATUS_KEY = "status";

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException e) {
    logger.error("Ocurrió una violación de restricción: {}", e.getMessage(), e);

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put(
        MESSAGE_KEY, "Se produjo una violación de restricción. Verifique los campos únicos.");
    errorResponse.put(DETAILS_KEY, e.getMessage());
    errorResponse.put("constraintViolations", e.getConstraintViolations());
    errorResponse.put(STATUS_KEY, HttpStatus.CONFLICT.value());

    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Object> handleDataIntegrityViolationException(
      DataIntegrityViolationException e) {
    logger.error("Ocurrió una violación de integridad de datos: {}", e.getMessage(), e);

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put(
        MESSAGE_KEY,
        "Se produjo una violación de integridad de datos. Verifique los campos únicos.");
    errorResponse.put(DETAILS_KEY, e.getMessage());
    errorResponse.put(STATUS_KEY, HttpStatus.CONFLICT.value());

    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleGeneralException(Exception e) {
    logger.error("Ocurrió un error inesperado: {}", e.getMessage(), e);

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("exceptionType", e.getClass().getSimpleName());
    errorResponse.put(MESSAGE_KEY, "Ocurrió un error inesperado.");
    errorResponse.put(DETAILS_KEY, e.getMessage());
    errorResponse.put(STATUS_KEY, HttpStatus.INTERNAL_SERVER_ERROR.value());

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<Object> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
    logger.warn("Acceso denegado: {}", e.getMessage());

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("exceptionType", "AuthorizationDeniedException");
    errorResponse.put(
        MESSAGE_KEY,
        "Acceso denegado. No tiene los permisos necesarios para acceder a este recurso.");
    errorResponse.put(DETAILS_KEY, "Permisos insuficientes");
    errorResponse.put(STATUS_KEY, HttpStatus.FORBIDDEN.value());

    return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
  }
}
