package com.sgivu.client.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sgivu.client.dto.CompanySearchCriteria;
import com.sgivu.client.entity.Address;
import com.sgivu.client.entity.Company;
import com.sgivu.client.repository.CompanyRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

  @Mock private CompanyRepository companyRepository;

  private CompanyServiceImpl companyService;

  @BeforeEach
  void setUp() {
    companyService = new CompanyServiceImpl(companyRepository);
  }

  @Test
  void updateWhenCompanyExistsUpdatesNameOnly() {
    Address address = new Address();
    Company existing = sampleCompany();
    existing.setId(7L);
    existing.setCompanyName("Old Corp");
    existing.setTaxId("RUC-OLD");
    existing.setEmail("contact@old.com");
    existing.setPhoneNumber(98765L);
    existing.setAddress(address);

    Company incoming = new Company();
    incoming.setCompanyName("New Corp");
    incoming.setTaxId("RUC-NEW");
    incoming.setEmail("new@corp.com");
    incoming.setPhoneNumber(11111L);
    incoming.setAddress(new Address());

    when(companyRepository.findById(7L)).thenReturn(Optional.of(existing));
    when(companyRepository.save(any(Company.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<Company> result = companyService.update(7L, incoming);

    assertThat(result).isPresent();
    Company saved = result.get();
    assertThat(saved.getCompanyName()).isEqualTo("New Corp");
    assertThat(saved.getTaxId()).isEqualTo("RUC-OLD");
    assertThat(saved.getEmail()).isEqualTo("contact@old.com");
    assertThat(saved.getPhoneNumber()).isEqualTo(98765L);
    assertThat(saved.getAddress()).isSameAs(address);
    verify(companyRepository).save(existing);
  }

  @Test
  void updateWhenCompanyNotFoundReturnsEmpty() {
    when(companyRepository.findById(9L)).thenReturn(Optional.empty());

    Optional<Company> result = companyService.update(9L, sampleCompany());

    assertThat(result).isEmpty();
    verify(companyRepository, never()).save(any(Company.class));
  }

  @Test
  void searchWithoutPaginationUsesUnpagedSpecification() {
    CompanySearchCriteria criteria =
        CompanySearchCriteria.builder().companyName("cars").taxId("123").build();
    List<Company> companies = List.of(sampleCompany());
    when(companyRepository.findAll(
            ArgumentMatchers.<Specification<Company>>any(), eq(Pageable.unpaged())))
        .thenReturn(new PageImpl<>(companies));

    List<Company> result = companyService.search(criteria);

    assertThat(result).containsExactlyElementsOf(companies);
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(companyRepository)
        .findAll(ArgumentMatchers.<Specification<Company>>any(), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue()).isEqualTo(Pageable.unpaged());
  }

  @Test
  void searchWithPaginationDelegatesToRepository() {
    CompanySearchCriteria criteria = CompanySearchCriteria.builder().email("mail@test.com").build();
    Pageable pageable = PageRequest.of(0, 5);
    when(companyRepository.findAll(ArgumentMatchers.<Specification<Company>>any(), eq(pageable)))
        .thenReturn(Page.empty(pageable));

    Page<Company> page = companyService.search(criteria, pageable);

    assertThat(page.getContent()).isEmpty();
    verify(companyRepository).findAll(ArgumentMatchers.<Specification<Company>>any(), eq(pageable));
  }

  @Test
  void changeStatusUpdatesFlagWhenCompanyExists() {
    Company company = sampleCompany();
    company.setId(11L);
    company.setEnabled(false);
    when(companyRepository.findById(11L)).thenReturn(Optional.of(company));
    when(companyRepository.save(any(Company.class))).thenReturn(company);

    boolean updated = companyService.changeStatus(11L, true);

    assertThat(updated).isTrue();
    assertThat(company.isEnabled()).isTrue();
    verify(companyRepository).save(company);
  }

  @Test
  void findByTaxIdDelegatesToRepository() {
    Company company = sampleCompany();
    when(companyRepository.findByTaxId("ABC123")).thenReturn(Optional.of(company));

    Optional<Company> result = companyService.findByTaxId("ABC123");

    assertThat(result).contains(company);
  }

  @Test
  void findByCompanyNameDelegatesToRepository() {
    Company company = sampleCompany();
    when(companyRepository.findByCompanyName("Acme")).thenReturn(Optional.of(company));

    Optional<Company> result = companyService.findByCompanyName("Acme");

    assertThat(result).contains(company);
  }

  private Company sampleCompany() {
    Company company = new Company();
    company.setTaxId("RUC-001");
    company.setCompanyName("SGIVU Motors");
    company.setEmail("contact@sgivu.com");
    company.setPhoneNumber(999888L);
    company.setAddress(new Address());
    return company;
  }
}
