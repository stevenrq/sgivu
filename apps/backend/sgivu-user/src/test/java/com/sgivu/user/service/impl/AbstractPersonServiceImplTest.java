package com.sgivu.user.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgivu.user.entity.Address;
import com.sgivu.user.entity.Person;
import com.sgivu.user.repository.PersonRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AbstractPersonServiceImplTest {

  static class TestPerson extends Person {}

  static class TestPersonService
      extends AbstractPersonServiceImpl<TestPerson, PersonRepository<TestPerson>> {

    protected TestPersonService(PersonRepository<TestPerson> personRepository) {
      super(personRepository);
    }
  }

  @Mock private PersonRepository<TestPerson> personRepository;

  @InjectMocks private TestPersonService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new TestPersonService(personRepository);
  }

  @Nested
  @DisplayName("update(Long, T)")
  class UpdateTests {

    @Test
    @DisplayName("Debe actualizar campos de la persona existente y guardar")
    void shouldUpdateExistingPersonAndSave() {
      Long id = 1L;
      TestPerson existing = new TestPerson();
      existing.setId(id);
      existing.setFirstName("OldFirst");
      existing.setLastName("OldLast");
      existing.setEmail("old@example.com");
      existing.setPhoneNumber(111111111L);
      Address oldAddress = new Address();
      oldAddress.setStreet("Old St");
      existing.setAddress(oldAddress);

      TestPerson updated = new TestPerson();
      updated.setFirstName("NewFirst");
      updated.setLastName("NewLast");
      updated.setEmail("new@example.com");
      updated.setPhoneNumber(222222222L);
      Address newAddress = new Address();
      newAddress.setStreet("New St");
      updated.setAddress(newAddress);

      when(personRepository.findById(id)).thenReturn(Optional.of(existing));
      when(personRepository.save(any(TestPerson.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Optional<TestPerson> result = service.update(id, updated);

      assertTrue(result.isPresent());
      TestPerson saved = result.get();
      assertEquals("NewFirst", saved.getFirstName());
      assertEquals("NewLast", saved.getLastName());
      assertEquals("new@example.com", saved.getEmail());
      assertEquals(222222222L, saved.getPhoneNumber());
      assertEquals("New St", saved.getAddress().getStreet());
      verify(personRepository).findById(id);
      verify(personRepository).save(existing);
    }

    @Test
    @DisplayName("Debe retornar Optional vacío cuando la persona no se encuentra")
    void shouldReturnEmptyWhenNotFound() {
      Long id = 99L;
      TestPerson updated = new TestPerson();

      when(personRepository.findById(id)).thenReturn(Optional.empty());

      Optional<TestPerson> result = service.update(id, updated);

      assertFalse(result.isPresent());
      verify(personRepository).findById(id);
      verify(personRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe propagar excepción cuando falla el guardado del repositorio")
    void shouldPropagateExceptionWhenSaveFails() {
      Long id = 2L;
      TestPerson existing = new TestPerson();
      existing.setId(id);

      TestPerson updated = new TestPerson();
      updated.setFirstName("X");

      when(personRepository.findById(id)).thenReturn(Optional.of(existing));
      when(personRepository.save(any(TestPerson.class)))
          .thenThrow(new RuntimeException("DB error"));

      assertThrows(RuntimeException.class, () -> service.update(id, updated));
      verify(personRepository).findById(id);
      verify(personRepository).save(existing);
    }
  }
}
