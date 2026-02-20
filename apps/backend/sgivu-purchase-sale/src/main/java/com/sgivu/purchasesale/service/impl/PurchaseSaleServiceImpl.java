package com.sgivu.purchasesale.service.impl;

import com.sgivu.purchasesale.dto.PurchaseSaleFilterCriteria;
import com.sgivu.purchasesale.dto.PurchaseSaleRequest;
import com.sgivu.purchasesale.entity.PurchaseSale;
import com.sgivu.purchasesale.enums.ContractStatus;
import com.sgivu.purchasesale.enums.ContractType;
import com.sgivu.purchasesale.exception.ContractValidationException;
import com.sgivu.purchasesale.exception.InvalidContractOperationException;
import com.sgivu.purchasesale.mapper.PurchaseSaleMapper;
import com.sgivu.purchasesale.repository.PurchaseSaleRepository;
import com.sgivu.purchasesale.service.ContractBusinessRuleService;
import com.sgivu.purchasesale.service.ContractReferenceResolver;
import com.sgivu.purchasesale.service.EntityResolutionService;
import com.sgivu.purchasesale.service.PurchaseSaleService;
import com.sgivu.purchasesale.specification.PurchaseSaleSpecifications;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestador del ciclo de vida de contratos de compra-venta. Coordina la resolución de entidades
 * externas, la referencia al vehículo, las reglas de negocio y la persistencia. Cada
 * responsabilidad específica está delegada a un servicio especializado:
 *
 * <ul>
 *   <li>{@link EntityResolutionService} — verifica existencia de clientes, vehículos y usuarios
 *   <li>{@link ContractReferenceResolver} — determina si crear o buscar un vehículo según el tipo
 *       de contrato
 *   <li>{@link ContractBusinessRuleService} — valida reglas de negocio y prepara precios
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class PurchaseSaleServiceImpl implements PurchaseSaleService {

  private final PurchaseSaleRepository purchaseSaleRepository;
  private final PurchaseSaleMapper purchaseSaleMapper;
  private final EntityResolutionService entityResolutionService;
  private final ContractReferenceResolver contractReferenceResolver;
  private final ContractBusinessRuleService contractBusinessRuleService;

  public PurchaseSaleServiceImpl(
      PurchaseSaleRepository purchaseSaleRepository,
      PurchaseSaleMapper purchaseSaleMapper,
      EntityResolutionService entityResolutionService,
      ContractReferenceResolver contractReferenceResolver,
      ContractBusinessRuleService contractBusinessRuleService) {
    this.purchaseSaleRepository = purchaseSaleRepository;
    this.purchaseSaleMapper = purchaseSaleMapper;
    this.entityResolutionService = entityResolutionService;
    this.contractReferenceResolver = contractReferenceResolver;
    this.contractBusinessRuleService = contractBusinessRuleService;
  }

  @Transactional
  @Override
  public PurchaseSale create(PurchaseSaleRequest purchaseSaleRequest) {
    ContractType contractType = normalizeContractType(purchaseSaleRequest);
    Long resolvedClientId =
        entityResolutionService.resolveClientId(purchaseSaleRequest.getClientId());
    Long resolvedUserId = entityResolutionService.resolveUserId(purchaseSaleRequest.getUserId());
    Long resolvedVehicleId =
        contractReferenceResolver.resolveVehicleReference(contractType, purchaseSaleRequest);
    List<PurchaseSale> contractsByVehicle =
        purchaseSaleRepository.findByVehicleId(resolvedVehicleId);
    contractBusinessRuleService.applyRules(
        contractType, purchaseSaleRequest, contractsByVehicle, null, resolvedVehicleId);

    PurchaseSale purchaseSale = purchaseSaleMapper.toPurchaseSale(purchaseSaleRequest);
    applyContractAdjustments(purchaseSale, purchaseSaleRequest);
    purchaseSale.setClientId(resolvedClientId);
    purchaseSale.setUserId(resolvedUserId);
    purchaseSale.setVehicleId(resolvedVehicleId);
    validatePurchasePrice(purchaseSale.getPurchasePrice());

    return purchaseSaleRepository.save(purchaseSale);
  }

  @Override
  public Optional<PurchaseSale> findById(Long id) {
    long resolvedId = requireContractId(id);
    return purchaseSaleRepository.findById(resolvedId);
  }

  @Override
  public List<PurchaseSale> findAll() {
    return purchaseSaleRepository.findAll();
  }

  @Override
  public Page<PurchaseSale> findAll(Pageable pageable) {
    return purchaseSaleRepository.findAll(requirePageable(pageable));
  }

  @Override
  public Page<PurchaseSale> search(PurchaseSaleFilterCriteria criteria, Pageable pageable) {
    return purchaseSaleRepository.findAll(
        PurchaseSaleSpecifications.withFilters(criteria), requirePageable(pageable));
  }

  @Transactional
  @Override
  public Optional<PurchaseSale> update(Long id, PurchaseSaleRequest purchaseSaleRequest) {
    long resolvedId = requireContractId(id);
    ContractType contractType = normalizeContractType(purchaseSaleRequest);
    Long resolvedClientId =
        entityResolutionService.resolveClientId(purchaseSaleRequest.getClientId());
    Long resolvedUserId = entityResolutionService.resolveUserId(purchaseSaleRequest.getUserId());
    Long resolvedVehicleId =
        entityResolutionService.resolveVehicleId(purchaseSaleRequest.getVehicleId());
    List<PurchaseSale> contractsByVehicle =
        purchaseSaleRepository.findByVehicleId(resolvedVehicleId);

    return purchaseSaleRepository
        .findById(resolvedId)
        .map(
            existingPurchaseSale -> {
              if (existingPurchaseSale.getContractType() != contractType) {
                throw new InvalidContractOperationException(
                    "No es posible cambiar el tipo de contrato una vez creado.");
              }
              contractBusinessRuleService.applyRules(
                  contractType,
                  purchaseSaleRequest,
                  contractsByVehicle,
                  existingPurchaseSale.getId(),
                  resolvedVehicleId);
              purchaseSaleMapper.updatePurchaseSaleFromRequest(
                  purchaseSaleRequest, existingPurchaseSale);
              applyContractAdjustments(existingPurchaseSale, purchaseSaleRequest);
              existingPurchaseSale.setClientId(resolvedClientId);
              existingPurchaseSale.setUserId(resolvedUserId);
              existingPurchaseSale.setVehicleId(resolvedVehicleId);
              validatePurchasePrice(existingPurchaseSale.getPurchasePrice());
              return purchaseSaleRepository.save(existingPurchaseSale);
            });
  }

  @Transactional
  @Override
  public void deleteById(Long id) {
    PurchaseSale purchaseSale =
        purchaseSaleRepository
            .findById(id)
            .orElseThrow(
                () -> new ContractValidationException("Contrato no encontrado con id: " + id));
    if (purchaseSale.getContractStatus() != ContractStatus.CANCELED) {
      throw new InvalidContractOperationException(
          "Solo se pueden eliminar contratos que estén en estado 'CANCELED'.");
    }

    purchaseSaleRepository.deleteById(requireContractId(id));
  }

  @Override
  public List<PurchaseSale> findByClientId(Long clientId) {
    Long resolvedClientId = entityResolutionService.resolveClientId(clientId);
    return purchaseSaleRepository.findByClientId(resolvedClientId);
  }

  @Override
  public List<PurchaseSale> findByUserId(Long userId) {
    Long resolvedUserId = entityResolutionService.resolveUserId(userId);
    return purchaseSaleRepository.findByUserId(resolvedUserId);
  }

  @Override
  public List<PurchaseSale> findByVehicleId(Long vehicleId) {
    Long resolvedVehicleId = entityResolutionService.resolveVehicleId(vehicleId);
    return purchaseSaleRepository.findByVehicleId(resolvedVehicleId);
  }

  /**
   * Normaliza el tipo de contrato y su estado cuando el request no los especifica explícitamente.
   * PURCHASE y PENDING son los valores por defecto porque representan el flujo más común: registrar
   * la compra de un vehículo nuevo que inicia en estado pendiente de aprobación.
   *
   * @param purchaseSaleRequest request a normalizar
   * @return el tipo de contrato normalizado, que se asigna al request para su uso posterior en el
   *     flujo
   */
  private ContractType normalizeContractType(PurchaseSaleRequest purchaseSaleRequest) {
    ContractType contractType =
        Optional.ofNullable(purchaseSaleRequest.getContractType()).orElse(ContractType.PURCHASE);
    purchaseSaleRequest.setContractType(contractType);
    if (purchaseSaleRequest.getContractStatus() == null) {
      purchaseSaleRequest.setContractStatus(ContractStatus.PENDING);
    }
    return contractType;
  }

  /**
   * Fija el tipo de contrato, estado y precio de venta en la entidad JPA tras el mapeo. El mapper
   * no gestiona estos campos porque sus valores dependen de reglas de negocio aplicadas previamente
   * por {@link ContractBusinessRuleService} (por ejemplo, el salePrice de una compra puede ser 0).
   *
   * @param purchaseSale entidad JPA a ajustar antes de persistir
   * @param purchaseSaleRequest request que contiene los valores de tipo, estado y precio de venta
   *     ya validados y preparados por ContractBusinessRuleService
   */
  private void applyContractAdjustments(
      PurchaseSale purchaseSale, PurchaseSaleRequest purchaseSaleRequest) {
    purchaseSale.setContractType(purchaseSaleRequest.getContractType());
    purchaseSale.setContractStatus(purchaseSaleRequest.getContractStatus());
    if (purchaseSaleRequest.getContractType() == ContractType.PURCHASE) {
      purchaseSale.setSalePrice(Optional.ofNullable(purchaseSaleRequest.getSalePrice()).orElse(0d));
    } else {
      purchaseSale.setSalePrice(purchaseSaleRequest.getSalePrice());
    }
  }

  /**
   * Última barrera de integridad antes de persistir: un contrato sin precio de compra válido
   * indicaría un fallo en los pasos anteriores del flujo que debe detenerse aquí.
   *
   * @param purchasePrice precio de compra a validar, que debe ser mayor a cero
   * @throws ContractValidationException si el precio de compra es nulo o no positivo
   */
  private void validatePurchasePrice(Double purchasePrice) {
    if (purchasePrice == null || purchasePrice <= 0) {
      throw new ContractValidationException("El precio de compra debe ser mayor a cero.");
    }
  }

  private long requireContractId(Long contractId) {
    if (contractId == null) {
      throw new ContractValidationException("El ID del contrato debe ser proporcionado.");
    }
    return contractId;
  }

  private Pageable requirePageable(Pageable pageable) {
    return Objects.requireNonNull(pageable, "La configuración de paginación es obligatoria.");
  }
}
