package com.sgivu.user.repository;

import com.sgivu.user.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio del catálogo de roles corporativos.
 *
 * <p>La búsqueda por nombre se utiliza al construir GrantedAuthorities desde JWT y durante
 * normalización de solicitudes de roles.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

  /**
   * Recupera un rol por su nombre único.
   *
   * @param name identificador lógico del rol (ej. {@code ADMIN}).
   * @return rol si existe, vacío en caso contrario.
   */
  Optional<Role> findByName(String name);
}
