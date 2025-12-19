package com.sgivu.vehicle.repository;

import com.sgivu.vehicle.entity.Motorcycle;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio especializado para motocicletas.
 *
 * <p>Expone consultas por tipo de moto necesarias para reglas de negocio (restricciones de
 * aseguradora, campañas) sin mezclar SQL en la capa de servicio.
 */
public interface MotorcycleRepository extends VehicleRepository<Motorcycle> {
  Optional<Motorcycle> findByMotorcycleType(String motorcycleType);

  List<Motorcycle> findByMotorcycleTypeContainingIgnoreCase(String motorcycleType);
}
