package com.sgivu.vehicle.specification;

import com.sgivu.vehicle.dto.CarSearchCriteria;
import com.sgivu.vehicle.entity.Car;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Construye especificaciones dinámicas para filtrar autos según múltiples criterios.
 *
 * <p>Permite combinar filtros complejos en consultas JPA sin perder la capacidad de usar índices de
 * PostgreSQL, clave para los listados del inventario y las integraciones con analítica de demanda.
 */
public final class CarSpecifications {

  private CarSpecifications() {}

  /**
   * Genera un {@link Specification} a partir de los filtros recibidos.
   *
   * <p>Construye predicados solo cuando hay filtros presentes para evitar full scans innecesarios
   * en PostgreSQL.
   *
   * @param criteria filtros opcionales
   * @return especificación lista para el repositorio
   */
  public static Specification<Car> withFilters(CarSearchCriteria criteria) {
    return (root, query, cb) -> {
      if (criteria == null) {
        return cb.conjunction();
      }

      List<Predicate> predicates = new ArrayList<>();

      like(predicates, cb, root.get("plate"), criteria.getPlate());
      like(predicates, cb, root.get("brand"), criteria.getBrand());
      like(predicates, cb, root.get("line"), criteria.getLine());
      like(predicates, cb, root.get("model"), criteria.getModel());
      like(predicates, cb, root.get("fuelType"), criteria.getFuelType());
      like(predicates, cb, root.get("bodyType"), criteria.getBodyType());
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

  /**
   * Agrega un predicado LIKE, ignorando mayúsculas, cuando hay texto válido.
   *
   * @param predicates lista donde se acumulan filtros
   * @param cb builder de criterios
   * @param path atributo de la entidad a filtrar
   * @param value texto buscado
   */
  private static void like(
      List<Predicate> predicates, CriteriaBuilder cb, Path<String> path, String value) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    predicates.add(cb.like(cb.lower(path), "%" + value.trim().toLowerCase() + "%"));
  }

  /**
   * Agrega límites mínimos y máximos para atributos numéricos.
   *
   * @param predicates predicados acumulados
   * @param cb builder de criterios
   * @param path ruta al atributo numérico
   * @param min valor mínimo permitido
   * @param max valor máximo permitido
   * @param <N> tipo numérico comparable
   */
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
