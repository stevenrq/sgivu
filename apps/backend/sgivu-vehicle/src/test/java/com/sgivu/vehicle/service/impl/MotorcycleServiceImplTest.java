package com.sgivu.vehicle.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgivu.vehicle.entity.Motorcycle;
import com.sgivu.vehicle.repository.MotorcycleRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MotorcycleServiceImplTest {

  @Mock private MotorcycleRepository motorcycleRepository;

  @InjectMocks private MotorcycleServiceImpl motorcycleService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Nested
  @DisplayName("update(Long, Motorcycle)")
  class UpdateTests {

    @Test
    @DisplayName("Debe actualizar campos específicos de la motocicleta y guardar")
    void shouldUpdateMotorcycleSpecificFieldsAndSave() {
      Long id = 1L;
      Motorcycle existing = new Motorcycle();
      existing.setId(id);
      existing.setMotorcycleType("Sport");

      Motorcycle updated = new Motorcycle();
      updated.setMotorcycleType("Cruiser");

      when(motorcycleRepository.findById(id)).thenReturn(Optional.of(existing));
      when(motorcycleRepository.save(any(Motorcycle.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Optional<Motorcycle> result = motorcycleService.update(id, updated);

      assertTrue(result.isPresent());
      Motorcycle saved = result.get();
      assertEquals("Cruiser", saved.getMotorcycleType());

      verify(motorcycleRepository).findById(id);
      verify(motorcycleRepository, atLeastOnce()).save(any(Motorcycle.class));
    }

    @Test
    @DisplayName("Debe retornar Optional vacío cuando la motocicleta no se encuentra")
    void shouldReturnEmptyWhenNotFound() {
      Long id = 99L;
      Motorcycle updated = new Motorcycle();

      when(motorcycleRepository.findById(id)).thenReturn(Optional.empty());

      Optional<Motorcycle> result = motorcycleService.update(id, updated);

      assertFalse(result.isPresent());
      verify(motorcycleRepository).findById(id);
      verify(motorcycleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe propagar excepción cuando falla el guardado del repositorio")
    void shouldPropagateExceptionWhenSaveFails() {
      Long id = 2L;
      Motorcycle existing = new Motorcycle();
      existing.setId(id);

      Motorcycle updated = new Motorcycle();
      updated.setMotorcycleType("Tourer");

      when(motorcycleRepository.findById(id)).thenReturn(Optional.of(existing));
      when(motorcycleRepository.save(any(Motorcycle.class)))
          .thenThrow(new RuntimeException("DB error"));

      assertThrows(RuntimeException.class, () -> motorcycleService.update(id, updated));
      verify(motorcycleRepository).findById(id);
      verify(motorcycleRepository).save(any(Motorcycle.class));
    }
  }
}
