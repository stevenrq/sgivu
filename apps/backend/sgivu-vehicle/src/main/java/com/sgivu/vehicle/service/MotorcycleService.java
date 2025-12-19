package com.sgivu.vehicle.service;

import com.sgivu.vehicle.dto.MotorcycleSearchCriteria;
import com.sgivu.vehicle.entity.Motorcycle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Servicio de dominio para motocicletas usadas.
 *
 * <p>Añade filtros por tipo de moto para controlar elegibilidad en venta y campañas comerciales.
 */
public interface MotorcycleService extends VehicleService<Motorcycle> {
  Optional<Motorcycle> findByMotorcycleType(String motorcycleType);

  List<Motorcycle> findByMotorcycleTypeContainingIgnoreCase(String motorcycleType);

  List<Motorcycle> search(MotorcycleSearchCriteria criteria);

  Page<Motorcycle> search(MotorcycleSearchCriteria criteria, Pageable pageable);
}
