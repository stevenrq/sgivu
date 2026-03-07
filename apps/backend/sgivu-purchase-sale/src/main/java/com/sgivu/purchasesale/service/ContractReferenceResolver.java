package com.sgivu.purchasesale.service;

import com.sgivu.purchasesale.dto.PurchaseSaleRequest;
import com.sgivu.purchasesale.enums.ContractType;
import com.sgivu.purchasesale.exception.InvalidContractOperationException;
import org.springframework.stereotype.Service;

/**
 * Servicio que determina cómo obtener la referencia al vehículo según el tipo de contrato.
 *
 * <p>En una <strong>compra</strong>, el vehículo puede no existir aún en inventario: si el request
 * incluye {@code vehicleId} se verifica su existencia, y si no lo incluye se crea un vehículo nuevo
 * con los datos de {@code vehicleData}. En una <strong>venta</strong>, el vehículo ya debe existir
 * (no se puede vender lo que no se ha registrado) y no se aceptan datos de creación.
 *
 * <p>Esta clase orquesta {@link EntityResolutionService} y {@link VehicleRegistrationService} sin
 * tomar decisiones de negocio sobre el contrato en sí.
 */
@Service
public class ContractReferenceResolver {

  private final EntityResolutionService entityResolutionService;
  private final VehicleRegistrationService vehicleRegistrationService;

  public ContractReferenceResolver(
      EntityResolutionService entityResolutionService,
      VehicleRegistrationService vehicleRegistrationService) {
    this.entityResolutionService = entityResolutionService;
    this.vehicleRegistrationService = vehicleRegistrationService;
  }

  /**
   * Resuelve o crea la referencia al vehículo asociado al contrato.
   *
   * <p>Para compras: si {@code vehicleId} está presente se verifica; si no, se registra un vehículo
   * nuevo con {@code vehicleData} y se asigna el ID al request.
   *
   * <p>Para ventas: {@code vehicleId} es obligatorio y {@code vehicleData} está prohibido.
   *
   * @param contractType tipo de contrato
   * @param request request del contrato que puede contener vehicleId y/o vehicleData
   * @return el ID del vehículo resuelto o recién creado
   * @throws InvalidContractOperationException si una venta no tiene vehicleId o incluye vehicleData
   */
  public Long resolveVehicleReference(ContractType contractType, PurchaseSaleRequest request) {
    if (contractType == ContractType.PURCHASE) {
      return resolvePurchaseVehicle(request);
    }
    return resolveSaleVehicle(request);
  }

  private Long resolvePurchaseVehicle(PurchaseSaleRequest request) {
    if (request.getVehicleId() != null) {
      return entityResolutionService.resolveVehicleId(request.getVehicleId());
    }
    Long vehicleId = vehicleRegistrationService.registerVehicle(request);
    request.setVehicleId(vehicleId);
    return vehicleId;
  }

  private Long resolveSaleVehicle(PurchaseSaleRequest request) {
    if (request.getVehicleId() == null) {
      throw new InvalidContractOperationException(
          "Debes seleccionar el vehículo asociado al contrato de venta.");
    }
    if (request.getVehicleData() != null) {
      throw new InvalidContractOperationException(
          "Los datos detallados del vehículo solo deben enviarse para contratos de compra.");
    }
    return entityResolutionService.resolveVehicleId(request.getVehicleId());
  }
}
