package com.sgivu.vehicle.service;

import com.sgivu.vehicle.dto.CarSearchCriteria;
import com.sgivu.vehicle.entity.Car;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CarService extends VehicleService<Car> {
  Optional<Car> findByFuelType(String fuelType);

  Optional<Car> findByBodyType(String bodyType);

  List<Car> findByFuelTypeContainingIgnoreCase(String fuelType);

  List<Car> findByBodyTypeContainingIgnoreCase(String bodyType);

  List<Car> search(CarSearchCriteria criteria);

  Page<Car> search(CarSearchCriteria criteria, Pageable pageable);
}
