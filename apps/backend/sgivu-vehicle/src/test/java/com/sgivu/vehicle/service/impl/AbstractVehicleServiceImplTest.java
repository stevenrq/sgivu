package com.sgivu.vehicle.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgivu.vehicle.entity.Vehicle;
import com.sgivu.vehicle.enums.VehicleStatus;
import com.sgivu.vehicle.repository.VehicleRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AbstractVehicleServiceImplTest {

  static class TestVehicle extends Vehicle {}

  static class TestVehicleService
      extends AbstractVehicleServiceImpl<TestVehicle, VehicleRepository<TestVehicle>> {

    protected TestVehicleService(VehicleRepository<TestVehicle> vehicleRepository) {
      super(vehicleRepository);
    }
  }

  @Mock private VehicleRepository<TestVehicle> vehicleRepository;

  @InjectMocks private TestVehicleService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new TestVehicleService(vehicleRepository);
  }

  @Nested
  @DisplayName("update(Long, T)")
  class UpdateTests {

    @Test
    @DisplayName("Debe actualizar campos del vehículo existente y guardar")
    void shouldUpdateExistingVehicleAndSave() {
      Long id = 1L;
      TestVehicle existing = new TestVehicle();
      existing.setId(id);
      existing.setBrand("OldBrand");
      existing.setModel("OldModel");
      existing.setYear(2000);
      existing.setPurchasePrice(1000.0);

      TestVehicle updated = new TestVehicle();
      updated.setBrand("NewBrand");
      updated.setModel("NewModel");
      updated.setYear(2020);
      updated.setPurchasePrice(2000.0);

      when(vehicleRepository.findById(id)).thenReturn(Optional.of(existing));
      when(vehicleRepository.save(any(TestVehicle.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Optional<TestVehicle> result = service.update(id, updated);

      assertTrue(result.isPresent());
      TestVehicle saved = result.get();
      assertEquals("NewBrand", saved.getBrand());
      assertEquals("NewModel", saved.getModel());
      assertEquals(2020, saved.getYear());
      assertEquals(2000.0, saved.getPurchasePrice());

      verify(vehicleRepository).findById(id);
      verify(vehicleRepository).save(existing);
    }

    @Test
    @DisplayName("Debe retornar Optional vacío cuando el vehículo no se encuentra")
    void shouldReturnEmptyWhenNotFound() {
      Long id = 99L;
      TestVehicle updated = new TestVehicle();

      when(vehicleRepository.findById(id)).thenReturn(Optional.empty());

      Optional<TestVehicle> result = service.update(id, updated);

      assertFalse(result.isPresent());
      verify(vehicleRepository).findById(id);
      verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe propagar excepción cuando falla el guardado del repositorio")
    void shouldPropagateExceptionWhenSaveFails() {
      Long id = 2L;
      TestVehicle existing = new TestVehicle();
      existing.setId(id);

      TestVehicle updated = new TestVehicle();
      updated.setBrand("X");

      when(vehicleRepository.findById(id)).thenReturn(Optional.of(existing));
      when(vehicleRepository.save(any(TestVehicle.class)))
          .thenThrow(new RuntimeException("DB error"));

      assertThrows(RuntimeException.class, () -> service.update(id, updated));
      verify(vehicleRepository).findById(id);
      verify(vehicleRepository).save(existing);
    }
  }

  @Nested
  @DisplayName("changeStatus(Long, VehicleStatus)")
  class ChangeStatusTests {

    @Test
    @DisplayName("Debe actualizar estado del vehículo cuando se encuentra y guardar")
    void shouldUpdateVehicleStatusWhenFoundAndSave() {
      Long id = 1L;
      TestVehicle vehicle = new TestVehicle();
      vehicle.setId(id);
      vehicle.setStatus(VehicleStatus.INACTIVE);

      when(vehicleRepository.findById(id)).thenReturn(Optional.of(vehicle));
      when(vehicleRepository.save(any(TestVehicle.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Optional<TestVehicle> result = service.changeStatus(id, VehicleStatus.AVAILABLE);

      assertTrue(result.isPresent());
      assertEquals(VehicleStatus.AVAILABLE, result.get().getStatus());
      verify(vehicleRepository).findById(id);
      verify(vehicleRepository).save(vehicle);
    }

    @Test
    @DisplayName("Debe retornar vacío si el vehículo no se encuentra")
    void shouldReturnEmptyIfVehicleNotFound() {
      Long id = 99L;
      when(vehicleRepository.findById(id)).thenReturn(Optional.empty());

      Optional<TestVehicle> result = service.changeStatus(id, VehicleStatus.AVAILABLE);

      assertFalse(result.isPresent());
      verify(vehicleRepository).findById(id);
      verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe propagar excepción cuando falla el guardado del repositorio")
    void shouldPropagateExceptionWhenSaveFails() {
      Long id = 2L;
      TestVehicle vehicle = new TestVehicle();
      vehicle.setId(id);

      when(vehicleRepository.findById(id)).thenReturn(Optional.of(vehicle));
      when(vehicleRepository.save(any(TestVehicle.class)))
          .thenThrow(new RuntimeException("DB error"));

      assertThrows(RuntimeException.class, () -> service.changeStatus(id, VehicleStatus.AVAILABLE));
      verify(vehicleRepository).findById(id);
      verify(vehicleRepository).save(vehicle);
    }
  }
}
