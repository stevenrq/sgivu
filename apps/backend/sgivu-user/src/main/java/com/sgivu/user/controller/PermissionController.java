package com.sgivu.user.controller;

import com.sgivu.user.controller.api.PermissionApi;
import com.sgivu.user.entity.Permission;
import com.sgivu.user.service.PermissionService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PermissionController implements PermissionApi {

  private final PermissionService permissionService;

  public PermissionController(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  @Override
  @PreAuthorize("hasAuthority('user:read')")
  public ResponseEntity<List<Permission>> getAllPermissions() {
    return ResponseEntity.ok(permissionService.findAll());
  }
}
