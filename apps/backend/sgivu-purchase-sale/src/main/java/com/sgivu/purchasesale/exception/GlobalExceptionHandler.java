package com.sgivu.purchasesale.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Manejador global de excepciones que traduce errores de dominio y técnicos a respuestas HTTP
 * estructuradas con código de estado semántico apropiado.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String MESSAGE_KEY = "message";
  private static final String DETAILS_KEY = "details";
  private static final String STATUS_KEY = "status";

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException exception) {
    Map<String, Object> body = new HashMap<>();
    body.put(MESSAGE_KEY, "Error de validación en la solicitud.");
    body.put(
        DETAILS_KEY,
        exception.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)));
    body.put(STATUS_KEY, HttpStatus.BAD_REQUEST.value());

    return ResponseEntity.badRequest().body(body);
  }

  /**
   * Entidades remotas no encontradas tras agotar todos los endpoints polimórficos disponibles
   * (persona/empresa, auto/motocicleta).
   */
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<Object> handleEntityNotFoundException(EntityNotFoundException exception) {
    logger.warn("Remote entity not found: {}", exception.getMessage());

    Map<String, Object> body = new HashMap<>();
    body.put(MESSAGE_KEY, "Recurso no encontrado.");
    body.put(DETAILS_KEY, exception.getMessage());
    body.put(STATUS_KEY, HttpStatus.NOT_FOUND.value());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  /** Conflictos de duplicidad: el vehículo ya tiene un contrato activo del mismo tipo. */
  @ExceptionHandler(DuplicateContractException.class)
  public ResponseEntity<Object> handleDuplicateContractException(
      DuplicateContractException exception) {
    logger.warn("Duplicate contract conflict: {}", exception.getMessage());

    Map<String, Object> body = new HashMap<>();
    body.put(MESSAGE_KEY, "Conflicto con contrato existente.");
    body.put(DETAILS_KEY, exception.getMessage());
    body.put(STATUS_KEY, HttpStatus.CONFLICT.value());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  /** Operaciones no permitidas sobre un contrato (cambio de tipo, eliminación sin cancelar). */
  @ExceptionHandler(InvalidContractOperationException.class)
  public ResponseEntity<Object> handleInvalidContractOperationException(
      InvalidContractOperationException exception) {
    logger.warn("Invalid contract operation: {}", exception.getMessage());

    Map<String, Object> body = new HashMap<>();
    body.put(MESSAGE_KEY, "Operación no permitida.");
    body.put(DETAILS_KEY, exception.getMessage());
    body.put(STATUS_KEY, HttpStatus.BAD_REQUEST.value());
    return ResponseEntity.badRequest().body(body);
  }

  /** Datos incompletos o inválidos para registrar un vehículo nuevo durante una compra. */
  @ExceptionHandler(VehicleRegistrationException.class)
  public ResponseEntity<Object> handleVehicleRegistrationException(
      VehicleRegistrationException exception) {
    logger.warn("Vehicle registration failed: {}", exception.getMessage());

    Map<String, Object> body = new HashMap<>();
    body.put(MESSAGE_KEY, "Error al registrar el vehículo.");
    body.put(DETAILS_KEY, exception.getMessage());
    body.put(STATUS_KEY, HttpStatus.UNPROCESSABLE_ENTITY.value());
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
  }

  /** Validaciones de negocio del contrato (precios, IDs, paginación). */
  @ExceptionHandler(ContractValidationException.class)
  public ResponseEntity<Object> handleContractValidationException(
      ContractValidationException exception) {
    logger.warn("Contract validation failed: {}", exception.getMessage());

    Map<String, Object> body = new HashMap<>();
    body.put(MESSAGE_KEY, "Error de validación del contrato.");
    body.put(DETAILS_KEY, exception.getMessage());
    body.put(STATUS_KEY, HttpStatus.UNPROCESSABLE_ENTITY.value());
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException exception) {
    logger.warn("Invalid request: {}", exception.getMessage());

    Map<String, Object> body = new HashMap<>();
    body.put(MESSAGE_KEY, "Solicitud inválida.");
    body.put(DETAILS_KEY, exception.getMessage());
    body.put(STATUS_KEY, HttpStatus.BAD_REQUEST.value());
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(HttpClientErrorException.class)
  public ResponseEntity<Object> handleHttpClientErrorException(HttpClientErrorException exception) {
    logger.error(
        "Error communicating with external services: {} - {}",
        exception.getStatusCode(),
        exception.getMessage());

    Map<String, Object> body = new HashMap<>();
    body.put(MESSAGE_KEY, "Error al validar datos externos.");
    body.put(STATUS_KEY, exception.getStatusCode().value());

    return ResponseEntity.status(exception.getStatusCode()).body(body);
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<Object> handleAuthorizationDeniedException(
      AuthorizationDeniedException exception) {
    logger.warn("Access denied: {}", exception.getMessage());

    Map<String, Object> body = new HashMap<>();
    body.put(MESSAGE_KEY, "Acceso denegado. Permisos insuficientes.");
    body.put(STATUS_KEY, HttpStatus.FORBIDDEN.value());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleGeneralException(Exception exception) {
    logger.error("Unexpected error.", exception);

    Map<String, Object> body = new HashMap<>();
    body.put(MESSAGE_KEY, "Error interno del servidor. Intente más tarde.");
    body.put(STATUS_KEY, HttpStatus.INTERNAL_SERVER_ERROR.value());
    return ResponseEntity.internalServerError().body(body);
  }
}
