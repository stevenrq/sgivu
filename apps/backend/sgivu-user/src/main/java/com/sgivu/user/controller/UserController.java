package com.sgivu.user.controller;

import com.sgivu.user.dto.ApiResponse;
import com.sgivu.user.dto.UserFilterCriteria;
import com.sgivu.user.dto.UserResponse;
import com.sgivu.user.dto.UserUpdateRequest;
import com.sgivu.user.entity.User;
import com.sgivu.user.mapper.UserMapper;
import com.sgivu.user.service.UserService;
import com.sgivu.user.validation.ValidationGroups;
import com.sgivu.user.validation.ValidationService;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * API REST para administrar usuarios de SGIVU.
 *
 * <p>Expone operaciones CRUD, cambio de estado y búsqueda multi-criterio utilizadas por los
 * backoffice de ventas/contratos y por el Authorization Server para enriquecer tokens con roles y
 * permisos.
 */
@RefreshScope
@RestController
@RequestMapping("/v1/users")
public class UserController {

  private final UserService userService;
  private final ValidationService validationService;

  private final UserMapper userMapper;

  public UserController(
      UserService userService, ValidationService validationService, UserMapper userMapper) {
    this.userService = userService;
    this.validationService = validationService;
    this.userMapper = userMapper;
  }

  /**
   * Alta de usuario con validaciones de dominio (documento, teléfono, roles) y retorno de la vista
   * segura.
   *
   * @apiNote Usado por backoffice para habilitar actores de compras/ventas o analistas de
   *     predicción. El password se cifra en el servicio antes de persistir.
   */
  @PreAuthorize("hasAuthority('user:create')")
  @PostMapping
  public ResponseEntity<ApiResponse<UserResponse>> create(
      @Validated(ValidationGroups.Create.class) @RequestBody User user,
      BindingResult bindingResult) {

    ResponseEntity<ApiResponse<UserResponse>> validationResult =
        validationService.handleValidation(user, bindingResult);

    if (validationResult != null) return validationResult;

    User savedUser = userService.save(user);
    UserResponse userResponse = userMapper.toUserResponse(savedUser);
    ApiResponse<UserResponse> apiResponse = new ApiResponse<>(userResponse);

    return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
  }

