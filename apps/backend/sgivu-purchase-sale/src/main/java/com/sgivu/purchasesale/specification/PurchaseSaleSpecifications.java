package com.sgivu.purchasesale.specification;

import com.sgivu.purchasesale.dto.PurchaseSaleFilterCriteria;
import com.sgivu.purchasesale.entity.PurchaseSale;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PurchaseSaleSpecifications {

  private PurchaseSaleSpecifications() {}

  public static Specification<PurchaseSale> withFilters(PurchaseSaleFilterCriteria criteria) {
    return (root, query, cb) -> {
      if (criteria == null) {
        return cb.conjunction();
      }

      List<Predicate> predicates = new ArrayList<>();

      equals(predicates, cb, root.get("clientId"), criteria.getClientId());
      equals(predicates, cb, root.get("userId"), criteria.getUserId());
      equals(predicates, cb, root.get("vehicleId"), criteria.getVehicleId());
      equals(predicates, cb, root.get("contractType"), criteria.getContractType());
      equals(predicates, cb, root.get("contractStatus"), criteria.getContractStatus());
      equals(predicates, cb, root.get("paymentMethod"), criteria.getPaymentMethod());

      betweenDates(
          predicates, cb, root.get("updatedAt"), criteria.getStartDate(), criteria.getEndDate());

      range(
          predicates,
          cb,
          root.get("purchasePrice"),
          criteria.getMinPurchasePrice(),
          criteria.getMaxPurchasePrice());
      range(
          predicates,
          cb,
          root.get("salePrice"),
          criteria.getMinSalePrice(),
          criteria.getMaxSalePrice());

      if (StringUtils.hasText(criteria.getTerm())) {
        predicates.add(buildSearchPredicate(criteria.getTerm(), root, cb));
      }

      if (predicates.isEmpty()) {
        return cb.conjunction();
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private static Predicate buildSearchPredicate(
      String term, jakarta.persistence.criteria.Root<PurchaseSale> root, CriteriaBuilder cb) {
    String normalizedTerm = term.trim().toLowerCase();
    String likePattern = "%" + normalizedTerm + "%";
    List<Predicate> orPredicates = new ArrayList<>();
    orPredicates.add(cb.like(cb.lower(root.get("paymentTerms")), likePattern));
    orPredicates.add(cb.like(cb.lower(root.get("paymentLimitations")), likePattern));
    orPredicates.add(cb.like(cb.lower(root.get("observations")), likePattern));
    orPredicates.add(cb.like(cb.lower(root.get("contractStatus").as(String.class)), likePattern));
    orPredicates.add(cb.like(cb.lower(root.get("contractType").as(String.class)), likePattern));
    orPredicates.add(cb.like(cb.lower(root.get("paymentMethod").as(String.class)), likePattern));

    Long numericTerm = parseLongLoose(term);
    if (numericTerm != null) {
      orPredicates.add(cb.equal(root.get("id"), numericTerm));
      orPredicates.add(cb.equal(root.get("clientId"), numericTerm));
      orPredicates.add(cb.equal(root.get("userId"), numericTerm));
      orPredicates.add(cb.equal(root.get("vehicleId"), numericTerm));
    }

    Double priceMatch = parseDouble(term);
    if (priceMatch != null) {
      orPredicates.add(cb.equal(root.get("purchasePrice"), priceMatch));
      orPredicates.add(cb.equal(root.get("salePrice"), priceMatch));
    }

    return cb.or(orPredicates.toArray(new Predicate[0]));
  }

  private static void equals(
      List<Predicate> predicates, CriteriaBuilder cb, Path<?> path, Object value) {
    if (value != null) {
      predicates.add(cb.equal(path, value));
    }
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

  private static void betweenDates(
      List<Predicate> predicates,
      CriteriaBuilder cb,
      Path<LocalDateTime> path,
      LocalDate startDate,
      LocalDate endDate) {
    if (startDate != null) {
      predicates.add(cb.greaterThanOrEqualTo(path, startDate.atStartOfDay()));
    }
    if (endDate != null) {
      predicates.add(cb.lessThanOrEqualTo(path, endDate.atTime(LocalTime.MAX)));
    }
  }

  private static Long parseLongLoose(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String digitsOnly = value.replaceAll("\\D+", "");
    if (!StringUtils.hasText(digitsOnly)) {
      return null;
    }
    try {
      return Long.parseLong(digitsOnly);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static Double parseDouble(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return Double.parseDouble(value.replace(",", ".").trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
