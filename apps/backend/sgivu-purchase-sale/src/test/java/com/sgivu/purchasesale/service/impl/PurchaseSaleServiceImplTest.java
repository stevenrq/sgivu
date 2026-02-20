package com.sgivu.purchasesale.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgivu.purchasesale.dto.PurchaseSaleFilterCriteria;
import com.sgivu.purchasesale.dto.PurchaseSaleRequest;
import com.sgivu.purchasesale.dto.VehicleCreationRequest;
import com.sgivu.purchasesale.entity.PurchaseSale;
import com.sgivu.purchasesale.enums.ContractStatus;
import com.sgivu.purchasesale.enums.ContractType;
import com.sgivu.purchasesale.enums.PaymentMethod;
import com.sgivu.purchasesale.exception.ContractValidationException;
import com.sgivu.purchasesale.exception.InvalidContractOperationException;
import com.sgivu.purchasesale.mapper.PurchaseSaleMapper;
import com.sgivu.purchasesale.repository.PurchaseSaleRepository;
import com.sgivu.purchasesale.service.ContractBusinessRuleService;
import com.sgivu.purchasesale.service.ContractReferenceResolver;
import com.sgivu.purchasesale.service.EntityResolutionService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
public class PurchaseSaleServiceImplTest {

  @Mock private PurchaseSaleRepository purchaseSaleRepository;
  @Mock private PurchaseSaleMapper purchaseSaleMapper;
  @Mock private EntityResolutionService entityResolutionService;
  @Mock private ContractReferenceResolver contractReferenceResolver;
  @Mock private ContractBusinessRuleService contractBusinessRuleService;

  @InjectMocks private PurchaseSaleServiceImpl service;

  // === Helpers ===

  private PurchaseSaleRequest validPurchaseRequest() {
    PurchaseSaleRequest request = new PurchaseSaleRequest();
    request.setClientId(1L);
    request.setUserId(2L);
    request.setPurchasePrice(1000d);
    request.setSalePrice(1200d);
    request.setContractType(ContractType.PURCHASE);
    request.setContractStatus(ContractStatus.PENDING);
    request.setPaymentLimitations("pl");
    request.setPaymentTerms("pt");
    request.setPaymentMethod(PaymentMethod.CASH);
    VehicleCreationRequest data = new VehicleCreationRequest();
    data.setSalePrice(1200d);
    request.setVehicleData(data);
    return request;
  }

  private PurchaseSaleRequest validSaleRequest() {
    PurchaseSaleRequest request = new PurchaseSaleRequest();
    request.setClientId(1L);
    request.setUserId(2L);
    request.setVehicleId(5L);
    request.setPurchasePrice(800d);
    request.setSalePrice(2500d);
    request.setContractType(ContractType.SALE);
    request.setContractStatus(ContractStatus.PENDING);
    request.setPaymentLimitations("pl");
    request.setPaymentTerms("pt");
    request.setPaymentMethod(PaymentMethod.CASH);
    return request;
  }

  private PurchaseSale mappedPurchaseEntity(Double purchasePrice) {
    PurchaseSale entity = new PurchaseSale();
    entity.setPurchasePrice(purchasePrice);
    entity.setSalePrice(0d);
    entity.setContractType(ContractType.PURCHASE);
    entity.setContractStatus(ContractStatus.PENDING);
    entity.setPaymentLimitations("pl");
    entity.setPaymentTerms("pt");
    entity.setPaymentMethod(PaymentMethod.CASH);
    return entity;
  }

  // === Tests ===

  @Nested
  @DisplayName("create(PurchaseSaleRequest)")
  class CreateTests {

    @Test
    @DisplayName("Debe resolver entidades, aplicar reglas y guardar contrato de compra")
    void shouldResolveEntitiesApplyRulesAndSavePurchase() {
      PurchaseSaleRequest request = validPurchaseRequest();

      when(entityResolutionService.resolveClientId(1L)).thenReturn(1L);
      when(entityResolutionService.resolveUserId(2L)).thenReturn(2L);
      when(contractReferenceResolver.resolveVehicleReference(ContractType.PURCHASE, request))
          .thenReturn(10L);
      when(purchaseSaleRepository.findByVehicleId(10L)).thenReturn(Collections.emptyList());

      PurchaseSale mapped = mappedPurchaseEntity(1000d);
      when(purchaseSaleMapper.toPurchaseSale(request)).thenReturn(mapped);
      when(purchaseSaleRepository.save(mapped)).thenReturn(mapped);

      PurchaseSale result = service.create(request);

      assertNotNull(result);
      assertEquals(1L, result.getClientId());
      assertEquals(2L, result.getUserId());
      assertEquals(10L, result.getVehicleId());
      verify(contractBusinessRuleService)
          .applyRules(ContractType.PURCHASE, request, Collections.emptyList(), null, 10L);
      verify(purchaseSaleRepository).save(mapped);
    }

