package com.sgivu.client.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgivu.client.entity.Person;
import com.sgivu.client.repository.PersonRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PersonServiceImplTest {

  @Mock private PersonRepository personRepository;

  @InjectMocks private PersonServiceImpl personService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Nested
  @DisplayName("update(Long, Person)")
  class UpdateTests {

    @Test
    @DisplayName("should update first and last name and save")
    void shouldUpdateNamesAndSave() {
      Long id = 1L;
      Person existing = new Person();
      existing.setId(id);
      existing.setFirstName("OldFirst");
      existing.setLastName("OldLast");

      Person updated = new Person();
      updated.setFirstName("NewFirst");
      updated.setLastName("NewLast");

      when(personRepository.findById(id)).thenReturn(Optional.of(existing));
      when(personRepository.save(any(Person.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Optional<Person> result = personService.update(id, updated);

      assertTrue(result.isPresent());
      Person saved = result.get();
      assertEquals("NewFirst", saved.getFirstName());
      assertEquals("NewLast", saved.getLastName());
      verify(personRepository).findById(id);
      verify(personRepository).save(existing);
    }

    @Test
    @DisplayName("should return empty when person not found")
    void shouldReturnEmptyWhenNotFound() {
      Long id = 99L;
      Person updated = new Person();
      updated.setFirstName("Anything");

      when(personRepository.findById(id)).thenReturn(Optional.empty());

      Optional<Person> result = personService.update(id, updated);

      assertFalse(result.isPresent());
      verify(personRepository).findById(id);
      verify(personRepository, never()).save(any());
    }

    @Test
    @DisplayName("should propagate exception when repository save fails")
    void shouldPropagateExceptionWhenSaveFails() {
      Long id = 2L;
      Person existing = new Person();
      existing.setId(id);
      existing.setFirstName("Old");

      Person updated = new Person();
      updated.setFirstName("New");

      when(personRepository.findById(id)).thenReturn(Optional.of(existing));
      when(personRepository.save(any(Person.class))).thenThrow(new RuntimeException("DB error"));

      assertThrows(RuntimeException.class, () -> personService.update(id, updated));
      verify(personRepository).findById(id);
      verify(personRepository).save(existing);
    }
  }
}
