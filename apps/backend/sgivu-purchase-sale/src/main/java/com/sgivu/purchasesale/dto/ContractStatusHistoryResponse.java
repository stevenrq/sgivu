package com.sgivu.purchasesale.dto;

import com.sgivu.purchasesale.enums.ContractStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Registro de un cambio de estado en un contrato")
@Data
@NoArgsConstructor
public class ContractStatusHistoryResponse {

  @Schema(description = "ID del registro", example = "1")
  private Long id;

  @Schema(description = "ID del contrato", example = "5")
  private Long purchaseSaleId;

  @Schema(description = "Estado anterior (null si es creacion)")
  private ContractStatus previousStatus;

  @Schema(description = "Nuevo estado", example = "ACTIVE")
  private ContractStatus newStatus;

  @Schema(description = "ID del usuario que realizo el cambio")
  private Long changedBy;

  @Schema(description = "Fecha y hora del cambio")
  private LocalDateTime changedAt;

  @Schema(description = "Razon del cambio")
  private String reason;
}
