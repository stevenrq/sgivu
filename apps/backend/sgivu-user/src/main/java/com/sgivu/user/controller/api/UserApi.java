package com.sgivu.user.controller.api;

import com.sgivu.user.dto.UserResponse;
import com.sgivu.user.dto.UserUpdateRequest;
import com.sgivu.user.entity.User;
import com.sgivu.user.validation.ValidationGroups;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Usuarios", description = "Operaciones CRUD y búsqueda de usuarios del sistema")
@RequestMapping("/v1/users")
public interface UserApi {

  // --------------------------------------------------
  // CRUD Básico
  // --------------------------------------------------

  @Operation(
      summary = "Alta de usuario",
      description =
          "Registra un nuevo usuario en el sistema con validaciones de dominio "
              + "(documento, teléfono, roles). El password se cifra antes de persistir.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Usuario creado exitosamente",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de entrada inválidos",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos suficientes (requiere user:create)",
            content = @Content)
      })
  @PostMapping
  ResponseEntity<com.sgivu.user.dto.ApiResponse<UserResponse>> create(
      @Parameter(description = "Datos del usuario a registrar", required = true)
          @Validated(ValidationGroups.Create.class)
          @RequestBody
          User user,
      BindingResult bindingResult);

  @Operation(
      summary = "Consultar usuario por ID",
      description =
          "Retorna el perfil completo de un usuario para pantallas de detalle y auditoría.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Usuario encontrado",
            content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content)
      })
  @GetMapping("/{id}")
  ResponseEntity<UserResponse> getById(
      @Parameter(description = "ID del usuario", required = true, example = "1") @PathVariable
          Long id);

  @Operation(
      summary = "Listar todos los usuarios",
      description =
          "Retorna la lista completa de usuarios sin paginación para exportaciones pequeñas.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content)
      })
  @GetMapping
  ResponseEntity<List<UserResponse>> getAll();

  @Operation(
      summary = "Listar usuarios paginados",
      description = "Retorna usuarios paginados con tamaño fijo de 10 elementos por página.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Página de usuarios obtenida exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content)
      })
  @GetMapping("/page/{page}")
  ResponseEntity<Page<UserResponse>> getAllPaginated(
      @Parameter(description = "Número de página (base 0)", required = true, example = "0")
          @PathVariable
          Integer page);

  @Operation(
      summary = "Actualizar usuario",
      description =
          "Actualiza el perfil de un usuario existente. Permite auto-edición del usuario "
              + "o modificación por un administrador con permiso user:update.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Usuario actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de entrada inválidos",
            content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos suficientes",
            content = @Content)
      })
  @PutMapping("/{id}")
  ResponseEntity<com.sgivu.user.dto.ApiResponse<UserResponse>> update(
      @Parameter(description = "ID del usuario a actualizar", required = true, example = "1")
          @PathVariable
          Long id,
      @Parameter(description = "ID del usuario autenticado (inyectado por Gateway)", hidden = true)
          @RequestHeader("X-User-ID")
          String authenticatedUserId,
      @Parameter(description = "Datos actualizados del usuario", required = true)
          @Validated(ValidationGroups.Update.class)
          @RequestBody
          UserUpdateRequest userUpdateRequest,
      BindingResult bindingResult);

  @Operation(
      summary = "Eliminar usuario",
      description = "Elimina un usuario definitivamente del sistema.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Usuario eliminado exitosamente"),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos suficientes (requiere user:delete)",
            content = @Content)
      })
  @DeleteMapping("/{id}")
  ResponseEntity<Void> deleteById(
      @Parameter(description = "ID del usuario a eliminar", required = true, example = "1")
          @PathVariable
          Long id);

  // --------------------------------------------------
  // Operaciones de Estado
  // --------------------------------------------------

  @Operation(
      summary = "Cambiar estado de usuario",
      description =
          "Habilita o deshabilita un usuario para bloquear el acceso a contratos "
              + "y operaciones transaccionales.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente"),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content)
      })
  @PatchMapping("/{id}/status")
  ResponseEntity<Map<String, Boolean>> updateStatus(
      @Parameter(description = "ID del usuario", required = true, example = "1") @PathVariable
          Long id,
      @Parameter(
              description = "Nuevo estado (true=habilitado, false=deshabilitado)",
              required = true)
          @RequestBody
          boolean enabled);

  @Operation(
      summary = "Obtener conteo de usuarios",
      description =
          "Retorna estadísticas rápidas: total, activos e inactivos para tableros operativos.")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Conteo obtenido exitosamente")})
  @GetMapping("/count")
  ResponseEntity<Map<String, Long>> getUserCounts();

  // --------------------------------------------------
  // Búsqueda
  // --------------------------------------------------

  @Operation(
      summary = "Buscar usuarios",
      description =
          "Búsqueda multi-criterio no paginada para autocompletar o exportaciones. "
              + "Todos los filtros son opcionales y se aplican con lógica AND.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Búsqueda ejecutada exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content)
      })
  @GetMapping("/search")
  ResponseEntity<List<UserResponse>> searchUsers(
      @Parameter(description = "Filtrar por nombre o apellido parcial", example = "Juan")
          @RequestParam(required = false)
          String name,
      @Parameter(description = "Filtrar por username exacto/parcial", example = "jperez")
          @RequestParam(required = false)
          String username,
      @Parameter(description = "Filtrar por email", example = "juan@ejemplo.com")
          @RequestParam(required = false)
          String email,
      @Parameter(description = "Filtrar por rol", example = "ADMIN") @RequestParam(required = false)
          String role,
      @Parameter(description = "Filtrar por estado (true=habilitado)", example = "true")
          @RequestParam(required = false)
          Boolean enabled);

  @Operation(
      summary = "Buscar usuarios paginados",
      description =
          "Variante paginada de la búsqueda multi-criterio para listados en backoffice "
              + "con filtros combinados.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Búsqueda paginada ejecutada exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content)
      })
  @GetMapping("/search/page/{page}")
  ResponseEntity<Page<UserResponse>> searchUsersPaginated(
      @Parameter(description = "Número de página (base 0)", required = true, example = "0")
          @PathVariable
          Integer page,
      @Parameter(description = "Tamaño de página", example = "10")
          @RequestParam(defaultValue = "10")
          Integer size,
      @Parameter(description = "Filtrar por nombre o apellido parcial", example = "Juan")
          @RequestParam(required = false)
          String name,
      @Parameter(description = "Filtrar por username exacto/parcial", example = "jperez")
          @RequestParam(required = false)
          String username,
      @Parameter(description = "Filtrar por email", example = "juan@ejemplo.com")
          @RequestParam(required = false)
          String email,
      @Parameter(description = "Filtrar por rol", example = "ADMIN") @RequestParam(required = false)
          String role,
      @Parameter(description = "Filtrar por estado (true=habilitado)", example = "true")
          @RequestParam(required = false)
          Boolean enabled);

  // --------------------------------------------------
  // Endpoints Internos (ocultos en Swagger UI)
  // --------------------------------------------------

  @Hidden
  @Operation(
      summary = "Obtener usuario por username (interno)",
      description =
          "Endpoint interno para que el Authorization Server obtenga el perfil (roles/permisos) al"
              + " emitir JWT. Protegido para solo invocaciones desde sgivu-auth.")
  @GetMapping("/username/{username}")
  ResponseEntity<UserResponse> getByUsername(@PathVariable String username);
}
