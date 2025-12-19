package com.sgivu.user.service.impl;

import com.sgivu.user.entity.Permission;
import com.sgivu.user.entity.Role;
import com.sgivu.user.repository.RoleRepository;
import com.sgivu.user.service.PermissionService;
import com.sgivu.user.service.RoleService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de gestión de roles y sus permisos.
 *
 * <p>Se asegura de mantener la relación rol-permiso coherente con la tabla de permisos, permitiendo
 * que los cambios se propaguen a los tokens y al control de acceso en los flujos de ventas,
 * inventario y contratos.
 */
@Service
public class RoleServiceImpl implements RoleService {

  private final RoleRepository roleRepository;
  private final PermissionService permissionService;

  public RoleServiceImpl(RoleRepository roleRepository, PermissionService permissionService) {
    this.roleRepository = roleRepository;
    this.permissionService = permissionService;
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public Optional<Role> addPermissions(Long id, Set<String> names) {
    if (names == null || names.isEmpty()) {
      // Sin cambios solicitados, se devuelve el rol actual para evitar sobrescrituras vacías.
      return roleRepository.findById(id);
    }

    return roleRepository
        .findById(id)
        .map(
            role -> {
              Optional<Set<Permission>> permissionsFound = permissionService.findByNameIn(names);
              permissionsFound.ifPresent(role::addPermissions);
              return roleRepository.save(role);
            });
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public Optional<Role> removePermissions(Long id, Set<String> names) {
    if (names == null || names.isEmpty()) {
      // No hay permisos a eliminar, se devuelve el rol tal como está.
      return roleRepository.findById(id);
    }

    return roleRepository
        .findById(id)
        .map(
            role -> {
              Optional<Set<Permission>> permissionsToRemove = permissionService.findByNameIn(names);
              permissionsToRemove.ifPresent(
                  permissions -> role.getPermissions().removeAll(permissions));
              return roleRepository.save(role);
            });
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public Optional<Role> updatePermissions(Long id, Set<String> names) {
    return roleRepository
        .findById(id)
        .map(
            role -> {
              role.getPermissions().clear();
              if (names != null && !names.isEmpty()) {
                Optional<Set<Permission>> newPermissions = permissionService.findByNameIn(names);
                role.getPermissions().addAll(newPermissions.orElseThrow());
              }
              return roleRepository.save(role);
            });
  }

  /** {@inheritDoc} */
  @Transactional(readOnly = true)
  @Override
  public Optional<Role> findById(Long id) {
    return roleRepository.findById(id);
  }

  /** {@inheritDoc} */
  @Transactional(readOnly = true)
  @Override
  public Optional<Role> findByName(String name) {
    return roleRepository.findByName(name);
  }

  /** {@inheritDoc} */
  @Transactional(readOnly = true)
  @Override
  public List<Role> findAll() {
    return roleRepository.findAll();
  }
}
