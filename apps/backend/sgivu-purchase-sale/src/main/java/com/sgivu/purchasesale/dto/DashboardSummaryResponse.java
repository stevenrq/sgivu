package com.sgivu.purchasesale.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Snapshot agregado que consume el dashboard del frontend. Se construye con consultas GROUP BY
 * directas sobre purchase_sales + un fanout a sgivu-vehicle (conteos). Se cachea 60s en Redis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse implements Serializable {

  private static final long serialVersionUID = 1L;

  /** Momento en que se calculó el snapshot (útil para el cliente y para debugging de cache). */
  private LocalDateTime generatedAt;

  /** Conteos de contratos por estado (PENDING/ACTIVE/COMPLETED/CANCELLED). */
  private Map<String, Long> contractStatusCounts;

  /** Distribución de contratos por método de pago (CASH/CREDIT/...). */
  private Map<String, Long> paymentMethodCounts;

  /** Ventas mensuales (ContractType=SALE, COMPLETED). */
  private List<MonthlyBucket> monthlySales;

  /** Compras mensuales (ContractType=PURCHASE, COMPLETED). */
  private List<MonthlyBucket> monthlyPurchases;

  /** Actividad reciente (últimos 20 contratos por createdAt desc). */
  private List<RecentActivityItem> recentActivity;

  /** Conteos de vehículos obtenidos de sgivu-vehicle (total/disponibles/no disponibles). */
  private VehicleCounts vehicleCounts;

  /** Métricas globales: total de contratos, ingresos totales, etc. */
  private GlobalMetrics globalMetrics;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MonthlyBucket implements Serializable {
    private static final long serialVersionUID = 1L;
    private String month;
    private Long count;
    private Double totalAmount;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecentActivityItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long contractId;
    private String contractType;
    private String contractStatus;
    private Double amount;
    private LocalDateTime createdAt;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VehicleCounts implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long totalCars;
    private Long availableCars;
    private Long totalMotorcycles;
    private Long availableMotorcycles;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GlobalMetrics implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long totalContracts;
    private Double totalRevenue;
    private Double totalInvestment;
  }
}
