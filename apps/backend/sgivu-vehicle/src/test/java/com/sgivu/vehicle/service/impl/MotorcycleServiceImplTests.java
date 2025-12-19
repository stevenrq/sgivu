package com.sgivu.vehicle.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sgivu.vehicle.entity.Motorcycle;
import com.sgivu.vehicle.enums.VehicleStatus;
import com.sgivu.vehicle.repository.MotorcycleRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MotorcycleServiceImplTests {

  @Mock private MotorcycleRepository motorcycleRepository;

  @InjectMocks private MotorcycleServiceImpl motorcycleService;

  private Motorcycle existingMoto;

  @BeforeEach
  void setUp() {
    existingMoto = buildMoto(1L, "Yamaha", "MT-07", "Naked");
  }

  @Test
  void update_shouldMergeBaseAndSpecificFields() {
    Motorcycle request = buildMoto(null, "Kawasaki", "Z900", "Sport");
    request.setPlate("MOTO99");
    request.setMotorNumber("MTR-900");
    request.setSerialNumber("SER-900");
    request.setChassisNumber("CHA-900");
    request.setCityRegistered("Medellin");
    request.setColor("Verde");
    request.setYear(2024);
    request.setMileage(3000);
    request.setTransmission("Manual");
    request.setStatus(VehicleStatus.IN_REPAIR);
    request.setPurchasePrice(9000d);
    request.setSalePrice(11000d);

    when(motorcycleRepository.findById(1L)).thenReturn(Optional.of(existingMoto));
    when(motorcycleRepository.save(any(Motorcycle.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<Motorcycle> result = motorcycleService.update(1L, request);

    assertTrue(result.isPresent());
    Motorcycle updated = result.orElseThrow();
    assertEquals("Kawasaki", updated.getBrand());
    assertEquals("Z900", updated.getModel());
    assertEquals(2, updated.getCapacity());
    assertEquals("Sport", updated.getMotorcycleType());
    assertEquals("MOTO99", updated.getPlate());
    assertEquals("MTR-900", updated.getMotorNumber());
    assertEquals("SER-900", updated.getSerialNumber());
    assertEquals("CHA-900", updated.getChassisNumber());
    assertEquals("Medellin", updated.getCityRegistered());
    assertEquals("Verde", updated.getColor());
    assertEquals(2024, updated.getYear());
    assertEquals(3000, updated.getMileage());
    assertEquals("Manual", updated.getTransmission());
    assertEquals(VehicleStatus.IN_REPAIR, updated.getStatus());
    assertEquals(9000d, updated.getPurchasePrice());
    assertEquals(11000d, updated.getSalePrice());
  }

  @Test
  void changeStatus_shouldUpdateWhenMotorcycleExists() {
    when(motorcycleRepository.findById(1L)).thenReturn(Optional.of(existingMoto));
    when(motorcycleRepository.save(any(Motorcycle.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<Motorcycle> result = motorcycleService.changeStatus(1L, VehicleStatus.SOLD);

    assertTrue(result.isPresent());
    assertEquals(VehicleStatus.SOLD, result.orElseThrow().getStatus());
    verify(motorcycleRepository).save(existingMoto);
  }

  @Test
  void changeStatus_shouldReturnEmptyWhenNotFound() {
    when(motorcycleRepository.findById(2L)).thenReturn(Optional.empty());

    Optional<Motorcycle> result = motorcycleService.changeStatus(2L, VehicleStatus.SOLD);

    assertTrue(result.isEmpty());
    verify(motorcycleRepository, never()).save(any(Motorcycle.class));
  }

  private Motorcycle buildMoto(Long id, String brand, String model, String type) {
    Motorcycle moto = new Motorcycle();
    moto.setId(id);
    moto.setBrand(brand);
    moto.setModel(model);
    moto.setCapacity(2);
    moto.setLine("Base");
    moto.setPlate("MTO123");
    moto.setMotorNumber("MTR-001");
    moto.setSerialNumber("SER-001");
    moto.setChassisNumber("CHA-001");
    moto.setColor("Negro");
    moto.setCityRegistered("Bogota");
    moto.setYear(2023);
    moto.setMileage(1500);
    moto.setTransmission("Manual");
    moto.setStatus(VehicleStatus.AVAILABLE);
    moto.setPurchasePrice(5000d);
    moto.setSalePrice(6500d);
    moto.setMotorcycleType(type);
    return moto;
  }
}
