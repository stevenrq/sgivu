package com.sgivu.client.exception;

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

/**
 * Maneja excepciones transversales del microservicio de clientes, devolviendo respuestas coherentes
 * a otros servicios (inventario, contratos) y al front.
 * Centraliza mensajes para evitar exponer detalles internos en integraciones distribuidas.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String MESSAGE_KEY = "message";
  private static final String DETAILS_KEY = "details";
  private static final String STATUS_KEY = "status";

  /**
   * Manejo de violaciones de restricciones (únicos) típicas cuando se crean clientes duplicados.
   *
   * @param e excepción capturada
   * @return respuesta con detalle de conflicto
   */
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

  /**
   * Captura errores de integridad de datos provenientes de la base (FK, únicos) al interactuar con
   * el inventario o contratos.
   *
   * @param e excepción
   * @return respuesta 409
   */
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

  /**
   * Fallback para cualquier error no controlado, evitando que detalles técnicos escapen a
   * consumidores externos.
   *
   * @param e excepción
   * @return respuesta 500
   */
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

  /**
   * Respuesta estandarizada para accesos sin permisos, usada cuando roles/claims no contienen las
   * autorizaciones requeridas.
   *
   * @param e excepción de autorización
   * @return respuesta 403
   */
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
