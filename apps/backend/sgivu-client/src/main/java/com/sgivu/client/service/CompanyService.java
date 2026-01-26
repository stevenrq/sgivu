package com.sgivu.client.service;

import com.sgivu.client.dto.CompanySearchCriteria;
import com.sgivu.client.entity.Company;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompanyService extends ClientService<Company> {

  Optional<Company> findByTaxId(String taxId);

  Optional<Company> findByCompanyName(String companyName);

  List<Company> findByCompanyNameContainingIgnoreCase(String companyName);

  List<Company> search(CompanySearchCriteria criteria);

  Page<Company> search(CompanySearchCriteria criteria, Pageable pageable);
}
