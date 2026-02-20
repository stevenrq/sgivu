package com.sgivu.user.service;

import com.sgivu.user.entity.Permission;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PermissionService {

  Optional<Permission> findById(Long id);

  Optional<Permission> findByName(String name);

  List<Permission> findAll();

  Optional<Set<Permission>> findByNameIn(Set<String> names);
}
