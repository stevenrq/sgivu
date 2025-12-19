package com.sgivu.user.exception;

/**
 * Excepción de dominio para errores al resolver roles/permisos solicitados.
 *
 * <p>Se utiliza al normalizar los roles enviados por el cliente contra el catálogo persistido; al
 * tratarse de seguridad, preferimos una excepción explícita para auditar eventos anómalos.
 */
public class RoleRetrievalException extends RuntimeException {

  public RoleRetrievalException(String message) {
    super(message);
  }

  public RoleRetrievalException(String message, Throwable cause) {
    super(message, cause);
  }
}
