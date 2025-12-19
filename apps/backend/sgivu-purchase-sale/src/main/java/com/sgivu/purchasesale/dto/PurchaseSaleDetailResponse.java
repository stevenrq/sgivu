package com.sgivu.purchasesale.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * DTO que extiende {@link PurchaseSaleResponse} para incluir detalles de entidades relacionadas
 * como el cliente, el usuario y el vehículo, facilitando la visualización completa de un contrato
 * de compra/venta.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PurchaseSaleDetailResponse extends PurchaseSaleResponse {

  private ClientSummary clientSummary;
  private UserSummary userSummary;
  private VehicleSummary vehicleSummary;
}
