package com.sgivu.vehicle.specification;

import com.sgivu.vehicle.dto.MotorcycleSearchCriteria;
import com.sgivu.vehicle.entity.Motorcycle;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class MotorcycleSpecifications {

  private MotorcycleSpecifications() {}

  public static Specification<Motorcycle> withFilters(MotorcycleSearchCriteria criteria) {
    // Evita nulls; si no hay filtros, devuelve conjunción para no restringir resultados
    return (root, query, cb) -> {
      if (criteria == null) {
        return cb.conjunction();
      }

      List<Predicate> predicates = new ArrayList<>();

      like(predicates, cb, root.get("plate"), criteria.getPlate());
      like(predicates, cb, root.get("brand"), criteria.getBrand());
      like(predicates, cb, root.get("line"), criteria.getLine());
      like(predicates, cb, root.get("model"), criteria.getModel());
      like(predicates, cb, root.get("motorcycleType"), criteria.getMotorcycleType());
      like(predicates, cb, root.get("transmission"), criteria.getTransmission());
      like(predicates, cb, root.get("cityRegistered"), criteria.getCityRegistered());

      range(predicates, cb, root.get("year"), criteria.getMinYear(), criteria.getMaxYear());
      range(
          predicates,
          cb,
          root.get("capacity"),
          criteria.getMinCapacity(),
          criteria.getMaxCapacity());
      range(
          predicates, cb, root.get("mileage"), criteria.getMinMileage(), criteria.getMaxMileage());
      range(
          predicates,
          cb,
          root.get("salePrice"),
          criteria.getMinSalePrice(),
          criteria.getMaxSalePrice());

      if (criteria.getStatus() != null) {
        predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
      }

      if (predicates.isEmpty()) {
        return cb.conjunction();
      }

      if (query != null) {
        query.distinct(true);
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private static void like(
      List<Predicate> predicates, CriteriaBuilder cb, Path<String> path, String value) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    predicates.add(cb.like(cb.lower(path), "%" + value.trim().toLowerCase() + "%"));
  }

  private static <N extends Number & Comparable<N>> void range(
      List<Predicate> predicates, CriteriaBuilder cb, Path<N> path, N min, N max) {
    if (min != null) {
      predicates.add(cb.greaterThanOrEqualTo(path, min));
    }
    if (max != null) {
      predicates.add(cb.lessThanOrEqualTo(path, max));
    }
  }
}
