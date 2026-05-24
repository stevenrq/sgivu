package com.sgivu.user.specification;

import com.sgivu.user.dto.UserFilterCriteria;
import com.sgivu.user.entity.Role;
import com.sgivu.user.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class UserSpecifications {

  private UserSpecifications() {}

  public static Specification<User> withFilters(UserFilterCriteria criteria) {
    Specification<User> specification = Specification.unrestricted();

    if (criteria == null) {
      return specification;
    }

    if (StringUtils.hasText(criteria.getName())) {
      specification = specification.and(nameContains(criteria.getName().trim()));
    }

    if (StringUtils.hasText(criteria.getUsername())) {
      specification =
          specification.and(
              (root, query, cb) ->
                  cb.like(
                      cb.lower(root.get("username")),
                      like(criteria.getUsername().trim().toLowerCase())));
    }

    if (StringUtils.hasText(criteria.getEmail())) {
      specification =
          specification.and(
              (root, query, cb) ->
                  cb.like(
                      cb.lower(root.get("email")), like(criteria.getEmail().trim().toLowerCase())));
    }

    if (criteria.getEnabled() != null) {
      specification =
          specification.and(
              (root, query, cb) -> cb.equal(root.get("enabled"), criteria.getEnabled()));
    }

    if (StringUtils.hasText(criteria.getRole())) {
      specification = specification.and(roleEquals(criteria.getRole().trim()));
    }

    return specification;
  }

  private static Specification<User> nameContains(String value) {
    final String normalized = like(value.toLowerCase());
    return (root, query, cb) ->
        cb.or(
            cb.like(cb.lower(root.get("firstName")), normalized),
            cb.like(cb.lower(root.get("lastName")), normalized));
  }

  private static Specification<User> roleEquals(String roleName) {
    final String normalized = roleName.toLowerCase();
    return (root, query, cb) -> {
      if (query != null) {
        query.distinct(true);
      }
      Join<User, Role> join = root.join("roles", JoinType.LEFT);
      return cb.equal(cb.lower(join.get("name")), normalized);
    };
  }

  private static String like(String value) {
    return "%" + value + "%";
  }
}
