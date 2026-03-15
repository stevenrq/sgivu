package com.sgivu.auth.exception;

import org.springframework.security.core.AuthenticationException;

public class ServiceUnavailableException extends AuthenticationException {

  public ServiceUnavailableException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
