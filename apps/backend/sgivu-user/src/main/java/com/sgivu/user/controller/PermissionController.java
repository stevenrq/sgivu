package com.sgivu.user.controller;

import com.sgivu.user.entity.Permission;
import com.sgivu.user.service.PermissionService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de solo lectura para el catálogo de permisos.
 *
 * <p>Permite a los clientes obtener la lista de permisos disponibles al configurar roles para
 * flujos de ventas e inventario.
 */
@RestController
@RequestMapping("/v1/permissions")
public class PermissionController {

  private final PermissionService permissionService;

  public PermissionController(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  /**
   * Devuelve el catálogo completo de permisos granulares.
   *
   * @return lista de permisos para construir matrices de autorización.
   */
  @GetMapping
  @PreAuthorize("hasAuthority('user:read')")
  public ResponseEntity<List<Permission>> getAllPermissions() {
    return ResponseEntity.ok(permissionService.findAll());
  }
}
