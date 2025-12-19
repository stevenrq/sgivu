package com.sgivu.purchasesale.dto;

import com.sgivu.purchasesale.enums.ContractStatus;
import com.sgivu.purchasesale.enums.ContractType;
import com.sgivu.purchasesale.enums.PaymentMethod;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

/**
 * Filtros combinados para búsquedas de contratos. Se utiliza en la capa de especificaciones para
 * construir predicados dinámicos (estado, tipo, rangos de fecha/precio y término libre) sin
 * exponer lógica de queries en el controlador.
 */
@Getter
@Builder
public class PurchaseSaleFilterCriteria {
  private final Long clientId;
  private final Long userId;
  private final Long vehicleId;
  private final ContractType contractType;
  private final ContractStatus contractStatus;
  private final PaymentMethod paymentMethod;
  private final LocalDate startDate;
  private final LocalDate endDate;
  private final Double minPurchasePrice;
  private final Double maxPurchasePrice;
  private final Double minSalePrice;
  private final Double maxSalePrice;
  private final String term;
}
