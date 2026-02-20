package com.sgivu.purchasesale.exception;

/**
 * Se lanza cuando un valor de entrada del contrato no cumple con las restricciones de negocio
 * (precios inválidos, IDs nulos, paginación ausente). A diferencia de las validaciones de Bean
 * Validation en el DTO, estas validaciones dependen del contexto de ejecución y del estado actual
 * del sistema.
 */
public class ContractValidationException extends ContractBusinessException {

  public ContractValidationException(String message) {
    super(message);
  }
}
