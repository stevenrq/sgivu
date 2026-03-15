package com.sgivu.purchasesale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Schema(
    description =
        "Detalle completo de un contrato de compra/venta con datos relacionados de cliente, usuario"
            + " y vehículo")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PurchaseSaleDetailResponse extends PurchaseSaleResponse {

  private ClientSummary clientSummary;
  private UserSummary userSummary;
  private VehicleSummary vehicleSummary;
}
