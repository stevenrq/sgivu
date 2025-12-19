package com.sgivu.auth.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Excepción de autenticación que indica indisponibilidad del servicio remoto de usuarios.
 *
 * <p>Usada como fallback cuando el circuito de Resilience4j detecta fallos al consultar
 * {@code sgivu-user}, evitando exponer detalles de red al flujo de login.
 */
public class ServiceUnavailableException extends AuthenticationException {

  /**
   * Construye la excepción con mensaje y causa original.
   *
   * @param msg descripción legible para el usuario final.
   * @param cause causa técnica del fallo remoto.
   */
  public ServiceUnavailableException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
