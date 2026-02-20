package com.sgivu.purchasesale.service;

import com.sgivu.purchasesale.dto.PurchaseSaleRequest;
import com.sgivu.purchasesale.entity.PurchaseSale;
import com.sgivu.purchasesale.enums.ContractStatus;
import com.sgivu.purchasesale.enums.ContractType;
import com.sgivu.purchasesale.exception.ContractValidationException;
import com.sgivu.purchasesale.exception.DuplicateContractException;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Servicio que encapsula las reglas de negocio que determinan si un contrato de compra-venta puede
 * crearse o actualizarse. Las reglas principales son:
 *
 * <ul>
 *   <li>Un vehículo no puede tener más de una compra activa/pendiente simultáneamente (evita doble
 *       registro en inventario).
 *   <li>Una venta requiere que exista al menos una compra ACTIVE/COMPLETED para el vehículo (no se
 *       puede vender lo que no se ha comprado).
 *   <li>No puede haber más de una venta activa/pendiente/completada por vehículo (un vehículo se
 *       vende una sola vez por ciclo).
 *   <li>El precio de compra de una venta se toma de la compra más reciente usando {@code updatedAt}
 *       (no {@code createdAt}) porque un contrato puede renegociarse después de su creación.
 * </ul>
 */
@Service
public class ContractBusinessRuleService {

  /**
   * Aplica las reglas de negocio correspondientes al tipo de contrato, preparando el request y
   * validando contra contratos existentes del mismo vehículo.
   *
   * @param contractType tipo de contrato (PURCHASE o SALE)
   * @param request datos del contrato a crear/actualizar
   * @param contractsByVehicle contratos existentes para el mismo vehículo
   * @param excludedContractId ID del contrato propio (para excluirse en actualizaciones), o null
   * @param vehicleId ID del vehículo (para mensajes de error)
   */
  public void applyRules(
      ContractType contractType,
      PurchaseSaleRequest request,
      List<PurchaseSale> contractsByVehicle,
      Long excludedContractId,
      Long vehicleId) {
    if (contractType == ContractType.PURCHASE) {
      preparePurchaseRequest(request);
      ensureNoActivePurchase(contractsByVehicle, excludedContractId, vehicleId);
    } else {
      prepareSaleRequest(request, contractsByVehicle);
      ensureSalePrerequisites(
          contractsByVehicle, excludedContractId, vehicleId, request.getContractStatus());
    }
  }

  /**
   * Resuelve el salePrice para compras. En una compra el precio de venta es informativo (se
   * definirá al momento de vender), por eso se permite 0 como valor predeterminado. Se prioriza el
   * precio del bloque vehicleData sobre el del contrato porque vehicleData contiene los datos más
   * específicos del vehículo.
   *
   * @param request request de compra que puede contener salePrice en el bloque principal o en
   *     vehicleData
   */
  public void preparePurchaseRequest(PurchaseSaleRequest request) {
    Double targetSalePrice = null;
    if (request.getVehicleData() != null) {
      targetSalePrice = request.getVehicleData().getSalePrice();
    }
    if (targetSalePrice == null) {
      targetSalePrice = request.getSalePrice();
    }
    request.setSalePrice(targetSalePrice != null ? targetSalePrice : 0d);
  }

  /**
   * Valida y prepara el request para una venta. El precio de venta debe ser positivo (es
   * obligatorio), y el precio de compra se obtiene de la última compra válida para reflejar el
   * costo real de adquisición.
   *
   * @param request request de venta que debe contener salePrice y puede contener purchasePrice como
   *     fallback
   * @param contractsByVehicle histórico de contratos del vehículo para resolver el precio de compra
   *     y validar la existencia de compras válidas
   */
  public void prepareSaleRequest(
      PurchaseSaleRequest request, List<PurchaseSale> contractsByVehicle) {
    Double salePrice = request.getSalePrice();
    if (salePrice == null || salePrice <= 0) {
      throw new ContractValidationException("El precio de venta debe ser mayor a cero.");
    }
    request.setPurchasePrice(
        findLatestPurchasePrice(contractsByVehicle, request.getPurchasePrice()));
  }

