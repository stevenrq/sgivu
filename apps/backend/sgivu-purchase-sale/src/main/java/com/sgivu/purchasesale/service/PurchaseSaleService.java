package com.sgivu.purchasesale.service;

import com.sgivu.purchasesale.dto.PurchaseSaleFilterCriteria;
import com.sgivu.purchasesale.dto.PurchaseSaleRequest;
import com.sgivu.purchasesale.entity.PurchaseSale;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PurchaseSaleService {

  PurchaseSale create(PurchaseSaleRequest purchaseSaleRequest);

  Optional<PurchaseSale> findById(Long id);

  List<PurchaseSale> findAll();

  Page<PurchaseSale> findAll(Pageable pageable);

  Optional<PurchaseSale> update(Long id, PurchaseSaleRequest purchaseSaleRequest);

  void deleteById(Long id);

  List<PurchaseSale> findByClientId(Long clientId);

  List<PurchaseSale> findByUserId(Long userId);

  List<PurchaseSale> findByVehicleId(Long vehicleId);

  Page<PurchaseSale> search(PurchaseSaleFilterCriteria criteria, Pageable pageable);

  /**
   * Obtiene los IDs de vehículos disponibles para venta. Un vehículo se considera disponible si ha
   * sido comprado y no tiene ventas registradas activas o completadas.
   *
   * @return lista de IDs de vehículos disponibles.
   */
  List<Long> findAvailableVehicleIds();
}
