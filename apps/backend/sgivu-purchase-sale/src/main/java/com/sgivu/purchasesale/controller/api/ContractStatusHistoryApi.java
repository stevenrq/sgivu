package com.sgivu.purchasesale.controller.api;

import com.sgivu.purchasesale.dto.ContractStatusHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(
    name = "Historial de Estados",
    description = "Consulta del historial de cambios de estado de contratos")
@RequestMapping("/v1/contract-status-history")
public interface ContractStatusHistoryApi {

  @Operation(
      summary = "Historial de un contrato",
      description =
          "Obtiene el historial de cambios de estado de un contrato ordenado cronologicamente")
  @ApiResponse(responseCode = "200", description = "Historial encontrado")
  @GetMapping("/contract/{purchaseSaleId}")
  ResponseEntity<List<ContractStatusHistoryResponse>> getByContractId(
      @PathVariable @Parameter(description = "ID del contrato") Long purchaseSaleId);
}
