package com.sgivu.vehicle.service.impl;

import com.sgivu.vehicle.dto.MotorcycleSearchCriteria;
import com.sgivu.vehicle.entity.Motorcycle;
import com.sgivu.vehicle.repository.MotorcycleRepository;
import com.sgivu.vehicle.service.MotorcycleService;
import com.sgivu.vehicle.specification.MotorcycleSpecifications;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MotorcycleServiceImpl
    extends AbstractVehicleServiceImpl<Motorcycle, MotorcycleRepository>
    implements MotorcycleService {

  private final MotorcycleRepository motorcycleRepository;

  public MotorcycleServiceImpl(MotorcycleRepository motorcycleRepository) {
    super(motorcycleRepository);
    this.motorcycleRepository = motorcycleRepository;
  }

  @Override
  public Optional<Motorcycle> findByMotorcycleType(String motorcycleType) {
    return motorcycleRepository.findByMotorcycleType(motorcycleType);
  }

  @Override
  public List<Motorcycle> findByMotorcycleTypeContainingIgnoreCase(String motorcycleType) {
    return motorcycleRepository.findByMotorcycleTypeContainingIgnoreCase(motorcycleType);
  }

  @Override
  public List<Motorcycle> search(MotorcycleSearchCriteria criteria) {
    return search(criteria, Pageable.unpaged()).getContent();
  }

  @Override
  public Page<Motorcycle> search(MotorcycleSearchCriteria criteria, Pageable pageable) {
    return motorcycleRepository.findAll(MotorcycleSpecifications.withFilters(criteria), pageable);
  }

  @Transactional
  @Override
  public Optional<Motorcycle> update(Long id, Motorcycle vehicle) {
    return super.update(id, vehicle)
        .map(
            updated -> {
              updated.setMotorcycleType(vehicle.getMotorcycleType());
              return motorcycleRepository.save(updated);
            });
  }
}
