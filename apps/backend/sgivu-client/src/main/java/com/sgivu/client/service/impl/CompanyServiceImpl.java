package com.sgivu.client.service.impl;

import com.sgivu.client.dto.CompanySearchCriteria;
import com.sgivu.client.entity.Company;
import com.sgivu.client.repository.CompanyRepository;
import com.sgivu.client.service.CompanyService;
import com.sgivu.client.specification.CompanySpecifications;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanyServiceImpl extends AbstractClientServiceImpl<Company, CompanyRepository>
    implements CompanyService {

  private final CompanyRepository companyRepository;

  public CompanyServiceImpl(CompanyRepository companyRepository) {
    super(companyRepository);
    this.companyRepository = companyRepository;
  }

  @Override
  public Optional<Company> findByTaxId(String taxId) {
    return companyRepository.findByTaxId(taxId);
  }

  @Override
  public Optional<Company> findByCompanyName(String companyName) {
    return companyRepository.findByCompanyName(companyName);
  }

  @Override
  public List<Company> findByCompanyNameContainingIgnoreCase(String companyName) {
    return companyRepository.findByCompanyNameContainingIgnoreCase(companyName);
  }

  @Override
  public List<Company> search(CompanySearchCriteria criteria) {
    return search(criteria, Pageable.unpaged()).getContent();
  }

  @Override
  public Page<Company> search(CompanySearchCriteria criteria, Pageable pageable) {
    return companyRepository.findAll(CompanySpecifications.withFilters(criteria), pageable);
  }

  @Override
  public Optional<Company> update(Long id, Company client) {
    return companyRepository
        .findById(id)
        .map(
            existing -> {
              existing.setCompanyName(client.getCompanyName());
              return companyRepository.save(existing);
            });
  }
}
