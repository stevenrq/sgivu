package com.sgivu.auth.exception;

/**
 * Excepción lanzada cuando hay un problema al recuperar los detalles del usuario del servicio de
 * usuarios.
 */
public class UserDetailsRetrievalException extends RuntimeException {

  /**
   * Crea la excepción encapsulando el mensaje y la causa original.
   *
   * @param message detalle del fallo de recuperación.
   * @param cause excepción raíz producida por la llamada remota.
   */
  public UserDetailsRetrievalException(String message, Throwable cause) {
    super(message, cause);
  }
}
