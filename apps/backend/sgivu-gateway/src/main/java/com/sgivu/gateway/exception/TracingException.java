package com.sgivu.gateway.exception;

/** Excepción personalizada para errores relacionados con el trazado (tracing) en la aplicación. */
public class TracingException extends RuntimeException {

  public TracingException(String message, Throwable cause) {
    super(message, cause);
  }

  public TracingException(String message) {
    super(message);
  }
}
