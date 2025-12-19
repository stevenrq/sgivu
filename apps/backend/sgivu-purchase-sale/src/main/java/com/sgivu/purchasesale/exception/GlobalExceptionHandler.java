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
 * Maneja de forma consistente las excepciones de validación, negocio y comunicación con servicios
 * externos. Permite a los consumidores del microservicio recibir mensajes claros cuando fallan
 * reglas de contrato o integraciones entre microservicios.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String MESSAGE_KEY = "message";
  private static final String DETAILS_KEY = "details";
  private static final String STATUS_KEY = "status";

  /**
   * Normaliza respuestas de validación de Bean Validation, devolviendo el mapa campo->mensaje para
   * que el front-end pueda mostrar errores precisos al capturar contratos.
   */
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
   * Retorna un mensaje claro cuando el servicio detecta argumentos inválidos o reglas incumplidas.
   *
   * @param exception excepción de negocio con el detalle de la validación
   * @return respuesta HTTP 400 con el mensaje específico
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException exception) {
    logger.warn("Solicitud inválida: {}", exception.getMessage());

    Map<String, Object> body = new HashMap<>();
    body.put(MESSAGE_KEY, "Solicitud inválida.");
    body.put(DETAILS_KEY, exception.getMessage());
    body.put(STATUS_KEY, HttpStatus.BAD_REQUEST.value());
    return ResponseEntity.badRequest().body(body);
  }

  /**
   * Captura errores al invocar microservicios externos (clientes, usuarios, vehículos) y devuelve
   * el body remoto para facilitar trazabilidad en auditorías.
   */
  @ExceptionHandler(HttpClientErrorException.class)
  public ResponseEntity<Object> handleHttpClientErrorException(HttpClientErrorException exception) {
    logger.error(
        "Error al comunicarse con servicios externos: {} - {}",
        exception.getStatusCode(),
        exception.getMessage());

    Map<String, Object> body = new HashMap<>();
    body.put(MESSAGE_KEY, "Error al validar datos externos.");
    body.put(DETAILS_KEY, exception.getResponseBodyAsString());
    body.put(STATUS_KEY, exception.getStatusCode().value());

    return ResponseEntity.status(exception.getStatusCode()).body(body);
  }

  /**
   * Maneja intentos de acceso sin autorización suficiente, ya sea por ausencia de JWT o permisos.
   *
   * @param exception excepción lanzada por el AuthorizationManager
   * @return respuesta HTTP 403 con detalle del motivo
   */
  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<Object> handleAuthorizationDeniedException(
      AuthorizationDeniedException exception) {
    logger.warn("Acceso denegado: {}", exception.getMessage());

    Map<String, Object> body = new HashMap<>();
    body.put(MESSAGE_KEY, "Acceso denegado. Permisos insuficientes.");
    body.put(DETAILS_KEY, exception.getMessage());
    body.put(STATUS_KEY, HttpStatus.FORBIDDEN.value());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
  }

  /**
   * Fallback genérico ante errores inesperados para evitar exponer stacktraces en las respuestas.
   *
   * @param exception excepción no controlada
   * @return respuesta HTTP 500 con mensaje de error interno
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleGeneralException(Exception exception) {
    logger.error("Error inesperado.", exception);

    Map<String, Object> body = new HashMap<>();
    body.put(MESSAGE_KEY, "Error interno del servidor.");
    body.put(DETAILS_KEY, exception.getMessage());
    body.put(STATUS_KEY, HttpStatus.INTERNAL_SERVER_ERROR.value());
    return ResponseEntity.internalServerError().body(body);
  }
}
