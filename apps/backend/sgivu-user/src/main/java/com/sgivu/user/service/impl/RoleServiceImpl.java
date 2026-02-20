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

@Service
public class RoleServiceImpl implements RoleService {

  private final RoleRepository roleRepository;
  private final PermissionService permissionService;

  public RoleServiceImpl(RoleRepository roleRepository, PermissionService permissionService) {
    this.roleRepository = roleRepository;
    this.permissionService = permissionService;
  }

  @Override
  @Transactional
  public Optional<Role> addPermissions(Long id, Set<String> names) {
    if (names == null || names.isEmpty()) {
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

  @Override
  @Transactional
  public Optional<Role> removePermissions(Long id, Set<String> names) {
    if (names == null || names.isEmpty()) {
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

  @Transactional(readOnly = true)
  @Override
  public Optional<Role> findById(Long id) {
    return roleRepository.findById(id);
  }

  @Transactional(readOnly = true)
  @Override
  public Optional<Role> findByName(String name) {
    return roleRepository.findByName(name);
  }

  @Transactional(readOnly = true)
  @Override
  public List<Role> findAll() {
    return roleRepository.findAll();
  }
}
