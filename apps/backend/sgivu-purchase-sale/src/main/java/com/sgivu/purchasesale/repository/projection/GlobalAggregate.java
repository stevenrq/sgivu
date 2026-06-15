package com.sgivu.purchasesale.repository.projection;

/** Proyección para totales globales: count, sum(sale_price), sum(purchase_price). */
public interface GlobalAggregate {
  Long getTotalContracts();

  Double getTotalRevenue();

  Double getTotalInvestment();
}
