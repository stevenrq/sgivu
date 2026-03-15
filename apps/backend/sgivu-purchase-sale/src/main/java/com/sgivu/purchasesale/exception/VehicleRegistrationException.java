package com.sgivu.purchasesale.exception;

/**
 * Se lanza cuando los datos proporcionados para registrar un vehículo nuevo durante una compra son
 * incompletos o inválidos. La validación se realiza en el servicio en lugar de Bean Validation
 * porque los campos requeridos varían según el tipo de vehículo (auto vs motocicleta) y el contexto
 * del contrato.
 */
public class VehicleRegistrationException extends ContractBusinessException {

  public VehicleRegistrationException(String message) {
    super(message);
  }
}
