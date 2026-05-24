package com.sgivu.purchasesale.repository.projection;

/** Proyección interface-based para GROUP BY contract_status. */
public interface ContractStatusCount {
  String getStatus();

  Long getCount();
}
