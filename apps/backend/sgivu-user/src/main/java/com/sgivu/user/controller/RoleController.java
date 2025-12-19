package com.sgivu.user.controller;

import com.sgivu.user.entity.Role;
import com.sgivu.user.service.RoleService;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * API para administrar roles y su set de permisos.
 *
 * <p>Se usa desde backoffice para mantener alineado el modelo de autorización con las necesidades
 * de ventas, contratos y predicción de demanda.
 */
@RestController
@RequestMapping("/v1/roles")
public class RoleController {

  private final RoleService roleService;

  public RoleController(RoleService roleService) {
    this.roleService = roleService;
  }

  /**
   * Agrega permisos al rol indicado sin sobreescribir los existentes.
   *
   * @param id identificador del rol.
   * @param names permisos a añadir.
   * @return rol con permisos actualizados.
   */
  @PostMapping("/{id}/add-permissions")
  @PreAuthorize("hasAnyAuthority('user:create', 'user:update')")
  public ResponseEntity<Role> addPermissions(
      @PathVariable Long id, @RequestBody Set<String> names) {
    return ResponseEntity.ok(roleService.addPermissions(id, names).orElseThrow());
  }

  /**
   * Lista todos los roles definidos en el sistema para configuraciones y despliegue de catálogos.
   *
   * @return roles disponibles.
   */
  @GetMapping
  @PreAuthorize("hasAuthority('user:read')")
  public ResponseEntity<List<Role>> findAll() {
    return ResponseEntity.ok(roleService.findAll());
  }

  /**
   * Reemplaza completamente los permisos de un rol (operación destructiva controlada).
   *
   * @param id identificador del rol.
   * @param names permisos definitivos.
   * @return rol guardado.
   */
  @PutMapping("/{id}/permissions")
  @PreAuthorize("hasAuthority('user:update')")
  public ResponseEntity<Role> updatePermissions(
      @PathVariable Long id, @RequestBody Set<String> names) {
    return ResponseEntity.ok(roleService.updatePermissions(id, names).orElseThrow());
  }

  /**
   * Elimina permisos específicos de un rol.
   *
   * @param id identificador del rol.
   * @param names permisos a remover.
   * @return rol resultante.
   */
  @DeleteMapping("/{id}/remove-permissions")
  @PreAuthorize("hasAuthority('user:delete')")
  public ResponseEntity<Role> removePermissions(
      @PathVariable Long id, @RequestBody Set<String> names) {
    return ResponseEntity.ok(roleService.removePermissions(id, names).orElseThrow());
  }
}
