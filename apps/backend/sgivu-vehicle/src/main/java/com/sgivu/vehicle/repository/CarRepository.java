package com.sgivu.vehicle.repository;

import com.sgivu.vehicle.entity.Car;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio específico para autos.
 *
 * <p>Provee consultas derivadas para atributos usados en segmentación de catálogo (tipo de
 * combustible, carrocería), reduciendo lógica en la capa de servicio.
 */
public interface CarRepository extends VehicleRepository<Car> {
  Optional<Car> findByFuelType(String fuelType);

  Optional<Car> findByBodyType(String bodyType);

  List<Car> findByFuelTypeContainingIgnoreCase(String fuelType);

  List<Car> findByBodyTypeContainingIgnoreCase(String bodyType);
}
