package com.sgivu.vehicle.service;

import com.sgivu.vehicle.dto.MotorcycleSearchCriteria;
import com.sgivu.vehicle.entity.Motorcycle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MotorcycleService extends VehicleService<Motorcycle> {
  Optional<Motorcycle> findByMotorcycleType(String motorcycleType);

  List<Motorcycle> findByMotorcycleTypeContainingIgnoreCase(String motorcycleType);

  List<Motorcycle> search(MotorcycleSearchCriteria criteria);

  Page<Motorcycle> search(MotorcycleSearchCriteria criteria, Pageable pageable);
}
