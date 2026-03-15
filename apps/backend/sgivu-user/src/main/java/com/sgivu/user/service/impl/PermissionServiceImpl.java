package com.sgivu.user.service.impl;

import com.sgivu.user.entity.Permission;
import com.sgivu.user.repository.PermissionRepository;
import com.sgivu.user.service.PermissionService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionServiceImpl implements PermissionService {

  private final PermissionRepository permissionRepository;

  public PermissionServiceImpl(PermissionRepository permissionRepository) {
    this.permissionRepository = permissionRepository;
  }

  @Transactional(readOnly = true)
  @Override
  public Optional<Permission> findById(Long id) {
    return permissionRepository.findById(id);
  }

  @Transactional(readOnly = true)
  @Override
  public Optional<Permission> findByName(String name) {
    return permissionRepository.findByName(name);
  }

  @Transactional(readOnly = true)
  @Override
  public List<Permission> findAll() {
    return permissionRepository.findAll();
  }

  @Transactional(readOnly = true)
  @Override
  public Optional<Set<Permission>> findByNameIn(Set<String> permissionNames) {
    return permissionRepository.findByNameIn(permissionNames);
  }
}
