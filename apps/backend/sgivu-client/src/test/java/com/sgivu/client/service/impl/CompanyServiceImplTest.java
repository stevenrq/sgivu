package com.sgivu.client.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgivu.client.entity.Company;
import com.sgivu.client.repository.CompanyRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CompanyServiceImplTest {

  @Mock private CompanyRepository companyRepository;

  @InjectMocks private CompanyServiceImpl companyService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Nested
  @DisplayName("update(Long, Company)")
  class UpdateTests {

    @Test
    @DisplayName("Debe actualizar nombre de la empresa y guardar")
    void shouldUpdateCompanyNameAndSave() {
      Long id = 1L;
      Company existing = new Company();
      existing.setId(id);
      existing.setCompanyName("OldCo");

      Company updated = new Company();
      updated.setCompanyName("NewCo");

      when(companyRepository.findById(id)).thenReturn(Optional.of(existing));
      when(companyRepository.save(any(Company.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Optional<Company> result = companyService.update(id, updated);

      assertTrue(result.isPresent());
      Company saved = result.get();
      assertEquals("NewCo", saved.getCompanyName());
      verify(companyRepository).findById(id);
      verify(companyRepository).save(existing);
    }

    @Test
    @DisplayName("Debe retornar Optional vacío cuando la empresa no se encuentra")
    void shouldReturnEmptyWhenNotFound() {
      Long id = 99L;
      Company updated = new Company();
      updated.setCompanyName("Anything");

      when(companyRepository.findById(id)).thenReturn(Optional.empty());

      Optional<Company> result = companyService.update(id, updated);

      assertFalse(result.isPresent());
      verify(companyRepository).findById(id);
      verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe propagar excepción cuando falla el guardado del repositorio")
    void shouldPropagateExceptionWhenSaveFails() {
      Long id = 2L;
      Company existing = new Company();
      existing.setId(id);
      existing.setCompanyName("Old");

      Company updated = new Company();
      updated.setCompanyName("New");

      when(companyRepository.findById(id)).thenReturn(Optional.of(existing));
      when(companyRepository.save(any(Company.class))).thenThrow(new RuntimeException("DB error"));

      assertThrows(RuntimeException.class, () -> companyService.update(id, updated));
      verify(companyRepository).findById(id);
      verify(companyRepository).save(existing);
    }
  }
}
