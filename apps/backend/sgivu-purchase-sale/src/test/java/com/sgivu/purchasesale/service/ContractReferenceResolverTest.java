package com.sgivu.purchasesale.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sgivu.purchasesale.dto.PurchaseSaleRequest;
import com.sgivu.purchasesale.dto.VehicleCreationRequest;
import com.sgivu.purchasesale.enums.ContractType;
import com.sgivu.purchasesale.exception.InvalidContractOperationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContractReferenceResolverTest {

  @Mock private EntityResolutionService entityResolutionService;
  @Mock private VehicleRegistrationService vehicleRegistrationService;
  @InjectMocks private ContractReferenceResolver resolver;

  @Nested
  @DisplayName("resolveVehicleReference(PURCHASE, ...)")
  class PurchaseResolutionTests {

    @Test
    @DisplayName("Debe verificar vehículo existente cuando vehicleId está presente")
    void shouldResolveExistingVehicleWhenIdProvided() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setVehicleId(10L);

      when(entityResolutionService.resolveVehicleId(10L)).thenReturn(10L);

      Long result = resolver.resolveVehicleReference(ContractType.PURCHASE, request);

      assertEquals(10L, result);
      verify(entityResolutionService).resolveVehicleId(10L);
      verifyNoInteractions(vehicleRegistrationService);
    }

    @Test
    @DisplayName("Debe registrar vehículo nuevo cuando vehicleId es nulo")
    void shouldRegisterNewVehicleWhenIdNull() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setVehicleId(null);
      request.setVehicleData(new VehicleCreationRequest());

      when(vehicleRegistrationService.registerVehicle(request)).thenReturn(77L);

      Long result = resolver.resolveVehicleReference(ContractType.PURCHASE, request);

      assertEquals(77L, result);
      assertEquals(77L, request.getVehicleId());
      verify(vehicleRegistrationService).registerVehicle(request);
      verifyNoInteractions(entityResolutionService);
    }
  }

  @Nested
  @DisplayName("resolveVehicleReference(SALE, ...)")
  class SaleResolutionTests {

    @Test
    @DisplayName(
        "Debe resolver vehículo existente cuando vehicleId está presente y sin vehicleData")
    void shouldResolveExistingVehicleForSale() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setVehicleId(20L);
      request.setVehicleData(null);

      when(entityResolutionService.resolveVehicleId(20L)).thenReturn(20L);

      Long result = resolver.resolveVehicleReference(ContractType.SALE, request);

      assertEquals(20L, result);
      verify(entityResolutionService).resolveVehicleId(20L);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando vehicleId es nulo en venta")
    void shouldThrowWhenVehicleIdNullForSale() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setVehicleId(null);

      InvalidContractOperationException ex =
          assertThrows(
              InvalidContractOperationException.class,
              () -> resolver.resolveVehicleReference(ContractType.SALE, request));
      assertTrue(ex.getMessage().contains("seleccionar el vehículo"));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando vehicleData está presente en venta")
    void shouldThrowWhenVehicleDataProvidedForSale() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setVehicleId(30L);
      request.setVehicleData(new VehicleCreationRequest());

      InvalidContractOperationException ex =
          assertThrows(
              InvalidContractOperationException.class,
              () -> resolver.resolveVehicleReference(ContractType.SALE, request));
      assertTrue(ex.getMessage().contains("solo deben enviarse para contratos de compra"));
    }
  }
}
