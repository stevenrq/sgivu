package com.sgivu.purchasesale.exception;

/**
 * Se lanza cuando se intenta realizar una operación no permitida sobre un contrato, como cambiar su
 * tipo una vez creado o eliminar un contrato que no está en estado cancelado. Estas restricciones
 * existen para mantener la integridad del historial transaccional.
 */
public class InvalidContractOperationException extends ContractBusinessException {

  public InvalidContractOperationException(String message) {
    super(message);
  }
}
