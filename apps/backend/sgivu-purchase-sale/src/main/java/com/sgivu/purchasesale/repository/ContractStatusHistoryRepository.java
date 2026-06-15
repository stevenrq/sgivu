package com.sgivu.purchasesale.repository;

import com.sgivu.purchasesale.entity.ContractStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractStatusHistoryRepository
    extends JpaRepository<ContractStatusHistory, Long> {
  List<ContractStatusHistory> findByPurchaseSaleIdOrderByChangedAtAsc(Long purchaseSaleId);
}
