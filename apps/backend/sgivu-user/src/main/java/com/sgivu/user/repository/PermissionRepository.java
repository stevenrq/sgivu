package com.sgivu.user.repository;

import com.sgivu.user.entity.Permission;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio del catálogo de permisos granulares.
 *
 * <p>Permite resolver permisos por nombre para construir matrices rol-permiso sin escribir
 * consultas manuales.
 */
public interface PermissionRepository extends JpaRepository<Permission, Long> {

  /**
   * Busca un permiso por su nombre único.
   *
   * @param name nombre de permiso (ej. {@code user:update}).
   * @return permiso si existe, vacío en caso contrario.
   */
  Optional<Permission> findByName(String name);

  /**
   * Obtiene un conjunto de permisos existentes a partir de sus nombres.
   *
   * @param names colección de nombres solicitados.
   * @return permisos encontrados; vacío si ninguno coincide.
   */
  Optional<Set<Permission>> findByNameIn(Set<String> names);
}
