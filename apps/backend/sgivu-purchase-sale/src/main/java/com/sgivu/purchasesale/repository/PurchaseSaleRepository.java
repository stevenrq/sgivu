package com.sgivu.purchasesale.repository;

import com.sgivu.purchasesale.entity.PurchaseSale;
import com.sgivu.purchasesale.repository.projection.ContractStatusCount;
import com.sgivu.purchasesale.repository.projection.GlobalAggregate;
import com.sgivu.purchasesale.repository.projection.MonthlyAggregate;
import com.sgivu.purchasesale.repository.projection.PaymentMethodCount;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface PurchaseSaleRepository
    extends JpaRepository<PurchaseSale, Long>, JpaSpecificationExecutor<PurchaseSale> {

  List<PurchaseSale> findByClientId(Long clientId);

  List<PurchaseSale> findByUserId(Long userId);

  List<PurchaseSale> findByVehicleId(Long vehicleId);

  // ----------------------------------------------------------------------
  // Queries agregadas para /dashboard-summary — GROUP BY en BD, NO findAll() en memoria.
  // ----------------------------------------------------------------------

  @Query(
      "SELECT ps.contractStatus AS status, COUNT(ps) AS count "
          + "FROM PurchaseSale ps GROUP BY ps.contractStatus")
  List<ContractStatusCount> countByContractStatus();

  @Query(
      "SELECT ps.paymentMethod AS method, COUNT(ps) AS count "
          + "FROM PurchaseSale ps GROUP BY ps.paymentMethod")
  List<PaymentMethodCount> countByPaymentMethod();

  /**
   * Agrega contratos por mes para un ContractType dado (SALE o PURCHASE), considerando sólo los
   * COMPLETED. Usa DATE_TRUNC (PostgreSQL) para truncar al primer día del mes.
   */
  @Query(
      value =
          "SELECT TO_CHAR(DATE_TRUNC('month', created_at), 'YYYY-MM') AS month, "
              + "       COUNT(*) AS count, "
              + "       COALESCE(SUM(sale_price), 0) AS totalAmount "
              + "FROM purchase_sales "
              + "WHERE contract_type = :contractType "
              + "  AND contract_status = 'COMPLETED' "
              + "GROUP BY DATE_TRUNC('month', created_at) "
              + "ORDER BY DATE_TRUNC('month', created_at) DESC "
              + "LIMIT 12",
      nativeQuery = true)
  List<MonthlyAggregate> aggregateMonthlyByType(String contractType);

  @Query(
      "SELECT COUNT(ps) AS totalContracts, "
          + "       COALESCE(SUM(ps.salePrice), 0) AS totalRevenue, "
          + "       COALESCE(SUM(ps.purchasePrice), 0) AS totalInvestment "
          + "FROM PurchaseSale ps")
  GlobalAggregate aggregateGlobals();

  /**
   * Últimos N contratos (actividad reciente del dashboard). Paginable para elegir N vía Pageable.
   */
  @Query("SELECT ps FROM PurchaseSale ps ORDER BY ps.createdAt DESC")
  List<PurchaseSale> findRecentActivity(Pageable pageable);

  /**
   * Busca los IDs de vehículos que se hayan comprado, pero aún no se hayan vendido. Esta consulta
   * verifica los vehículos que tengan al menos un contrato de compra con estado "ACTIVE" o
   * "COMPLETED" y ningún contrato de venta con estado "PENDING", "ACTIVE" o "COMPLETED".
   *
   * @return una lista de IDs de vehículos disponibles para venta.
   */
  @Query(
      value =
          "SELECT DISTINCT ps.vehicle_id "
              + "FROM purchase_sales ps "
              + "WHERE EXISTS ( "
              + "  SELECT 1 FROM purchase_sales p "
              + "  WHERE p.vehicle_id = ps.vehicle_id "
              + "    AND p.contract_type = 'PURCHASE' "
              + "    AND p.contract_status IN ('ACTIVE','COMPLETED') "
              + ") "
              + "AND NOT EXISTS ( "
              + "  SELECT 1 FROM purchase_sales s "
              + "  WHERE s.vehicle_id = ps.vehicle_id "
              + "    AND s.contract_type = 'SALE' "
              + "    AND s.contract_status IN ('PENDING','ACTIVE','COMPLETED')"
              + ")",
      nativeQuery = true)
  List<Long> findAvailableVehicleIds();
}
