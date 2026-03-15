package com.sgivu.vehicle.repository;

import com.sgivu.vehicle.entity.Vehicle;
import com.sgivu.vehicle.enums.VehicleStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VehicleRepository<T extends Vehicle>
    extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
  Optional<T> findByPlate(String plate);

  List<T> findByPlateContainingIgnoreCase(String plate);

  List<T> findByBrandContainingIgnoreCase(String brand);

  List<T> findByModelContainingIgnoreCase(String model);

  List<T> findByLineContainingIgnoreCase(String line);

  long countByStatus(VehicleStatus status);
}
