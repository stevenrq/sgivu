package com.sgivu.purchasesale.service;

import com.sgivu.purchasesale.entity.ContractStatusHistory;
import com.sgivu.purchasesale.enums.ContractStatus;
import java.util.List;

public interface ContractStatusHistoryService {
  void recordStatusChange(
      Long purchaseSaleId,
      ContractStatus previousStatus,
      ContractStatus newStatus,
      Long changedBy,
      String reason);

  List<ContractStatusHistory> findByContractId(Long purchaseSaleId);
}
