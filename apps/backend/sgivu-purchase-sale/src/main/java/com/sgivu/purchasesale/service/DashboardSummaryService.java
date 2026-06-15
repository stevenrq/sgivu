package com.sgivu.purchasesale.service;

import com.sgivu.purchasesale.client.VehicleServiceClient;
import com.sgivu.purchasesale.config.CacheConfig;
import com.sgivu.purchasesale.dto.DashboardSummaryResponse;
import com.sgivu.purchasesale.dto.DashboardSummaryResponse.GlobalMetrics;
import com.sgivu.purchasesale.dto.DashboardSummaryResponse.MonthlyBucket;
import com.sgivu.purchasesale.dto.DashboardSummaryResponse.RecentActivityItem;
import com.sgivu.purchasesale.dto.DashboardSummaryResponse.VehicleCounts;
import com.sgivu.purchasesale.entity.PurchaseSale;
import com.sgivu.purchasesale.repository.PurchaseSaleRepository;
import com.sgivu.purchasesale.repository.projection.ContractStatusCount;
import com.sgivu.purchasesale.repository.projection.GlobalAggregate;
import com.sgivu.purchasesale.repository.projection.MonthlyAggregate;
import com.sgivu.purchasesale.repository.projection.PaymentMethodCount;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Construye el snapshot del dashboard con queries GROUP BY + un fanout a sgivu-vehicle para
 * conteos. Evita cargar todos los contratos en memoria.
 */
@Service
@Transactional(readOnly = true)
public class DashboardSummaryService {

  private static final int RECENT_ACTIVITY_LIMIT = 20;
  private static final Executor EXECUTOR =
      new DelegatingSecurityContextExecutor(ForkJoinPool.commonPool());

  private final PurchaseSaleRepository purchaseSaleRepository;
  private final VehicleServiceClient vehicleServiceClient;

  public DashboardSummaryService(
      PurchaseSaleRepository purchaseSaleRepository, VehicleServiceClient vehicleServiceClient) {
    this.purchaseSaleRepository = purchaseSaleRepository;
    this.vehicleServiceClient = vehicleServiceClient;
  }

  /**
   * Snapshot del dashboard. Cacheado 60s en Caffeine (ver {@link CacheConfig}). Invalidado en
   * create/update/delete de {@code PurchaseSaleServiceImpl}.
   */
  @Cacheable(value = CacheConfig.DASHBOARD_SUMMARY_CACHE, key = "'global'")
  public DashboardSummaryResponse getSummary() {
    // Los conteos de vehículos se resuelven en paralelo porque cruzan red; las queries
    // locales al mismo DataSource se ejecutan secuencialmente para no saturar el pool.
    CompletableFuture<VehicleCounts> vehicleCountsFuture =
        CompletableFuture.supplyAsync(this::fetchVehicleCounts, EXECUTOR);

    Map<String, Long> statusCounts =
        toStatusCountsMap(purchaseSaleRepository.countByContractStatus());
    Map<String, Long> paymentCounts =
        toPaymentCountsMap(purchaseSaleRepository.countByPaymentMethod());
    List<MonthlyBucket> monthlySales =
        toMonthlyBuckets(purchaseSaleRepository.aggregateMonthlyByType("SALE"));
    List<MonthlyBucket> monthlyPurchases =
        toMonthlyBuckets(purchaseSaleRepository.aggregateMonthlyByType("PURCHASE"));
    List<RecentActivityItem> recent =
        toRecentActivity(
            purchaseSaleRepository.findRecentActivity(PageRequest.of(0, RECENT_ACTIVITY_LIMIT)));
    GlobalMetrics globals = toGlobalMetrics(purchaseSaleRepository.aggregateGlobals());

    return DashboardSummaryResponse.builder()
        .generatedAt(LocalDateTime.now())
        .contractStatusCounts(statusCounts)
        .paymentMethodCounts(paymentCounts)
        .monthlySales(monthlySales)
        .monthlyPurchases(monthlyPurchases)
        .recentActivity(recent)
        .vehicleCounts(vehicleCountsFuture.join())
        .globalMetrics(globals)
        .build();
  }

  private Map<String, Long> toStatusCountsMap(List<ContractStatusCount> rows) {
    Map<String, Long> result = new LinkedHashMap<>();
    for (ContractStatusCount row : rows) {
      if (row.getStatus() != null) {
        result.put(row.getStatus(), row.getCount() == null ? 0L : row.getCount());
      }
    }
    return result;
  }

  private Map<String, Long> toPaymentCountsMap(List<PaymentMethodCount> rows) {
    Map<String, Long> result = new LinkedHashMap<>();
    for (PaymentMethodCount row : rows) {
      if (row.getMethod() != null) {
        result.put(row.getMethod(), row.getCount() == null ? 0L : row.getCount());
      }
    }
    return result;
  }

  private List<MonthlyBucket> toMonthlyBuckets(List<MonthlyAggregate> rows) {
    return rows.stream()
        .map(
            row ->
                MonthlyBucket.builder()
                    .month(row.getMonth())
                    .count(row.getCount() == null ? 0L : row.getCount())
                    .totalAmount(row.getTotalAmount() == null ? 0d : row.getTotalAmount())
                    .build())
        .toList();
  }

  private List<RecentActivityItem> toRecentActivity(List<PurchaseSale> contracts) {
    return contracts.stream()
        .map(
            c ->
                RecentActivityItem.builder()
                    .contractId(c.getId())
                    .contractType(c.getContractType() != null ? c.getContractType().name() : null)
                    .contractStatus(
                        c.getContractStatus() != null ? c.getContractStatus().name() : null)
                    .amount(c.getSalePrice())
                    .createdAt(c.getCreatedAt())
                    .build())
        .toList();
  }

  private GlobalMetrics toGlobalMetrics(GlobalAggregate row) {
    if (row == null) {
      return GlobalMetrics.builder()
          .totalContracts(0L)
          .totalRevenue(0d)
          .totalInvestment(0d)
          .build();
    }
    return GlobalMetrics.builder()
        .totalContracts(row.getTotalContracts() == null ? 0L : row.getTotalContracts())
        .totalRevenue(row.getTotalRevenue() == null ? 0d : row.getTotalRevenue())
        .totalInvestment(row.getTotalInvestment() == null ? 0d : row.getTotalInvestment())
        .build();
  }

  /**
   * Obtiene conteos de vehículos desde sgivu-vehicle. Si el microservicio no responde, devuelve
   * ceros en lugar de fallar — el dashboard prefiere mostrar datos parciales antes que un 500.
   */
  private VehicleCounts fetchVehicleCounts() {
    Map<String, Long> cars = safeCounts(vehicleServiceClient::getCarCounts);
    Map<String, Long> motorcycles = safeCounts(vehicleServiceClient::getMotorcycleCounts);
    return VehicleCounts.builder()
        .totalCars(cars.getOrDefault("totalCars", 0L))
        .availableCars(cars.getOrDefault("availableCars", 0L))
        .totalMotorcycles(motorcycles.getOrDefault("totalMotorcycles", 0L))
        .availableMotorcycles(motorcycles.getOrDefault("availableMotorcycles", 0L))
        .build();
  }

  private Map<String, Long> safeCounts(java.util.function.Supplier<Map<String, Long>> call) {
    try {
      Map<String, Long> result = call.get();
      return result == null ? Map.of() : result;
    } catch (HttpClientErrorException ex) {
      return Map.of();
    }
  }
}
