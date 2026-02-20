package com.sgivu.purchasesale.exception;

/**
 * Se lanza cuando se intenta crear o actualizar un contrato que entraría en conflicto con otro
 * contrato existente para el mismo vehículo. Un vehículo solo puede tener una compra activa/
 * pendiente y una venta activa/pendiente/completada a la vez, para evitar inconsistencias en el
 * inventario y doble contabilización de transacciones.
 */
public class DuplicateContractException extends ContractBusinessException {

  public DuplicateContractException(String message) {
    super(message);
  }
}
