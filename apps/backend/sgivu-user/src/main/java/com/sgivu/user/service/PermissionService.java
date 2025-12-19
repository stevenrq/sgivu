package com.sgivu.user.service;

import com.sgivu.user.entity.Permission;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Consulta el catálogo de permisos finos usados para autorizar operaciones SGIVU (contratos,
 * inventario, compras/ventas).
 */
public interface PermissionService {

  /**
   * Busca un permiso por id para ediciones puntuales o auditoría.
   *
   * @param id identificador.
   * @return permiso si existe.
   */
  Optional<Permission> findById(Long id);

  /**
   * Obtiene un permiso por nombre, usado al construir {@code GrantedAuthorities} en autenticación.
   *
   * @param name nombre único.
   * @return permiso si existe.
   */
  Optional<Permission> findByName(String name);

  /** @return lista completa de permisos disponibles. */
  List<Permission> findAll();

  /**
   * Recupera un conjunto de permisos existentes por nombre.
   *
   * @param names colección de nombres solicitados desde el cliente.
   * @return permisos encontrados; vacío si ninguno existe.
   */
  Optional<Set<Permission>> findByNameIn(Set<String> names);
}
