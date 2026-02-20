package com.sgivu.user.service;

import com.sgivu.user.entity.Role;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoleService {

  Optional<Role> addPermissions(Long id, Set<String> permissionsName);

  Optional<Role> removePermissions(Long id, Set<String> permissionsName);

  Optional<Role> updatePermissions(Long id, Set<String> permissionsName);

  Optional<Role> findById(Long id);

  Optional<Role> findByName(String name);

  List<Role> findAll();
}
