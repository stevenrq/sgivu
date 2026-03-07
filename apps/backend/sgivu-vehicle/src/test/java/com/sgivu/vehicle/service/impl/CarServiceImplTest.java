package com.sgivu.vehicle.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgivu.vehicle.entity.Car;
import com.sgivu.vehicle.repository.CarRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CarServiceImplTest {

  @Mock private CarRepository carRepository;

  @InjectMocks private CarServiceImpl carService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Nested
  @DisplayName("update(Long, Car)")
  class UpdateTests {

    @Test
    @DisplayName("Debe actualizar campos específicos del auto y guardar")
    void shouldUpdateCarSpecificFieldsAndSave() {
      Long id = 1L;
      Car existing = new Car();
      existing.setId(id);
      existing.setBodyType("Sedan");
      existing.setFuelType("Gasoline");
      existing.setNumberOfDoors(4);

      Car updated = new Car();
      updated.setBodyType("Hatchback");
      updated.setFuelType("Electric");
      updated.setNumberOfDoors(2);

      when(carRepository.findById(id)).thenReturn(Optional.of(existing));
      when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

      Optional<Car> result = carService.update(id, updated);

      assertTrue(result.isPresent());
      Car saved = result.get();
      assertEquals("Hatchback", saved.getBodyType());
      assertEquals("Electric", saved.getFuelType());
      assertEquals(2, saved.getNumberOfDoors());

      verify(carRepository).findById(id);
      // saved twice: in super.update and in CarServiceImpl.update
      verify(carRepository, atLeastOnce()).save(any(Car.class));
    }

    @Test
    @DisplayName("Debe retornar Optional vacío cuando el auto no se encuentra")
    void shouldReturnEmptyWhenNotFound() {
      Long id = 99L;
      Car updated = new Car();

      when(carRepository.findById(id)).thenReturn(Optional.empty());

      Optional<Car> result = carService.update(id, updated);

      assertFalse(result.isPresent());
      verify(carRepository).findById(id);
      verify(carRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe propagar excepción cuando falla el guardado del repositorio")
    void shouldPropagateExceptionWhenSaveFails() {
      Long id = 2L;
      Car existing = new Car();
      existing.setId(id);

      Car updated = new Car();
      updated.setBodyType("Coupe");

      when(carRepository.findById(id)).thenReturn(Optional.of(existing));
      when(carRepository.save(any(Car.class))).thenThrow(new RuntimeException("DB error"));

      assertThrows(RuntimeException.class, () -> carService.update(id, updated));
      verify(carRepository).findById(id);
      verify(carRepository).save(any(Car.class));
    }
  }
}
