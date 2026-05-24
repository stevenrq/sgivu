package com.sgivu.purchasesale.service.impl;

import com.sgivu.purchasesale.entity.ContractStatusHistory;
import com.sgivu.purchasesale.enums.ContractStatus;
import com.sgivu.purchasesale.repository.ContractStatusHistoryRepository;
import com.sgivu.purchasesale.service.ContractStatusHistoryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ContractStatusHistoryServiceImpl implements ContractStatusHistoryService {

  private final ContractStatusHistoryRepository contractStatusHistoryRepository;

  public ContractStatusHistoryServiceImpl(
      ContractStatusHistoryRepository contractStatusHistoryRepository) {
    this.contractStatusHistoryRepository = contractStatusHistoryRepository;
  }

  @Transactional
  @Override
  public void recordStatusChange(
      Long purchaseSaleId,
      ContractStatus previousStatus,
      ContractStatus newStatus,
      Long changedBy,
      String reason) {
    ContractStatusHistory history = new ContractStatusHistory();
    history.setPurchaseSaleId(purchaseSaleId);
    history.setPreviousStatus(previousStatus);
    history.setNewStatus(newStatus);
    history.setChangedBy(changedBy);
    history.setReason(reason);
    contractStatusHistoryRepository.save(history);
  }

  @Override
  public List<ContractStatusHistory> findByContractId(Long purchaseSaleId) {
    return contractStatusHistoryRepository.findByPurchaseSaleIdOrderByChangedAtAsc(purchaseSaleId);
  }
}
