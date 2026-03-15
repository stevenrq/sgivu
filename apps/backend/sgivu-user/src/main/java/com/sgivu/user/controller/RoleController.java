package com.sgivu.user.controller;

import com.sgivu.user.controller.api.RoleApi;
import com.sgivu.user.entity.Role;
import com.sgivu.user.service.RoleService;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoleController implements RoleApi {

  private final RoleService roleService;

  public RoleController(RoleService roleService) {
    this.roleService = roleService;
  }

  @Override
  @PreAuthorize("hasAnyAuthority('user:create', 'user:update')")
  public ResponseEntity<Role> addPermissions(Long id, Set<String> names) {
    return ResponseEntity.ok(roleService.addPermissions(id, names).orElseThrow());
  }

  @Override
  @PreAuthorize("hasAuthority('user:read')")
  public ResponseEntity<List<Role>> findAll() {
    return ResponseEntity.ok(roleService.findAll());
  }

  @Override
  @PreAuthorize("hasAuthority('user:update')")
  public ResponseEntity<Role> updatePermissions(Long id, Set<String> names) {
    return ResponseEntity.ok(roleService.updatePermissions(id, names).orElseThrow());
  }

  @Override
  @PreAuthorize("hasAuthority('user:delete')")
  public ResponseEntity<Role> removePermissions(Long id, Set<String> names) {
    return ResponseEntity.ok(roleService.removePermissions(id, names).orElseThrow());
  }
}
