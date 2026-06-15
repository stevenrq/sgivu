package com.sgivu.client.repository;

import com.sgivu.client.entity.Company;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends ClientRepository<Company> {

  Optional<Company> findByTaxId(String taxId);

  Optional<Company> findByCompanyName(String companyName);

  List<Company> findByCompanyNameContainingIgnoreCase(String companyName);
}
