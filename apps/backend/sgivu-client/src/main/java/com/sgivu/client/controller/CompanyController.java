package com.sgivu.client.controller;

import com.sgivu.client.controller.api.CompanyApi;
import com.sgivu.client.dto.CompanyResponse;
import com.sgivu.client.dto.CompanySearchCriteria;
import com.sgivu.client.entity.Company;
import com.sgivu.client.mapper.ClientMapper;
import com.sgivu.client.service.CompanyService;
import java.util.*;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
public class CompanyController implements CompanyApi {

  private final CompanyService companyService;
  private final ClientMapper clientMapper;

  public CompanyController(CompanyService companyService, ClientMapper clientMapper) {
    this.companyService = companyService;
    this.clientMapper = clientMapper;
  }

  @Override
  @PreAuthorize("hasAuthority('company:create')")
  public ResponseEntity<CompanyResponse> create(Company company, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().build();
    }
    Company savedCompany = companyService.save(company);
    CompanyResponse response = clientMapper.toCompanyResponse(savedCompany);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  @PreAuthorize("hasAuthority('company:read')")
  public ResponseEntity<CompanyResponse> getById(Long id) {
    return companyService
        .findById(id)
        .map(company -> ResponseEntity.ok(clientMapper.toCompanyResponse(company)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  @PreAuthorize("hasAuthority('company:read')")
  public ResponseEntity<List<CompanyResponse>> getAll() {
    List<CompanyResponse> responses =
        companyService.findAll().stream().map(clientMapper::toCompanyResponse).toList();
    return ResponseEntity.ok(responses);
  }

  @Override
  @PreAuthorize("hasAuthority('company:read')")
  public ResponseEntity<Page<CompanyResponse>> getAllPaginated(Integer page) {
    Page<Company> companyPage = companyService.findAll(PageRequest.of(page, 10));
    Page<CompanyResponse> responsePage = companyPage.map(clientMapper::toCompanyResponse);
    return ResponseEntity.ok(responsePage);
  }

  @Override
  @PreAuthorize("hasAuthority('company:update')")
  public ResponseEntity<CompanyResponse> update(
      Long id, Company company, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().build();
    }
    return companyService
        .update(id, company)
        .map(updatedCompany -> ResponseEntity.ok(clientMapper.toCompanyResponse(updatedCompany)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  @PreAuthorize("hasAuthority('company:delete')")
  public ResponseEntity<Void> deleteById(Long id) {
    Optional<Company> companyOptional = companyService.findById(id);

    if (companyOptional.isPresent()) {
      companyService.deleteById(id);
      return ResponseEntity.noContent().build();
    }

    return ResponseEntity.notFound().build();
  }

  @Override
  @PreAuthorize("hasAuthority('company:update')")
  public ResponseEntity<Map<String, Boolean>> changeStatus(Long id, boolean enabled) {
    boolean isUpdated = companyService.changeStatus(id, enabled);
    if (isUpdated) {
      return ResponseEntity.ok(Collections.singletonMap("status", enabled));
    }
    return ResponseEntity.notFound().build();
  }

  @Override
  @PreAuthorize("hasAuthority('company:read')")
  public ResponseEntity<Map<String, Long>> getCompanyCounts() {
    long totalCompanies = companyService.findAll().size();
    long activeCompanies = companyService.countByEnabled(true);
    long inactiveCompanies = totalCompanies - activeCompanies;

    Map<String, Long> counts = new HashMap<>(Map.of("totalCompanies", totalCompanies));
    counts.put("activeCompanies", activeCompanies);
    counts.put("inactiveCompanies", inactiveCompanies);

    return ResponseEntity.ok(counts);
  }

  @Override
  @PreAuthorize("hasAuthority('company:read')")
  public ResponseEntity<List<CompanyResponse>> searchCompanies(
      String taxId,
      String companyName,
      String email,
      Long phoneNumber,
      Boolean enabled,
      String city) {

    CompanySearchCriteria criteria =
        CompanySearchCriteria.builder()
            .taxId(trimToNull(taxId))
            .companyName(trimToNull(companyName))
            .email(trimToNull(email))
            .phoneNumber(phoneNumber)
            .enabled(enabled)
            .city(trimToNull(city))
            .build();

    List<CompanyResponse> companyResponses =
        companyService.search(criteria).stream().map(clientMapper::toCompanyResponse).toList();
    return ResponseEntity.ok(companyResponses);
  }

  @Override
  @PreAuthorize("hasAuthority('company:read')")
  public ResponseEntity<Page<CompanyResponse>> searchCompaniesPaginated(
      Integer page,
      Integer size,
      String taxId,
      String companyName,
      String email,
      Long phoneNumber,
      Boolean enabled,
      String city) {

    CompanySearchCriteria criteria =
        CompanySearchCriteria.builder()
            .taxId(trimToNull(taxId))
            .companyName(trimToNull(companyName))
            .email(trimToNull(email))
            .phoneNumber(phoneNumber)
            .enabled(enabled)
            .city(trimToNull(city))
            .build();

    Page<CompanyResponse> responsePage =
        companyService
            .search(criteria, PageRequest.of(page, size))
            .map(clientMapper::toCompanyResponse);
    return ResponseEntity.ok(responsePage);
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
