package com.sgivu.purchasesale.service;

import static org.junit.jupiter.api.Assertions.*;

import com.sgivu.purchasesale.dto.PurchaseSaleRequest;
import com.sgivu.purchasesale.dto.VehicleCreationRequest;
import com.sgivu.purchasesale.entity.PurchaseSale;
import com.sgivu.purchasesale.enums.ContractStatus;
import com.sgivu.purchasesale.enums.ContractType;
import com.sgivu.purchasesale.exception.ContractValidationException;
import com.sgivu.purchasesale.exception.DuplicateContractException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ContractBusinessRuleServiceTest {

  private ContractBusinessRuleService service;

  @BeforeEach
  void setUp() {
    service = new ContractBusinessRuleService();
  }

  @Nested
  @DisplayName("applyRules(...)")
  class ApplyRulesTests {

    @Test
    @DisplayName(
        "Compra establece salePrice desde vehicleData cuando está presente y sin conflictos")
    void shouldSetSalePriceFromVehicleDataForPurchase() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      VehicleCreationRequest data = new VehicleCreationRequest();
      data.setSalePrice(555d);
      request.setVehicleData(data);

      service.applyRules(ContractType.PURCHASE, request, Collections.emptyList(), null, 1L);

      assertEquals(555d, request.getSalePrice());
    }

    @Test
    @DisplayName("Compra lanza excepción cuando hay una compra activa o pendiente")
    void shouldThrowWhenActiveOrPendingPurchaseExists() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      VehicleCreationRequest data = new VehicleCreationRequest();
      data.setSalePrice(100d);
      request.setVehicleData(data);

      PurchaseSale conflicting = new PurchaseSale();
      conflicting.setId(10L);
      conflicting.setContractType(ContractType.PURCHASE);
      conflicting.setContractStatus(ContractStatus.PENDING);

      DuplicateContractException ex =
          assertThrows(
              DuplicateContractException.class,
              () ->
                  service.applyRules(
                      ContractType.PURCHASE, request, List.of(conflicting), null, 1L));
      assertTrue(ex.getMessage().contains("ya tiene una compra registrada"));
    }

    @Test
    @DisplayName("Compra ignora id de contrato excluido al verificar conflictos")
    void shouldIgnoreExcludedContractIdForPurchase() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      VehicleCreationRequest data = new VehicleCreationRequest();
      data.setSalePrice(100d);
      request.setVehicleData(data);

      PurchaseSale conflicting = new PurchaseSale();
      conflicting.setId(10L);
      conflicting.setContractType(ContractType.PURCHASE);
      conflicting.setContractStatus(ContractStatus.PENDING);

      assertDoesNotThrow(
          () -> service.applyRules(ContractType.PURCHASE, request, List.of(conflicting), 10L, 1L));
    }

    @Test
    @DisplayName("Venta lanza excepción cuando salePrice falta o es <= 0")
    void shouldThrowWhenInvalidSalePriceForSale() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setContractType(ContractType.SALE);
      request.setContractStatus(ContractStatus.PENDING);
      request.setSalePrice(0d);

      ContractValidationException ex =
          assertThrows(
              ContractValidationException.class,
              () ->
                  service.applyRules(
                      ContractType.SALE, request, Collections.emptyList(), null, 2L));
      assertEquals("El precio de venta debe ser mayor a cero.", ex.getMessage());
    }

    @Test
    @DisplayName("Venta lanza excepción si no hay stock de compra disponible")
    void shouldThrowWhenNoAvailableStockForSale() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setContractType(ContractType.SALE);
      request.setContractStatus(ContractStatus.PENDING);
      request.setSalePrice(2000d);

      PurchaseSale past = new PurchaseSale();
      past.setContractType(ContractType.PURCHASE);
      past.setContractStatus(ContractStatus.PENDING);

      ContractValidationException ex =
          assertThrows(
              ContractValidationException.class,
              () -> service.applyRules(ContractType.SALE, request, List.of(past), null, 3L));
      assertTrue(
          ex.getMessage().contains("No se encontró una compra válida asociada al vehículo."));
    }

    @Test
    @DisplayName("Venta lanza excepción cuando hay una venta en conflicto")
    void shouldThrowWhenConflictingSaleExists() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setContractType(ContractType.SALE);
      request.setContractStatus(ContractStatus.PENDING);
      request.setSalePrice(2500d);

      PurchaseSale purchase = new PurchaseSale();
      purchase.setContractType(ContractType.PURCHASE);
      purchase.setContractStatus(ContractStatus.ACTIVE);
      purchase.setPurchasePrice(1800d);

      PurchaseSale otherSale = new PurchaseSale();
      otherSale.setId(99L);
      otherSale.setContractType(ContractType.SALE);
      otherSale.setContractStatus(ContractStatus.PENDING);

      DuplicateContractException ex =
          assertThrows(
              DuplicateContractException.class,
              () ->
                  service.applyRules(
                      ContractType.SALE, request, List.of(purchase, otherSale), null, 4L));
      assertTrue(ex.getMessage().contains("ya cuenta con una venta registrada"));
    }

    @Test
    @DisplayName("Venta establece purchasePrice desde la última compra cuando está disponible")
    void shouldSetPurchasePriceFromLatestPurchaseForSale() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setContractType(ContractType.SALE);
      request.setContractStatus(ContractStatus.PENDING);
      request.setSalePrice(3000d);

      PurchaseSale purchase1 = new PurchaseSale();
      purchase1.setContractType(ContractType.PURCHASE);
      purchase1.setContractStatus(ContractStatus.COMPLETED);
      purchase1.setPurchasePrice(1500d);
      purchase1.setUpdatedAt(LocalDateTime.now().minusDays(2));

      PurchaseSale purchase2 = new PurchaseSale();
      purchase2.setContractType(ContractType.PURCHASE);
      purchase2.setContractStatus(ContractStatus.ACTIVE);
      purchase2.setPurchasePrice(2000d);
      purchase2.setUpdatedAt(LocalDateTime.now());

      service.applyRules(ContractType.SALE, request, List.of(purchase1, purchase2), null, 5L);

      assertEquals(2000d, request.getPurchasePrice());
    }
  }

  @Nested
  @DisplayName("preparePurchaseRequest(PurchaseSaleRequest)")
  class PreparePurchaseRequestTests {

    @Test
    @DisplayName("Usa vehicleData.salePrice cuando está presente")
    void shouldUseVehicleDataSalePrice() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      VehicleCreationRequest data = new VehicleCreationRequest();
      data.setSalePrice(1550d);
      request.setVehicleData(data);
      request.setSalePrice(1000d);

      service.preparePurchaseRequest(request);

      assertEquals(1550d, request.getSalePrice());
    }

    @Test
    @DisplayName("Retiene salePrice del request cuando vehicleData es nulo")
    void shouldRetainRequestSalePriceWhenVehicleDataNull() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setSalePrice(1200d);

      service.preparePurchaseRequest(request);

      assertEquals(1200d, request.getSalePrice());
    }

    @Test
    @DisplayName("Establece cero por defecto cuando vehicleData y salePrice están ausentes")
    void shouldDefaultToZeroWhenMissing() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();

      service.preparePurchaseRequest(request);

      assertEquals(0d, request.getSalePrice());
    }

    @Test
    @DisplayName("Usa salePrice del request cuando vehicleData.salePrice es nulo")
    void shouldUseRequestSalePriceWhenVehicleDataSalePriceNull() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      VehicleCreationRequest data = new VehicleCreationRequest();
      data.setSalePrice(null);
      request.setVehicleData(data);
      request.setSalePrice(900d);

      service.preparePurchaseRequest(request);

      assertEquals(900d, request.getSalePrice());
    }
  }

  @Nested
  @DisplayName("prepareSaleRequest(PurchaseSaleRequest, List<PurchaseSale>)")
  class PrepareSaleRequestTests {

    @Test
    @DisplayName("Debe lanzar excepción cuando salePrice es nulo")
    void shouldThrowWhenSalePriceIsNull() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setSalePrice(null);

      assertThrows(
          ContractValidationException.class,
          () -> service.prepareSaleRequest(request, Collections.emptyList()));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando salePrice es cero o negativo")
    void shouldThrowWhenSalePriceInvalid() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setSalePrice(0d);

      assertThrows(
          ContractValidationException.class,
          () -> service.prepareSaleRequest(request, Collections.emptyList()));
    }

    @Test
    @DisplayName("Debe establecer purchasePrice desde la última compra cuando está disponible")
    void shouldSetPurchasePriceFromLatestPurchase() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setSalePrice(2500d);

      PurchaseSale old = new PurchaseSale();
      old.setContractType(ContractType.PURCHASE);
      old.setContractStatus(ContractStatus.COMPLETED);
      old.setPurchasePrice(1400d);
      old.setUpdatedAt(LocalDateTime.now().minusDays(3));

      PurchaseSale recent = new PurchaseSale();
      recent.setContractType(ContractType.PURCHASE);
      recent.setContractStatus(ContractStatus.ACTIVE);
      recent.setPurchasePrice(1800d);
      recent.setUpdatedAt(LocalDateTime.now());

      service.prepareSaleRequest(request, List.of(old, recent));

      assertEquals(1800d, request.getPurchasePrice());
    }

    @Test
    @DisplayName("Debe usar purchasePrice alternativo cuando no hay compras disponibles")
    void shouldUseFallbackPurchasePriceWhenNoPurchases() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setSalePrice(2000d);
      request.setPurchasePrice(1700d);

      service.prepareSaleRequest(request, Collections.emptyList());

      assertEquals(1700d, request.getPurchasePrice());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando no hay compras y el alternativo falta")
    void shouldThrowWhenNoPurchasesAndNoFallback() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setSalePrice(2000d);
      request.setPurchasePrice(null);

      assertThrows(
          ContractValidationException.class,
          () -> service.prepareSaleRequest(request, Collections.emptyList()));
    }
  }

  @Nested
  @DisplayName("findLatestPurchasePrice(List<PurchaseSale>, Double)")
  class FindLatestPurchasePriceTests {

    @Test
    @DisplayName("Debe retornar último precio de compra entre compras ACTIVE/COMPLETED")
    void shouldReturnLatestPurchasePriceFromActiveOrCompleted() {
      PurchaseSale old = new PurchaseSale();
      old.setContractType(ContractType.PURCHASE);
      old.setContractStatus(ContractStatus.COMPLETED);
      old.setPurchasePrice(1500d);
      old.setUpdatedAt(LocalDateTime.now().minusDays(5));

      PurchaseSale recent = new PurchaseSale();
      recent.setContractType(ContractType.PURCHASE);
      recent.setContractStatus(ContractStatus.ACTIVE);
      recent.setPurchasePrice(2000d);
      recent.setUpdatedAt(LocalDateTime.now());

      // PENDING con updatedAt posterior debe ser ignorada
      PurchaseSale pending = new PurchaseSale();
      pending.setContractType(ContractType.PURCHASE);
      pending.setContractStatus(ContractStatus.PENDING);
      pending.setPurchasePrice(3000d);
      pending.setUpdatedAt(LocalDateTime.now().plusDays(1));

      Double result = service.findLatestPurchasePrice(List.of(old, recent, pending), null);

      assertEquals(2000d, result);
    }

    @Test
    @DisplayName("Debe retornar alternativo cuando no hay compras válidas y alternativo > 0")
    void shouldReturnFallbackWhenNoValidPurchases() {
      Double result = service.findLatestPurchasePrice(Collections.emptyList(), 1800d);

      assertEquals(1800d, result);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando no hay compras válidas y el alternativo es inválido")
    void shouldThrowWhenNoValidPurchasesAndNoFallback() {
      assertThrows(
          ContractValidationException.class,
          () -> service.findLatestPurchasePrice(Collections.emptyList(), null));
      assertThrows(
          ContractValidationException.class,
          () -> service.findLatestPurchasePrice(Collections.emptyList(), 0d));
    }
  }

  @Nested
  @DisplayName("ensureNoActivePurchase(List<PurchaseSale>, Long, Long)")
  class EnsureNoActivePurchaseTests {

    @Test
    @DisplayName("Debe lanzar excepción cuando hay una compra PENDING")
    void shouldThrowWhenPendingPurchaseExists() {
      PurchaseSale pending = new PurchaseSale();
      pending.setId(10L);
      pending.setContractType(ContractType.PURCHASE);
      pending.setContractStatus(ContractStatus.PENDING);

      DuplicateContractException ex =
          assertThrows(
              DuplicateContractException.class,
              () -> service.ensureNoActivePurchase(List.of(pending), null, 5L));
      assertTrue(ex.getMessage().contains("ya tiene una compra registrada"));
      assertTrue(ex.getMessage().contains("5"));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando hay una compra ACTIVE")
    void shouldThrowWhenActivePurchaseExists() {
      PurchaseSale active = new PurchaseSale();
      active.setId(11L);
      active.setContractType(ContractType.PURCHASE);
      active.setContractStatus(ContractStatus.ACTIVE);

      DuplicateContractException ex =
          assertThrows(
              DuplicateContractException.class,
              () -> service.ensureNoActivePurchase(List.of(active), null, 6L));
      assertTrue(ex.getMessage().contains("ya tiene una compra registrada"));
    }

    @Test
    @DisplayName("Debe ignorar compra que coincide con excludedContractId")
    void shouldIgnoreExcludedContractId() {
      PurchaseSale pending = new PurchaseSale();
      pending.setId(20L);
      pending.setContractType(ContractType.PURCHASE);
      pending.setContractStatus(ContractStatus.PENDING);

      assertDoesNotThrow(() -> service.ensureNoActivePurchase(List.of(pending), 20L, 7L));
    }

    @Test
    @DisplayName("No debe lanzar excepción cuando solo existen compras COMPLETED")
    void shouldNotThrowForCompletedPurchases() {
      PurchaseSale completed = new PurchaseSale();
      completed.setId(30L);
      completed.setContractType(ContractType.PURCHASE);
      completed.setContractStatus(ContractStatus.COMPLETED);

      assertDoesNotThrow(() -> service.ensureNoActivePurchase(List.of(completed), null, 8L));
    }
  }

  @Nested
  @DisplayName("ensureSalePrerequisites(List<PurchaseSale>, Long, Long, ContractStatus)")
  class EnsureSalePrerequisitesTests {

    @Test
    @DisplayName("Debe lanzar excepción cuando no existe compra ACTIVE/COMPLETED disponible")
    void shouldThrowWhenNoAvailablePurchase() {
      PurchaseSale onlyPending = new PurchaseSale();
      onlyPending.setContractType(ContractType.PURCHASE);
      onlyPending.setContractStatus(ContractStatus.PENDING);

      ContractValidationException ex =
          assertThrows(
              ContractValidationException.class,
              () ->
                  service.ensureSalePrerequisites(
                      List.of(onlyPending), null, 42L, ContractStatus.PENDING));
      assertTrue(ex.getMessage().contains("no cuenta con una compra activa o completada"));
      assertTrue(ex.getMessage().contains("42"));
    }

    @Test
    @DisplayName("No debe lanzar excepción cuando hay una compra ACTIVE presente")
    void shouldNotThrowWhenActivePurchaseExists() {
      PurchaseSale active = new PurchaseSale();
      active.setContractType(ContractType.PURCHASE);
      active.setContractStatus(ContractStatus.ACTIVE);

      assertDoesNotThrow(
          () ->
              service.ensureSalePrerequisites(List.of(active), null, 43L, ContractStatus.PENDING));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando existe una venta en conflicto")
    void shouldThrowWhenConflictingSaleExists() {
      PurchaseSale purchase = new PurchaseSale();
      purchase.setContractType(ContractType.PURCHASE);
      purchase.setContractStatus(ContractStatus.ACTIVE);

      PurchaseSale otherSale = new PurchaseSale();
      otherSale.setId(99L);
      otherSale.setContractType(ContractType.SALE);
      otherSale.setContractStatus(ContractStatus.PENDING);

      DuplicateContractException ex =
          assertThrows(
              DuplicateContractException.class,
              () ->
                  service.ensureSalePrerequisites(
                      List.of(purchase, otherSale), null, 50L, ContractStatus.PENDING));
      assertTrue(ex.getMessage().contains("ya cuenta con una venta registrada"));
    }

    @Test
    @DisplayName("Debe ignorar venta en conflicto que coincide con excludedContractId")
    void shouldIgnoreExcludedConflictingSale() {
      PurchaseSale purchase = new PurchaseSale();
      purchase.setContractType(ContractType.PURCHASE);
      purchase.setContractStatus(ContractStatus.ACTIVE);

      PurchaseSale otherSale = new PurchaseSale();
      otherSale.setId(123L);
      otherSale.setContractType(ContractType.SALE);
      otherSale.setContractStatus(ContractStatus.PENDING);

      assertDoesNotThrow(
          () ->
              service.ensureSalePrerequisites(
                  List.of(purchase, otherSale), 123L, 51L, ContractStatus.PENDING));
    }

    @Test
    @DisplayName(
        "Debe validar ventas en conflicto incluso cuando la validación de disponibilidad se omite")
    void shouldValidateSalesWhenAvailabilitySkipped() {
      PurchaseSale onlySale = new PurchaseSale();
      onlySale.setId(77L);
      onlySale.setContractType(ContractType.SALE);
      onlySale.setContractStatus(ContractStatus.ACTIVE);

      DuplicateContractException ex =
          assertThrows(
              DuplicateContractException.class,
              () -> service.ensureSalePrerequisites(List.of(onlySale), null, 52L, null));
      assertTrue(ex.getMessage().contains("ya cuenta con una venta registrada"));
    }
  }
}
