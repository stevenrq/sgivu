package com.sgivu.user.exception;

/** Excepción personalizada lanzada cuando hay un error al recuperar roles de usuario. */
public class RoleRetrievalException extends RuntimeException {

  public RoleRetrievalException(String message) {
    super(message);
  }

  public RoleRetrievalException(String message, Throwable cause) {
    super(message, cause);
  }
}
