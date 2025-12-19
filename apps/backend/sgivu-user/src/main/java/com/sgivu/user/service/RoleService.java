package com.sgivu.user.service;

import com.sgivu.user.entity.Role;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Gestiona el catálogo de roles corporativos compartido por los microservicios SGIVU.
 *
 * <p>Los roles gobiernan el acceso a operaciones de compra/venta, contratos y predicción; por ello
 * las modificaciones deben ser explícitas y controladas.
 */
public interface RoleService {

  /**
   * Agrega permisos a un rol existente respetando la taxonomía de seguridad.
   *
   * @param id identificador del rol objetivo.
   * @param permissionsName nombres de permisos a vincular.
   * @return rol actualizado si existe.
   */
  Optional<Role> addPermissions(Long id, Set<String> permissionsName);

  /**
   * Quita permisos de un rol cuando se restringen capacidades (p. ej. bloquear edición de contratos
   * a un rol de ventas).
   *
   * @param id identificador del rol.
   * @param permissionsName permisos a retirar.
   * @return rol actualizado si existe.
   */
  Optional<Role> removePermissions(Long id, Set<String> permissionsName);

  /**
   * Reemplaza completamente el set de permisos de un rol.
   *
   * @param id identificador.
   * @param permissionsName permisos definitivos.
   * @return rol persistido si existe.
   */
  Optional<Role> updatePermissions(Long id, Set<String> permissionsName);

  /**
   * Consulta por id para auditorías o edición puntual.
   *
   * @param id identificador.
   * @return rol si existe.
   */
  Optional<Role> findById(Long id);

  /**
   * Consulta por nombre, utilizado durante autenticación para poblar GrantedAuthorities.
   *
   * @param name nombre único.
   * @return rol si existe.
   */
  Optional<Role> findByName(String name);

  /** @return todos los roles disponibles para mostrarlos en el panel de administración. */
  List<Role> findAll();
}
