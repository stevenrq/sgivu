package com.sgivu.user.controller.api;

import com.sgivu.user.entity.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Roles", description = "Gestión de roles y asignación de permisos")
@RequestMapping("/v1/roles")
public interface RoleApi {

  @Operation(
      summary = "Agregar permisos a un rol",
      description =
          "Añade permisos al rol indicado sin sobreescribir los existentes. "
              + "Requiere autoridad user:create o user:update.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Permisos agregados exitosamente",
            content = @Content(schema = @Schema(implementation = Role.class))),
        @ApiResponse(responseCode = "404", description = "Rol no encontrado", content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos suficientes",
            content = @Content)
      })
  @PostMapping("/{id}/add-permissions")
  ResponseEntity<Role> addPermissions(
      @Parameter(description = "ID del rol", required = true, example = "1") @PathVariable Long id,
      @Parameter(
              description = "Nombres de los permisos a agregar",
              required = true,
              example = "[\"user:read\", \"user:create\"]")
          @RequestBody
          Set<String> names);

  @Operation(
      summary = "Listar todos los roles",
      description =
          "Retorna todos los roles definidos en el sistema para configuraciones "
              + "y despliegue de catálogos.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Lista de roles obtenida exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content)
      })
  @GetMapping
  ResponseEntity<List<Role>> findAll();

  @Operation(
      summary = "Reemplazar permisos de un rol",
      description =
          "Reemplaza completamente los permisos de un rol (operación destructiva controlada). "
              + "Requiere autoridad user:update.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Permisos actualizados exitosamente",
            content = @Content(schema = @Schema(implementation = Role.class))),
        @ApiResponse(responseCode = "404", description = "Rol no encontrado", content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos suficientes",
            content = @Content)
      })
  @PutMapping("/{id}/permissions")
  ResponseEntity<Role> updatePermissions(
      @Parameter(description = "ID del rol", required = true, example = "1") @PathVariable Long id,
      @Parameter(
              description = "Nombres de los permisos definitivos",
              required = true,
              example = "[\"user:read\"]")
          @RequestBody
          Set<String> names);

  @Operation(
      summary = "Eliminar permisos de un rol",
      description = "Remueve permisos específicos de un rol. Requiere autoridad user:delete.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Permisos eliminados exitosamente",
            content = @Content(schema = @Schema(implementation = Role.class))),
        @ApiResponse(responseCode = "404", description = "Rol no encontrado", content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos suficientes",
            content = @Content)
      })
  @DeleteMapping("/{id}/remove-permissions")
  ResponseEntity<Role> removePermissions(
      @Parameter(description = "ID del rol", required = true, example = "1") @PathVariable Long id,
      @Parameter(
              description = "Nombres de los permisos a remover",
              required = true,
              example = "[\"user:delete\"]")
          @RequestBody
          Set<String> names);
}
