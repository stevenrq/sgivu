package com.sgivu.vehicle.service.impl;

import com.sgivu.vehicle.dto.CarSearchCriteria;
import com.sgivu.vehicle.entity.Car;
import com.sgivu.vehicle.repository.CarRepository;
import com.sgivu.vehicle.service.CarService;
import com.sgivu.vehicle.specification.CarSpecifications;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CarServiceImpl extends AbstractVehicleServiceImpl<Car, CarRepository>
    implements CarService {

  private final CarRepository carRepository;

  public CarServiceImpl(CarRepository carRepository) {
    super(carRepository);
    this.carRepository = carRepository;
  }

  @Override
  public Optional<Car> findByFuelType(String fuelType) {
    return carRepository.findByFuelType(fuelType);
  }

  @Override
  public Optional<Car> findByBodyType(String bodyType) {
    return carRepository.findByBodyType(bodyType);
  }

  @Override
  public List<Car> findByFuelTypeContainingIgnoreCase(String fuelType) {
    return carRepository.findByFuelTypeContainingIgnoreCase(fuelType);
  }

  @Override
  public List<Car> findByBodyTypeContainingIgnoreCase(String bodyType) {
    return carRepository.findByBodyTypeContainingIgnoreCase(bodyType);
  }

  @Override
  public List<Car> search(CarSearchCriteria criteria) {
    return search(criteria, Pageable.unpaged()).getContent();
  }

  @Override
  public Page<Car> search(CarSearchCriteria criteria, Pageable pageable) {
    return carRepository.findAll(CarSpecifications.withFilters(criteria), pageable);
  }

  @Transactional
  @Override
  public Optional<Car> update(Long id, Car vehicle) {
    return super.update(id, vehicle)
        .map(
            updated -> {
              updated.setBodyType(vehicle.getBodyType());
              updated.setFuelType(vehicle.getFuelType());
              updated.setNumberOfDoors(vehicle.getNumberOfDoors());
              return carRepository.save(updated);
            });
  }
}