  /**
   * Consulta puntual por identificador, usada para pantallas de detalle y auditoría.
   *
   * @param id identificador del usuario.
   * @return perfil si existe, 404 en caso contrario.
   */
  @PreAuthorize("hasAuthority('user:read')")
  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
    return userService
        .findById(id)
        .map(user -> ResponseEntity.ok(userMapper.toUserResponse(user)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Endpoint interno para que el Authorization Server obtenga el perfil (roles/permisos) al emitir
   * JWT.
   *
   * @apiNote Protegido en {@link com.sgivu.user.security.SecurityConfig} para permitir solo
   *     invocaciones desde {@code sgivu-auth}.
   */
  @GetMapping("/username/{username}")
  public ResponseEntity<UserResponse> getByUsername(@PathVariable String username) {
    return userService
        .findByUsername(username)
        .map(user -> ResponseEntity.ok(userMapper.toUserResponse(user)))
        .orElse(ResponseEntity.notFound().build());
  }

  /** Lista completa sin paginación para exportaciones pequeñas. */
  @PreAuthorize("hasAuthority('user:read')")
  @GetMapping
  public ResponseEntity<List<UserResponse>> getAll() {
    return ResponseEntity.ok(
        userService.findAll().stream().map(userMapper::toUserResponse).toList());
  }

  /**
   * Listado paginado de usuarios para vistas de backoffice.
   *
   * @param page índice de página base 0.
   * @return página con tamaño fijo de 10 elementos.
   */
  @PreAuthorize("hasAuthority('user:read')")
  @GetMapping("/page/{page}")
  public ResponseEntity<Page<UserResponse>> getAllPaginated(@PathVariable Integer page) {
    return ResponseEntity.ok(
        userService.findAll(PageRequest.of(page, 10)).map(userMapper::toUserResponse));
  }

  /**
   * Actualiza el perfil de un usuario existente.
   *
   * <p>Este endpoint maneja la lógica de autorización para dos escenarios principales:
   *
   * <ol>
   *   <li><b>Administrador:</b> Un usuario con la autoridad 'user:update' puede modificar el perfil
   *       de cualquier usuario.
   *   <li><b>Auto-edición de Usuario:</b> Un usuario regular puede modificar su propio perfil. Esta
   *       verificación se realiza comparando el ID del path con el ID del usuario autenticado, que
   *       es inyectado de forma segura por el API Gateway en el header 'X-User-ID'.
   * </ol>
   *
   * Primero valida los datos de entrada y, si son correctos, procede con la actualización a través
   * del {@link UserService}.
   *
   * @param id El identificador único del usuario a ser actualizado, extraído de la URL.
   * @param authenticatedUserId El ID del usuario que realiza la petición, extraído del header
   *     'X-User-ID'. Este header es añadido por el API Gateway y se utiliza para la verificación de
   *     auto-edición.
   * @param userUpdateRequest El DTO (Data Transfer Object) que contiene los nuevos datos del
   *     usuario.
   * @param bindingResult Objeto que contiene los resultados de la validación de Spring.
   * @return Un {@link ResponseEntity} con un {@link ApiResponse} que contiene el {@link
   *     UserResponse} actualizado si la operación es exitosa (200 OK), o un {@link ResponseEntity}
   *     con los errores de validación (400 Bad Request), o un {@link ResponseEntity} vacío si el
   *     usuario no es encontrado (404 Not Found).
   * @see UserService#update(Long, UserUpdateRequest)
   */
  @PreAuthorize("hasAuthority('user:update') or #id.toString() == #authenticatedUserId")
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<UserResponse>> update(
      @PathVariable Long id,
      @RequestHeader("X-User-ID") String authenticatedUserId,
      @Validated(ValidationGroups.Update.class) @RequestBody UserUpdateRequest userUpdateRequest,
      BindingResult bindingResult) {

    ResponseEntity<ApiResponse<UserResponse>> validationResult =
        validationService.handleValidation(userUpdateRequest, bindingResult);

    if (validationResult != null) return validationResult;

    return userService
        .update(id, userUpdateRequest)
        .map(
            user -> {
              UserResponse userResponse = userMapper.toUserResponse(user);
              ApiResponse<UserResponse> apiResponse = new ApiResponse<>(userResponse);
              return ResponseEntity.ok(apiResponse);
            })
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Elimina un usuario definitivamente.
   *
   * @param id identificador objetivo.
   * @return 204 si se eliminó, 404 si no existe.
   */
  @PreAuthorize("hasAuthority('user:delete')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteById(@PathVariable Long id) {
    Optional<User> userOptional = userService.findById(id);

    if (userOptional.isPresent()) {
      userService.deleteById(id);
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.notFound().build();
  }

  /**
   * Cambia el estado de un usuario para habilitar o bloquear el acceso a contratos y operaciones
   * transaccionales.
   *
   * @param id identificador del usuario.
   * @param isEnabled estado deseado.
   * @return mapa sencillo con el estado actualizado.
   */
  @PreAuthorize("hasAuthority('user:update')")
  @PatchMapping("/{id}/status")
  public ResponseEntity<Map<String, Boolean>> updateStatus(
      @PathVariable Long id, @RequestBody boolean isEnabled) {
    boolean isUpdated = userService.changeStatus(id, isEnabled);

    if (isUpdated) {
      return ResponseEntity.ok(Collections.singletonMap("User status: ", isEnabled));
    }
    return ResponseEntity.notFound().build();
  }

  /**
   * Estadísticas rápidas para tableros operativos: total, activos e inactivos.
   *
   * @return mapa con cantidades calculadas en memoria.
   */
  @GetMapping("/count")
  public ResponseEntity<Map<String, Long>> getUserCounts() {
    long totalUsers = userService.findAll().size();
    long activeUsers = userService.countActiveUsers();
    long inactiveUsers = totalUsers - activeUsers;

    Map<String, Long> counts = new HashMap<>();
    counts.put("totalUsers", totalUsers);
    counts.put("activeUsers", activeUsers);
    counts.put("inactiveUsers", inactiveUsers);

    return ResponseEntity.ok(counts);
  }

  /**
   * Búsqueda multi-criterio no paginada para autocompletar o exportaciones.
   *
   * @param name nombre o apellido parcial.
   * @param username username exacto/parcial.
   * @param email filtro por email.
   * @param role rol requerido.
   * @param enabled estado habilitado.
   * @return lista de coincidencias en orden natural.
   */
  @PreAuthorize("hasAuthority('user:read')")
  @GetMapping("/search")
  public ResponseEntity<List<UserResponse>> searchUsers(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String role,
      @RequestParam(required = false) Boolean enabled) {

    UserFilterCriteria criteria =
        UserFilterCriteria.builder()
            .name(name)
            .username(username)
            .email(email)
            .role(role)
            .enabled(enabled)
            .build();

    List<User> users = userService.search(criteria);
    return ResponseEntity.ok(users.stream().map(userMapper::toUserResponse).toList());
  }

  /**
   * Variante paginada para listados en backoffice con filtros combinados.
   *
   * @param page número de página base 0.
   * @param size tamaño de página.
   * @param name filtro por nombre/apellido.
   * @param username filtro por username.
   * @param email filtro por email.
   * @param role filtro por rol.
   * @param enabled estado habilitado.
   * @return página de usuarios ya mapeados a DTO.
   */
  @PreAuthorize("hasAuthority('user:read')")
  @GetMapping("/search/page/{page}")
  public ResponseEntity<Page<UserResponse>> searchUsersPaginated(
      @PathVariable Integer page,
      @RequestParam(defaultValue = "10") Integer size,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String role,
      @RequestParam(required = false) Boolean enabled) {

    UserFilterCriteria criteria =
        UserFilterCriteria.builder()
            .name(name)
            .username(username)
            .email(email)
            .role(role)
            .enabled(enabled)
            .build();

    Page<UserResponse> responsePage =
        userService
            .search(criteria, PageRequest.of(page, size))
            .map(userMapper::toUserResponse);
    return ResponseEntity.ok(responsePage);
  }
}