  /**
   * Busca el precio de compra más reciente entre los contratos de compra ACTIVE/COMPLETED. Se usa
   * {@code updatedAt} (no {@code createdAt}) porque un contrato puede renegociarse y actualizarse
   * tras su creación. Si no hay compras válidas, intenta el fallback proporcionado por el usuario.
   *
   * @param contractsByVehicle histórico de contratos del vehículo
   * @param fallbackPurchasePrice precio alternativo proporcionado directamente en el request
   * @return el precio de compra a usar en el contrato de venta
   * @throws ContractValidationException si no hay compras válidas y el fallback es inválido
   */
  public Double findLatestPurchasePrice(
      List<PurchaseSale> contractsByVehicle, Double fallbackPurchasePrice) {
    return contractsByVehicle.stream()
        .filter(contract -> contract.getContractType() == ContractType.PURCHASE)
        .filter(
            contract ->
                EnumSet.of(ContractStatus.ACTIVE, ContractStatus.COMPLETED)
                    .contains(contract.getContractStatus()))
        .max(
            Comparator.comparing(
                PurchaseSale::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
        .map(PurchaseSale::getPurchasePrice)
        .orElseGet(
            () -> {
              if (fallbackPurchasePrice != null && fallbackPurchasePrice > 0) {
                return fallbackPurchasePrice;
              }
              throw new ContractValidationException(
                  "No se encontró una compra válida asociada al vehículo.");
            });
  }

  /**
   * Garantiza que no exista otra compra activa o pendiente para el mismo vehículo. Un vehículo solo
   * puede tener una compra vigente a la vez para evitar doble registro en inventario y ambigüedad
   * en la trazabilidad del precio.
   *
   * @param contractsByVehicle contratos existentes del vehículo
   * @param excludedContractId contrato a excluir (el propio durante actualización)
   * @param vehicleId ID del vehículo (para mensaje de error)
   * @throws DuplicateContractException si ya existe una compra PENDING o ACTIVE
   */
  public void ensureNoActivePurchase(
      List<PurchaseSale> contractsByVehicle, Long excludedContractId, Long vehicleId) {
    boolean hasActiveOrPendingPurchase =
        contractsByVehicle.stream()
            .filter(
                existing ->
                    !Objects.equals(excludedContractId, existing.getId())
                        && existing.getContractType() == ContractType.PURCHASE)
            .anyMatch(
                existing ->
                    existing.getContractStatus() == ContractStatus.PENDING
                        || existing.getContractStatus() == ContractStatus.ACTIVE);

    if (hasActiveOrPendingPurchase) {
      throw new DuplicateContractException(
          "El vehículo con id "
              + vehicleId
              + " ya tiene una compra registrada con estado pendiente o activa.");
    }
  }

  /**
   * Valida los prerrequisitos para registrar una venta:
   *
   * <ol>
   *   <li>Debe existir al menos una compra ACTIVE/COMPLETED (no se puede vender sin stock).
   *   <li>No debe existir otra venta en estado PENDING/ACTIVE/COMPLETED (un vehículo se vende una
   *       sola vez por ciclo de negocio).
   * </ol>
   *
   * La validación de disponibilidad solo aplica cuando el estado objetivo indica una venta "real"
   * (PENDING, ACTIVE, COMPLETED). Un contrato CANCELED no requiere validación de stock.
   *
   * @param contractsByVehicle contratos existentes del vehículo
   * @param excludedContractId contrato a excluir (el propio durante actualización)
   * @param vehicleId ID del vehículo (para mensaje de error)
   * @param targetStatus estado objetivo del contrato de venta
   */
  public void ensureSalePrerequisites(
      List<PurchaseSale> contractsByVehicle,
      Long excludedContractId,
      Long vehicleId,
      ContractStatus targetStatus) {
    boolean shouldValidateAvailability =
        EnumSet.of(ContractStatus.PENDING, ContractStatus.ACTIVE, ContractStatus.COMPLETED)
            .contains(targetStatus);

    if (shouldValidateAvailability) {
      boolean hasAvailableStock =
          contractsByVehicle.stream()
              .filter(contract -> contract.getContractType() == ContractType.PURCHASE)
              .anyMatch(
                  contract ->
                      EnumSet.of(ContractStatus.ACTIVE, ContractStatus.COMPLETED)
                          .contains(contract.getContractStatus()));

      if (!hasAvailableStock) {
        throw new ContractValidationException(
            "El vehículo con id "
                + vehicleId
                + " no cuenta con una compra activa o completada registrada.");
      }
    }

    boolean hasConflictingSale =
        contractsByVehicle.stream()
            .filter(
                contract ->
                    !Objects.equals(excludedContractId, contract.getId())
                        && contract.getContractType() == ContractType.SALE)
            .anyMatch(
                contract ->
                    EnumSet.of(
                            ContractStatus.PENDING, ContractStatus.ACTIVE, ContractStatus.COMPLETED)
                        .contains(contract.getContractStatus()));

    if (hasConflictingSale) {
      throw new DuplicateContractException(
          "El vehículo con id "
              + vehicleId
              + " ya cuenta con una venta registrada en estado pendiente, activa o completada.");
    }
  }
}
