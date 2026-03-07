package com.sgivu.vehicle.service.impl;

import com.sgivu.vehicle.entity.Vehicle;
import com.sgivu.vehicle.enums.VehicleStatus;
import com.sgivu.vehicle.repository.VehicleRepository;
import com.sgivu.vehicle.service.VehicleService;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public abstract class AbstractVehicleServiceImpl<T extends Vehicle, R extends VehicleRepository<T>>
    implements VehicleService<T> {

  protected final R vehicleRepository;

  protected AbstractVehicleServiceImpl(R vehicleRepository) {
    this.vehicleRepository = vehicleRepository;
  }

  @Transactional
  @Override
  public T save(T vehicle) {
    return vehicleRepository.save(vehicle);
  }

  @Override
  public List<T> findAll() {
    return vehicleRepository.findAll();
  }

  @Override
  public Page<T> findAll(Pageable pageable) {
    return vehicleRepository.findAll(pageable);
  }

  @Override
  public Optional<T> findById(Long id) {
    return vehicleRepository.findById(id);
  }

  @Transactional
  @Override
  public Optional<T> update(Long id, T vehicle) {
    return vehicleRepository
        .findById(id)
        .map(
            existing -> {
              existing.setBrand(vehicle.getBrand());
              existing.setModel(vehicle.getModel());
              existing.setCapacity(vehicle.getCapacity());
              existing.setLine(vehicle.getLine());
              existing.setPlate(vehicle.getPlate());
              existing.setMotorNumber(vehicle.getMotorNumber());
              existing.setSerialNumber(vehicle.getSerialNumber());
              existing.setChassisNumber(vehicle.getChassisNumber());
              existing.setColor(vehicle.getColor());
              existing.setCityRegistered(vehicle.getCityRegistered());
              existing.setYear(vehicle.getYear());
              existing.setMileage(vehicle.getMileage());
              existing.setTransmission(vehicle.getTransmission());
              existing.setStatus(vehicle.getStatus());
              existing.setPurchasePrice(vehicle.getPurchasePrice());
              existing.setSalePrice(vehicle.getSalePrice());
              return vehicleRepository.save(existing);
            });
  }

  @Transactional
  @Override
  public void deleteById(Long id) {
    vehicleRepository.deleteById(id);
  }

  @Transactional
  @Override
  public Optional<T> changeStatus(Long id, VehicleStatus status) {
    return vehicleRepository
        .findById(id)
        .map(
            vehicle -> {
              vehicle.setStatus(status);
              vehicleRepository.save(vehicle);
              return Optional.of(vehicle);
            })
        .orElse(Optional.empty());
  }

  @Override
  public long countByStatus(VehicleStatus status) {
    return vehicleRepository.countByStatus(status);
  }

  @Override
  public List<T> findByBrandContainingIgnoreCase(String brand) {
    return vehicleRepository.findByBrandContainingIgnoreCase(brand);
  }

  @Override
  public List<T> findByLineContainingIgnoreCase(String line) {
    return vehicleRepository.findByLineContainingIgnoreCase(line);
  }

  @Override
  public List<T> findByModelContainingIgnoreCase(String model) {
    return vehicleRepository.findByModelContainingIgnoreCase(model);
  }

  @Override
  public Optional<T> findByPlate(String plate) {
    return vehicleRepository.findByPlate(plate);
  }

  @Override
  public List<T> findByPlateContainingIgnoreCase(String plate) {
    return vehicleRepository.findByPlateContainingIgnoreCase(plate);
  }
}
