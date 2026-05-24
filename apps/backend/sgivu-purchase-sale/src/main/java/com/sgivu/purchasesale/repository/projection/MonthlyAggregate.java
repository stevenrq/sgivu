package com.sgivu.purchasesale.repository.projection;

/** Proyección para DATE_TRUNC('month', created_at) + agregados. */
public interface MonthlyAggregate {
  String getMonth();

  Long getCount();

  Double getTotalAmount();
}
