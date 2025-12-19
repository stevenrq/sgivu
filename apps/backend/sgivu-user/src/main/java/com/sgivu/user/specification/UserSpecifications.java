package com.sgivu.user.specification;

import com.sgivu.user.dto.UserFilterCriteria;
import com.sgivu.user.entity.Role;
import com.sgivu.user.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Utilidades para construir {@link Specification} dinámicas sobre {@link User} usando los filtros
 * definidos en {@link UserFilterCriteria}.
 */
public final class UserSpecifications {

  private UserSpecifications() {}

  /**
   * Construye especificaciones dinámicas según los criterios recibidos desde la API.
   *
   * @param criteria filtros opcionales; si es nulo se devuelve la consulta sin restricciones.
   * @return specification combinada con lógica AND.
   * @apiNote Se usa para búsquedas administrativas y sincronización con el Gateway sin escribir
   *     consultas manuales.
   */
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
              (root, query, cb) -> cb.equal(root.get("isEnabled"), criteria.getEnabled()));
    }

    if (StringUtils.hasText(criteria.getRole())) {
      specification = specification.and(roleEquals(criteria.getRole().trim()));
    }

    return specification;
  }

  /**
   * Genera una condición OR que compara nombre y apellido con coincidencia parcial.
   *
   * @param value fragmento de texto a buscar.
   * @return specification que aplica {@code LIKE} sobre ambos campos en minúsculas.
   */
  private static Specification<User> nameContains(String value) {
    final String normalized = like(value.toLowerCase());
    return (root, query, cb) ->
        cb.or(
            cb.like(cb.lower(root.get("firstName")), normalized),
            cb.like(cb.lower(root.get("lastName")), normalized));
  }

  /**
   * Aplica filtro por rol, usando join para evitar N+1 y marcando la consulta como {@code distinct}
   * para no duplicar usuarios con múltiples roles.
   *
   * @param roleName nombre del rol buscado.
   * @return specification que filtra por el campo {@code name} de {@link Role}.
   */
  private static Specification<User> roleEquals(String roleName) {
    final String normalized = roleName.toLowerCase();
    return (root, query, cb) -> {
      if (query != null) {
        // Distinct evita multiplicar resultados cuando un usuario tiene más de un rol.
        query.distinct(true);
      }
      Join<User, Role> join = root.join("roles", JoinType.LEFT);
      return cb.equal(cb.lower(join.get("name")), normalized);
    };
  }

  /**
   * Normaliza el patrón para búsquedas {@code LIKE} envolviendo el valor con comodines.
   *
   * @param value cadena a envolver.
   * @return patrón con comodines a izquierda y derecha.
   */
  private static String like(String value) {
    return "%" + value + "%";
  }
}
