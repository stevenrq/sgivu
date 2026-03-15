package com.sgivu.purchasesale.exception;

/**
 * Excepción base para todos los errores de reglas de negocio del dominio de contratos. Permite
 * distinguir semánticamente los errores de negocio de errores genéricos como {@link
 * IllegalArgumentException}, facilitando el mapeo a códigos HTTP específicos en el {@link
 * GlobalExceptionHandler}.
 */
public abstract class ContractBusinessException extends RuntimeException {

  protected ContractBusinessException(String message) {
    super(message);
  }

  protected ContractBusinessException(String message, Throwable cause) {
    super(message, cause);
  }
}
