package com.sgivu.purchasesale.controller;

import com.sgivu.purchasesale.controller.api.ContractStatusHistoryApi;
import com.sgivu.purchasesale.dto.ContractStatusHistoryResponse;
import com.sgivu.purchasesale.mapper.ContractStatusHistoryMapper;
import com.sgivu.purchasesale.service.ContractStatusHistoryService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContractStatusHistoryController implements ContractStatusHistoryApi {

  private final ContractStatusHistoryService contractStatusHistoryService;
  private final ContractStatusHistoryMapper contractStatusHistoryMapper;

  public ContractStatusHistoryController(
      ContractStatusHistoryService contractStatusHistoryService,
      ContractStatusHistoryMapper contractStatusHistoryMapper) {
    this.contractStatusHistoryService = contractStatusHistoryService;
    this.contractStatusHistoryMapper = contractStatusHistoryMapper;
  }

  @Override
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<List<ContractStatusHistoryResponse>> getByContractId(Long purchaseSaleId) {
    List<ContractStatusHistoryResponse> responses =
        contractStatusHistoryService.findByContractId(purchaseSaleId).stream()
            .map(contractStatusHistoryMapper::toResponse)
            .toList();
    return ResponseEntity.ok(responses);
  }
}