    @Test
    @DisplayName("Debe resolver entidades, aplicar reglas y guardar contrato de venta")
    void shouldResolveEntitiesApplyRulesAndSaveSale() {
      PurchaseSaleRequest request = validSaleRequest();

      when(entityResolutionService.resolveClientId(1L)).thenReturn(1L);
      when(entityResolutionService.resolveUserId(2L)).thenReturn(2L);
      when(contractReferenceResolver.resolveVehicleReference(ContractType.SALE, request))
          .thenReturn(5L);

      PurchaseSale activePurchase = new PurchaseSale();
      activePurchase.setContractType(ContractType.PURCHASE);
      activePurchase.setContractStatus(ContractStatus.ACTIVE);
      activePurchase.setPurchasePrice(800d);
      when(purchaseSaleRepository.findByVehicleId(5L)).thenReturn(List.of(activePurchase));

      PurchaseSale mapped = new PurchaseSale();
      mapped.setPurchasePrice(800d);
      mapped.setSalePrice(2500d);
      when(purchaseSaleMapper.toPurchaseSale(request)).thenReturn(mapped);
      when(purchaseSaleRepository.save(mapped)).thenReturn(mapped);

      PurchaseSale result = service.create(request);

      assertNotNull(result);
      assertEquals(1L, result.getClientId());
      assertEquals(2L, result.getUserId());
      assertEquals(5L, result.getVehicleId());
      verify(contractBusinessRuleService)
          .applyRules(eq(ContractType.SALE), eq(request), anyList(), isNull(), eq(5L));
      verify(purchaseSaleRepository).save(mapped);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando purchasePrice es inválido tras mapeo")
    void shouldThrowWhenPurchasePriceInvalid() {
      PurchaseSaleRequest request = validPurchaseRequest();

      when(entityResolutionService.resolveClientId(1L)).thenReturn(1L);
      when(entityResolutionService.resolveUserId(2L)).thenReturn(2L);
      when(contractReferenceResolver.resolveVehicleReference(ContractType.PURCHASE, request))
          .thenReturn(10L);
      when(purchaseSaleRepository.findByVehicleId(10L)).thenReturn(Collections.emptyList());

      PurchaseSale mapped = mappedPurchaseEntity(0d);
      when(purchaseSaleMapper.toPurchaseSale(request)).thenReturn(mapped);

      ContractValidationException ex =
          assertThrows(ContractValidationException.class, () -> service.create(request));
      assertEquals("El precio de compra debe ser mayor a cero.", ex.getMessage());
      verify(purchaseSaleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe normalizar contractType a PURCHASE cuando es nulo")
    void shouldDefaultContractTypeToPurchase() {
      PurchaseSaleRequest request = validPurchaseRequest();
      request.setContractType(null);
      request.setContractStatus(null);

      when(entityResolutionService.resolveClientId(1L)).thenReturn(1L);
      when(entityResolutionService.resolveUserId(2L)).thenReturn(2L);
      when(contractReferenceResolver.resolveVehicleReference(ContractType.PURCHASE, request))
          .thenReturn(10L);
      when(purchaseSaleRepository.findByVehicleId(10L)).thenReturn(Collections.emptyList());

      PurchaseSale mapped = mappedPurchaseEntity(1000d);
      when(purchaseSaleMapper.toPurchaseSale(request)).thenReturn(mapped);
      when(purchaseSaleRepository.save(mapped)).thenReturn(mapped);

      service.create(request);

      assertEquals(ContractType.PURCHASE, request.getContractType());
      assertEquals(ContractStatus.PENDING, request.getContractStatus());
    }

    @Test
    @DisplayName("Debe establecer salePrice a 0 en compra mediante applyContractAdjustments")
    void shouldSetSalePriceToZeroForPurchaseWhenNull() {
      PurchaseSaleRequest request = validPurchaseRequest();
      request.setSalePrice(null);

      when(entityResolutionService.resolveClientId(1L)).thenReturn(1L);
      when(entityResolutionService.resolveUserId(2L)).thenReturn(2L);
      when(contractReferenceResolver.resolveVehicleReference(ContractType.PURCHASE, request))
          .thenReturn(10L);
      when(purchaseSaleRepository.findByVehicleId(10L)).thenReturn(Collections.emptyList());

      PurchaseSale mapped = mappedPurchaseEntity(1000d);
      when(purchaseSaleMapper.toPurchaseSale(request)).thenReturn(mapped);
      when(purchaseSaleRepository.save(mapped)).thenReturn(mapped);

      PurchaseSale result = service.create(request);

      assertEquals(0d, result.getSalePrice());
    }
  }

  @Nested
  @DisplayName("update(Long, PurchaseSaleRequest)")
  class UpdateTests {

    @Test
    @DisplayName("Debe actualizar compra existente y guardar")
    void shouldUpdateExistingPurchaseAndSave() {
      Long id = 1L;
      PurchaseSaleRequest request = validPurchaseRequest();
      request.setVehicleId(5L);

      PurchaseSale existing = new PurchaseSale();
      existing.setId(id);
      existing.setContractType(ContractType.PURCHASE);
      existing.setPurchasePrice(1000d);

      when(entityResolutionService.resolveClientId(1L)).thenReturn(1L);
      when(entityResolutionService.resolveUserId(2L)).thenReturn(2L);
      when(entityResolutionService.resolveVehicleId(5L)).thenReturn(5L);
      when(purchaseSaleRepository.findByVehicleId(5L)).thenReturn(Collections.emptyList());
      when(purchaseSaleRepository.findById(id)).thenReturn(Optional.of(existing));

      doAnswer(
              invocation -> {
                PurchaseSaleRequest r = invocation.getArgument(0);
                PurchaseSale p = invocation.getArgument(1);
                p.setPurchasePrice(r.getPurchasePrice());
                p.setSalePrice(r.getSalePrice());
                return null;
              })
          .when(purchaseSaleMapper)
          .updatePurchaseSaleFromRequest(any(PurchaseSaleRequest.class), any(PurchaseSale.class));

      when(purchaseSaleRepository.save(existing)).thenReturn(existing);

      Optional<PurchaseSale> result = service.update(id, request);

      assertTrue(result.isPresent());
      assertEquals(1L, result.get().getClientId());
      assertEquals(2L, result.get().getUserId());
      assertEquals(5L, result.get().getVehicleId());
      verify(contractBusinessRuleService)
          .applyRules(
              eq(ContractType.PURCHASE), eq(request), eq(Collections.emptyList()), eq(id), eq(5L));
      verify(purchaseSaleRepository).save(existing);
    }

    @Test
    @DisplayName("Debe retornar Optional vacío si el contrato no se encuentra")
    void shouldReturnEmptyIfContractNotFound() {
      PurchaseSaleRequest request = validPurchaseRequest();
      request.setVehicleId(5L);

      when(entityResolutionService.resolveClientId(1L)).thenReturn(1L);
      when(entityResolutionService.resolveUserId(2L)).thenReturn(2L);
      when(entityResolutionService.resolveVehicleId(5L)).thenReturn(5L);
      when(purchaseSaleRepository.findByVehicleId(5L)).thenReturn(Collections.emptyList());
      when(purchaseSaleRepository.findById(99L)).thenReturn(Optional.empty());

      Optional<PurchaseSale> result = service.update(99L, request);

      assertFalse(result.isPresent());
      verify(purchaseSaleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción al intentar cambiar el tipo de contrato")
    void shouldThrowWhenChangingContractType() {
      PurchaseSaleRequest request = validSaleRequest();

      PurchaseSale existing = new PurchaseSale();
      existing.setId(3L);
      existing.setContractType(ContractType.PURCHASE);

      when(entityResolutionService.resolveClientId(1L)).thenReturn(1L);
      when(entityResolutionService.resolveUserId(2L)).thenReturn(2L);
      when(entityResolutionService.resolveVehicleId(5L)).thenReturn(5L);
      when(purchaseSaleRepository.findByVehicleId(5L)).thenReturn(Collections.emptyList());
      when(purchaseSaleRepository.findById(3L)).thenReturn(Optional.of(existing));

      InvalidContractOperationException ex =
          assertThrows(InvalidContractOperationException.class, () -> service.update(3L, request));
      assertEquals("No es posible cambiar el tipo de contrato una vez creado.", ex.getMessage());
    }

    @Test
    @DisplayName("Debe propagar excepción cuando falla el guardado")
    void shouldPropagateExceptionWhenSaveFails() {
      PurchaseSaleRequest request = validPurchaseRequest();
      request.setVehicleId(5L);

      PurchaseSale existing = new PurchaseSale();
      existing.setId(4L);
      existing.setContractType(ContractType.PURCHASE);
      existing.setPurchasePrice(100d);

      when(entityResolutionService.resolveClientId(1L)).thenReturn(1L);
      when(entityResolutionService.resolveUserId(2L)).thenReturn(2L);
      when(entityResolutionService.resolveVehicleId(5L)).thenReturn(5L);
      when(purchaseSaleRepository.findById(4L)).thenReturn(Optional.of(existing));
      when(purchaseSaleRepository.findByVehicleId(5L)).thenReturn(Collections.emptyList());
      doNothing().when(purchaseSaleMapper).updatePurchaseSaleFromRequest(any(), any());
      when(purchaseSaleRepository.save(existing)).thenThrow(new RuntimeException("DB error"));

      assertThrows(RuntimeException.class, () -> service.update(4L, request));
      verify(purchaseSaleRepository).save(existing);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando id es nulo")
    void shouldThrowWhenIdIsNull() {
      PurchaseSaleRequest request = validPurchaseRequest();

      ContractValidationException ex =
          assertThrows(ContractValidationException.class, () -> service.update(null, request));
      assertEquals("El ID del contrato debe ser proporcionado.", ex.getMessage());
    }
  }

  @Nested
  @DisplayName("deleteById(Long)")
  class DeleteByIdTests {

    @Test
    @DisplayName("Debe eliminar contrato cuando el estado es CANCELED")
    void shouldDeleteWhenStatusIsCanceled() {
      PurchaseSale ps = new PurchaseSale();
      ps.setId(1L);
      ps.setContractStatus(ContractStatus.CANCELED);

      when(purchaseSaleRepository.findById(1L)).thenReturn(Optional.of(ps));

      assertDoesNotThrow(() -> service.deleteById(1L));

      verify(purchaseSaleRepository).findById(1L);
      verify(purchaseSaleRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el contrato no existe")
    void shouldThrowWhenContractNotFound() {
      when(purchaseSaleRepository.findById(2L)).thenReturn(Optional.empty());

      ContractValidationException ex =
          assertThrows(ContractValidationException.class, () -> service.deleteById(2L));
      assertEquals("Contrato no encontrado con id: 2", ex.getMessage());
      verify(purchaseSaleRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el estado no es CANCELED")
    void shouldThrowWhenStatusIsNotCanceled() {
      PurchaseSale ps = new PurchaseSale();
      ps.setId(3L);
      ps.setContractStatus(ContractStatus.PENDING);

      when(purchaseSaleRepository.findById(3L)).thenReturn(Optional.of(ps));

      InvalidContractOperationException ex =
          assertThrows(InvalidContractOperationException.class, () -> service.deleteById(3L));
      assertEquals(
          "Solo se pueden eliminar contratos que estén en estado 'CANCELED'.", ex.getMessage());
      verify(purchaseSaleRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando id es nulo después de pasar validación de estado")
    void shouldThrowWhenIdNullAfterStatusCheck() {
      PurchaseSale ps = new PurchaseSale();
      ps.setId(null);
      ps.setContractStatus(ContractStatus.CANCELED);

      doReturn(Optional.of(ps)).when(purchaseSaleRepository).findById(null);

      ContractValidationException ex =
          assertThrows(ContractValidationException.class, () -> service.deleteById(null));
      assertEquals("El ID del contrato debe ser proporcionado.", ex.getMessage());
      verify(purchaseSaleRepository, never()).deleteById(any());
    }
  }

  @Nested
  @DisplayName("findById(Long)")
  class FindByIdTests {

    @Test
    @DisplayName("Debe retornar contrato cuando existe")
    void shouldReturnContractWhenExists() {
      PurchaseSale ps = new PurchaseSale();
      ps.setId(1L);
      when(purchaseSaleRepository.findById(1L)).thenReturn(Optional.of(ps));

      Optional<PurchaseSale> result = service.findById(1L);

      assertTrue(result.isPresent());
      assertEquals(1L, result.get().getId());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando id es nulo")
    void shouldThrowWhenIdIsNull() {
      ContractValidationException ex =
          assertThrows(ContractValidationException.class, () -> service.findById(null));
      assertEquals("El ID del contrato debe ser proporcionado.", ex.getMessage());
    }
  }

  @Nested
  @DisplayName("findAll()")
  class FindAllTests {

    @Test
    @DisplayName("Debe delegar al repositorio y retornar lista")
    void shouldDelegateToRepository() {
      PurchaseSale ps = new PurchaseSale();
      when(purchaseSaleRepository.findAll()).thenReturn(List.of(ps));

      List<PurchaseSale> result = service.findAll();

      assertEquals(1, result.size());
      verify(purchaseSaleRepository).findAll();
    }
  }

  @Nested
  @DisplayName("findAll(Pageable)")
  class FindAllPageableTests {

    @Test
    @DisplayName("Debe retornar página del repositorio")
    void shouldReturnPageFromRepository() {
      Pageable pageable = PageRequest.of(0, 10);
      Page<PurchaseSale> page = new PageImpl<>(List.of(new PurchaseSale()));
      when(purchaseSaleRepository.findAll(pageable)).thenReturn(page);

      Page<PurchaseSale> result = service.findAll(pageable);

      assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Debe lanzar NullPointerException cuando pageable es nulo")
    void shouldThrowWhenPageableNull() {
      assertThrows(NullPointerException.class, () -> service.findAll((Pageable) null));
    }
  }

  @Nested
  @DisplayName("search(PurchaseSaleFilterCriteria, Pageable)")
  class SearchTests {

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Debe delegar búsqueda al repositorio con specification y pageable")
    void shouldDelegateSearchToRepository() {
      PurchaseSaleFilterCriteria criteria = PurchaseSaleFilterCriteria.builder().build();
      Pageable pageable = PageRequest.of(0, 10);
      Page<PurchaseSale> page = new PageImpl<>(List.of(new PurchaseSale()));
      when(purchaseSaleRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

      Page<PurchaseSale> result = service.search(criteria, pageable);

      assertEquals(1, result.getTotalElements());
    }
  }

  @Nested
  @DisplayName("findByClientId(Long)")
  class FindByClientIdTests {

    @Test
    @DisplayName("Debe resolver clientId y retornar contratos")
    void shouldResolveClientIdAndReturnContracts() {
      when(entityResolutionService.resolveClientId(10L)).thenReturn(10L);
      when(purchaseSaleRepository.findByClientId(10L)).thenReturn(List.of(new PurchaseSale()));

      List<PurchaseSale> result = service.findByClientId(10L);

      assertEquals(1, result.size());
      verify(entityResolutionService).resolveClientId(10L);
    }
  }

  @Nested
  @DisplayName("findByUserId(Long)")
  class FindByUserIdTests {

    @Test
    @DisplayName("Debe resolver userId y retornar contratos")
    void shouldResolveUserIdAndReturnContracts() {
      when(entityResolutionService.resolveUserId(20L)).thenReturn(20L);
      when(purchaseSaleRepository.findByUserId(20L)).thenReturn(List.of(new PurchaseSale()));

      List<PurchaseSale> result = service.findByUserId(20L);

      assertEquals(1, result.size());
      verify(entityResolutionService).resolveUserId(20L);
    }
  }

  @Nested
  @DisplayName("findByVehicleId(Long)")
  class FindByVehicleIdTests {

    @Test
    @DisplayName("Debe resolver vehicleId y retornar contratos")
    void shouldResolveVehicleIdAndReturnContracts() {
      when(entityResolutionService.resolveVehicleId(30L)).thenReturn(30L);
      when(purchaseSaleRepository.findByVehicleId(30L)).thenReturn(List.of(new PurchaseSale()));

      List<PurchaseSale> result = service.findByVehicleId(30L);

      assertEquals(1, result.size());
      verify(entityResolutionService).resolveVehicleId(30L);
    }
  }
}
