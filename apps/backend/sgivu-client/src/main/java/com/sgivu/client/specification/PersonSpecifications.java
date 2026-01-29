package com.sgivu.client.specification;

import com.sgivu.client.dto.PersonSearchCriteria;
import com.sgivu.client.entity.Address;
import com.sgivu.client.entity.Person;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PersonSpecifications {

  private PersonSpecifications() {}

  public static Specification<Person> withFilters(PersonSearchCriteria criteria) {
    Specification<Person> specification = Specification.unrestricted();

    if (criteria == null) {
      return specification;
    }

    if (StringUtils.hasText(criteria.getName())) {
      final String normalized = like(criteria.getName().trim().toLowerCase());
      specification =
          specification.and(
              (root, query, cb) ->
                  cb.or(
                      cb.like(cb.lower(root.get("firstName")), normalized),
                      cb.like(cb.lower(root.get("lastName")), normalized)));
    }

    if (criteria.getNationalId() != null) {
      specification =
          specification.and(
              (root, query, cb) -> cb.equal(root.get("nationalId"), criteria.getNationalId()));
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
                Join<Person, Address> address = root.join("address", JoinType.LEFT);
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
