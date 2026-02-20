package com.sgivu.purchasesale.dto;

import com.sgivu.purchasesale.enums.ContractStatus;
import com.sgivu.purchasesale.enums.ContractType;
import com.sgivu.purchasesale.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Schema(
    description =
        "Criterios de búsqueda/paginación para contratos: filtros por tipo, estado, rango de fechas"
            + " y precios")
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
