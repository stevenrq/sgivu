package com.sgivu.client.specification;

import com.sgivu.client.dto.CompanySearchCriteria;
import com.sgivu.client.entity.Address;
import com.sgivu.client.entity.Company;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CompanySpecifications {

  private CompanySpecifications() {}

  public static Specification<Company> withFilters(CompanySearchCriteria criteria) {
    Specification<Company> specification = Specification.unrestricted();

    if (criteria == null) {
      return specification;
    }

    if (StringUtils.hasText(criteria.getCompanyName())) {
      specification =
          specification.and(
              (root, query, cb) ->
                  cb.like(
                      cb.lower(root.get("companyName")),
                      like(criteria.getCompanyName().trim().toLowerCase())));
    }

    if (StringUtils.hasText(criteria.getTaxId())) {
      specification =
          specification.and(
              (root, query, cb) ->
                  cb.like(
                      cb.lower(root.get("taxId")), like(criteria.getTaxId().trim().toLowerCase())));
    }

    if (StringUtils.hasText(criteria.getEmail())) {
      specification =
          specification.and(
              (root, query, cb) ->
                  cb.like(
                      cb.lower(root.get("email")), like(criteria.getEmail().trim().toLowerCase())));
    }

    if (criteria.getPhoneNumber() != null) {
      specification =
          specification.and(
              (root, query, cb) -> cb.equal(root.get("phoneNumber"), criteria.getPhoneNumber()));
    }

    if (criteria.getEnabled() != null) {
      specification =
          specification.and(
              (root, query, cb) -> cb.equal(root.get("enabled"), criteria.getEnabled()));
    }

    if (StringUtils.hasText(criteria.getCity())) {
      specification =
          specification.and(
              (root, query, cb) -> {
                Join<Company, Address> address = root.join("address", JoinType.LEFT);
                return cb.like(
                    cb.lower(address.get("city")), like(criteria.getCity().trim().toLowerCase()));
              });
    }

    return specification;
  }

  private static String like(String value) {
    return "%" + value + "%";
  }
}
