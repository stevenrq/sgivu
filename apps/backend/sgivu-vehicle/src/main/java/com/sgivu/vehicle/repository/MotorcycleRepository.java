package com.sgivu.vehicle.repository;

import com.sgivu.vehicle.entity.Motorcycle;
import java.util.List;
import java.util.Optional;

public interface MotorcycleRepository extends VehicleRepository<Motorcycle> {
  Optional<Motorcycle> findByMotorcycleType(String motorcycleType);

  List<Motorcycle> findByMotorcycleTypeContainingIgnoreCase(String motorcycleType);
}
