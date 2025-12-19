package com.sgivu.vehicle.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sgivu.vehicle.entity.Car;
import com.sgivu.vehicle.enums.VehicleStatus;
import com.sgivu.vehicle.repository.CarRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CarServiceImplTests {

  @Mock private CarRepository carRepository;

  @InjectMocks private CarServiceImpl carService;

  private Car existingCar;

  @BeforeEach
  void setUp() {
    existingCar = buildCar(1L, "Mazda", "3", "Sedan", "Gasolina", 4);
    existingCar.setStatus(VehicleStatus.AVAILABLE);
  }

  @Test
  void update_shouldMergeBaseAndSpecificFields() {
    Car request = buildCar(null, "Honda", "Civic", "Hatchback", "Hibrido", 5);
    request.setPlate("XYZ987");
    request.setMotorNumber("MTR-002");
    request.setSerialNumber("SER-002");
    request.setChassisNumber("CHA-002");
    request.setCityRegistered("Medellin");
    request.setColor("Azul");
    request.setYear(2024);
    request.setMileage(12000);
    request.setTransmission("Automatica");
    request.setStatus(VehicleStatus.IN_MAINTENANCE);
    request.setPurchasePrice(20000d);
    request.setSalePrice(25000d);

    when(carRepository.findById(1L)).thenReturn(Optional.of(existingCar));
    when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<Car> result = carService.update(1L, request);

    assertTrue(result.isPresent());
    Car updated = result.orElseThrow();
    assertEquals("Honda", updated.getBrand());
    assertEquals("Civic", updated.getModel());
    assertEquals(5, updated.getCapacity());
    assertEquals("Hatchback", updated.getBodyType());
    assertEquals("Hibrido", updated.getFuelType());
    assertEquals(5, updated.getNumberOfDoors());
    assertEquals("XYZ987", updated.getPlate());
    assertEquals("MTR-002", updated.getMotorNumber());
    assertEquals("SER-002", updated.getSerialNumber());
    assertEquals("CHA-002", updated.getChassisNumber());
    assertEquals("Medellin", updated.getCityRegistered());
    assertEquals("Azul", updated.getColor());
    assertEquals(2024, updated.getYear());
    assertEquals(12000, updated.getMileage());
    assertEquals("Automatica", updated.getTransmission());
    assertEquals(VehicleStatus.IN_MAINTENANCE, updated.getStatus());
    assertEquals(20000d, updated.getPurchasePrice());
    assertEquals(25000d, updated.getSalePrice());
  }

  @Test
  void changeStatus_shouldUpdateWhenVehicleExists() {
    when(carRepository.findById(1L)).thenReturn(Optional.of(existingCar));
    when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<Car> result = carService.changeStatus(1L, VehicleStatus.SOLD);

    assertTrue(result.isPresent());
    assertEquals(VehicleStatus.SOLD, result.orElseThrow().getStatus());
    verify(carRepository).save(existingCar);
  }

  @Test
  void changeStatus_shouldReturnEmptyWhenVehicleIsMissing() {
    when(carRepository.findById(2L)).thenReturn(Optional.empty());

    Optional<Car> result = carService.changeStatus(2L, VehicleStatus.SOLD);

    assertTrue(result.isEmpty());
    verify(carRepository, never()).save(any(Car.class));
  }

  private Car buildCar(
      Long id, String brand, String model, String bodyType, String fuelType, int doors) {
    Car car = new Car();
    car.setId(id);
    car.setBrand(brand);
    car.setModel(model);
    car.setCapacity(5);
    car.setLine("Touring");
    car.setPlate("ABC123");
    car.setMotorNumber("MTR-001");
    car.setSerialNumber("SER-001");
    car.setChassisNumber("CHA-001");
    car.setColor("Negro");
    car.setCityRegistered("Bogota");
    car.setYear(2023);
    car.setMileage(5000);
    car.setTransmission("Mecanica");
    car.setStatus(VehicleStatus.AVAILABLE);
    car.setPurchasePrice(15000d);
    car.setSalePrice(18000d);
    car.setBodyType(bodyType);
    car.setFuelType(fuelType);
    car.setNumberOfDoors(doors);
    return car;
  }
}
