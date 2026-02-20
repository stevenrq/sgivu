package com.sgivu.vehicle.service;

import com.sgivu.vehicle.entity.Vehicle;
import com.sgivu.vehicle.enums.VehicleStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VehicleService<T extends Vehicle> {
  T save(T vehicle);

  Optional<T> findById(Long id);

  List<T> findAll();

  Page<T> findAll(Pageable pageable);

  Optional<T> update(Long id, T vehicle);

  void deleteById(Long id);

  Optional<T> findByPlate(String plate);

  Optional<T> changeStatus(Long id, VehicleStatus status);

  long countByStatus(VehicleStatus status);

  List<T> findByPlateContainingIgnoreCase(String plate);

  List<T> findByBrandContainingIgnoreCase(String brand);

  List<T> findByModelContainingIgnoreCase(String model);

  List<T> findByLineContainingIgnoreCase(String line);
}
