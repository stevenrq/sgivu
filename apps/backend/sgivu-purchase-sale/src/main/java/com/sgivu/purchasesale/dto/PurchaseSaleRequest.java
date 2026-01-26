package com.sgivu.purchasesale.dto;

import com.sgivu.purchasesale.enums.ContractStatus;
import com.sgivu.purchasesale.enums.ContractType;
import com.sgivu.purchasesale.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Schema(description = "Payload de entrada para crear o actualizar contratos de compra/venta")
@Getter
@Setter
@ToString
public class PurchaseSaleRequest {

  @NotNull(message = "El ID del cliente es obligatorio.")
  private Long clientId;

  @NotNull(message = "El ID del usuario interno es obligatorio.")
  private Long userId;

  private Long vehicleId;

  @NotNull(message = "El precio de compra es obligatorio.")
  @PositiveOrZero(message = "El precio de compra no puede ser negativo.")
  private Double purchasePrice;

  @NotNull(message = "El precio de venta es obligatorio.")
  @PositiveOrZero(message = "El precio de venta no puede ser negativo.")
  private Double salePrice = 0d;

  @NotNull(message = "El tipo de contrato es obligatorio.")
  private ContractType contractType = ContractType.PURCHASE;

  @NotNull(message = "El estado del contrato es obligatorio.")
  private ContractStatus contractStatus = ContractStatus.PENDING;

  @NotBlank(message = "Las limitaciones de pago son obligatorias.")
  @Size(max = 200, message = "Las limitaciones de pago no pueden exceder 200 caracteres.")
  private String paymentLimitations;

  @NotBlank(message = "Los términos de pago son obligatorios.")
  @Size(max = 200, message = "Los términos de pago no pueden exceder 200 caracteres.")
  private String paymentTerms;

  @NotNull(message = "El método de pago es obligatorio.")
  private PaymentMethod paymentMethod;

  @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres.")
  private String observations;

  @Valid private VehicleCreationRequest vehicleData;
}
