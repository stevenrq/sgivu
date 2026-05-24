package com.sgivu.purchasesale.repository.projection;

/** Proyección interface-based para GROUP BY payment_method. */
public interface PaymentMethodCount {
  String getMethod();

  Long getCount();
}
