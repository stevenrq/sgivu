package com.sgivu.gateway.exception;

/**
 * Excepción controlada para fallos de trazabilidad distribuida en el Gateway. Permite capturar
 * problemas al crear spans o propagar el traceId sin silenciar errores de negocio.
 */
public class TracingException extends RuntimeException {

  public TracingException(String message, Throwable cause) {
    super(message, cause);
  }

  public TracingException(String message) {
    super(message);
  }
}
