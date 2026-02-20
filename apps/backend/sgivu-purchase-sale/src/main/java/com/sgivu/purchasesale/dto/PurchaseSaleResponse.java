package com.sgivu.purchasesale.dto;

import com.sgivu.purchasesale.enums.ContractStatus;
import com.sgivu.purchasesale.enums.ContractType;
import com.sgivu.purchasesale.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Respuesta estándar de contratos orientada a listados y reportes básicos")
@Data
@NoArgsConstructor
public class PurchaseSaleResponse {
  private Long id;
  private Long clientId;
  private Long userId;
  private Long vehicleId;
  private Double purchasePrice;
  private Double salePrice;
  private ContractType contractType;
  private ContractStatus contractStatus;
  private String paymentLimitations;
  private String paymentTerms;
  private PaymentMethod paymentMethod;
  private String observations;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
