package com.sgivu.purchasesale.service;

import com.sgivu.purchasesale.dto.PurchaseSaleFilterCriteria;
import com.sgivu.purchasesale.dto.PurchaseSaleRequest;
import com.sgivu.purchasesale.entity.PurchaseSale;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PurchaseSaleService {

  /** Registra un contrato alineado con inventario, clientes y usuarios externos. */
  PurchaseSale create(PurchaseSaleRequest purchaseSaleRequest);

  Optional<PurchaseSale> findById(Long id);

  List<PurchaseSale> findAll();

  Page<PurchaseSale> findAll(Pageable pageable);

  /**
   * Actualiza un contrato manteniendo su tipo original y revalidando reglas de negocio.
   *
   * @param id identificador del contrato
   * @param purchaseSaleRequest datos a modificar
   * @return contrato actualizado o vacío si no existe
   */
  Optional<PurchaseSale> update(Long id, PurchaseSaleRequest purchaseSaleRequest);

  void deleteById(Long id);

  List<PurchaseSale> findByClientId(Long clientId);

  List<PurchaseSale> findByUserId(Long userId);

  List<PurchaseSale> findByVehicleId(Long vehicleId);

  /**
   * Búsqueda avanzada aplicando filtros combinados de negocio.
   *
   * @param criteria filtros opcionales (estado, tipo, fechas, precios, término libre)
   * @param pageable paginación
   * @return página filtrada de contratos
   */
  Page<PurchaseSale> search(PurchaseSaleFilterCriteria criteria, Pageable pageable);
}
