package com.sgivu.user.service.impl;

import com.sgivu.user.entity.Permission;
import com.sgivu.user.repository.PermissionRepository;
import com.sgivu.user.service.PermissionService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementa consultas sobre el catálogo de permisos.
 *
 * <p>Optimiza lectura marcándolas como transaccionales de solo lectura para evitar bloqueos en
 * flujos de autenticación que resuelven permisos en caliente.
 */
@Service
public class PermissionServiceImpl implements PermissionService {

  private final PermissionRepository permissionRepository;

  public PermissionServiceImpl(PermissionRepository permissionRepository) {
    this.permissionRepository = permissionRepository;
  }

  /** {@inheritDoc} */
  @Transactional(readOnly = true)
  @Override
  public Optional<Permission> findById(Long id) {
    return permissionRepository.findById(id);
  }

  /** {@inheritDoc} */
  @Transactional(readOnly = true)
  @Override
  public Optional<Permission> findByName(String name) {
    return permissionRepository.findByName(name);
  }

  /** {@inheritDoc} */
  @Transactional(readOnly = true)
  @Override
  public List<Permission> findAll() {
    return permissionRepository.findAll();
  }

  /** {@inheritDoc} */
  @Transactional(readOnly = true)
  @Override
  public Optional<Set<Permission>> findByNameIn(Set<String> permissionNames) {
    return permissionRepository.findByNameIn(permissionNames);
  }
}
