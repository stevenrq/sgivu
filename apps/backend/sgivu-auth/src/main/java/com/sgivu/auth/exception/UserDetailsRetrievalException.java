package com.sgivu.auth.exception;

/**
 * Excepción lanzada cuando hay un fallo al recuperar los detalles del usuario desde el servicio
 * remoto.
 */
public class UserDetailsRetrievalException extends RuntimeException {

  public UserDetailsRetrievalException(String message, Throwable cause) {
    super(message, cause);
  }
}
