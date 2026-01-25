package com.sgivu.user.dto;

import com.sgivu.user.entity.Address;
import com.sgivu.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Representación pública de un usuario del sistema")
public class UserResponse {

  @Schema(description = "Identificador único del usuario", example = "1")
  private Long id;

  @Schema(description = "Número de documento de identidad nacional", example = "1234567890")
  private Long nationalId;

  @Schema(description = "Nombre del usuario", example = "Juan")
  private String firstName;

  @Schema(description = "Apellido del usuario", example = "Pérez")
  private String lastName;

  @Schema(description = "Dirección física del usuario")
  private Address address;

  @Schema(description = "Número de teléfono", example = "3001234567")
  private Long phoneNumber;

  @Schema(description = "Correo electrónico", example = "juan.perez@ejemplo.com")
  private String email;

  @Schema(description = "Nombre de usuario para autenticación", example = "jperez")
  private String username;

  @Schema(description = "Contraseña cifrada del usuario")
  private String password;

  @Schema(description = "Indica si el usuario está habilitado para operar", example = "true")
  private boolean enabled;

  @Schema(description = "Indica si la cuenta no ha expirado", example = "true")
  private boolean accountNonExpired;

  @Schema(description = "Indica si la cuenta no está bloqueada", example = "true")
  private boolean accountNonLocked;

  @Schema(description = "Indica si las credenciales no han expirado", example = "true")
  private boolean credentialsNonExpired;

  @Schema(description = "Conjunto de roles asignados al usuario")
  private Set<Role> roles;

  /**
   * Indica si el usuario posee el rol administrativo.
   *
   * @apiNote Se usa en UI para habilitar configuraciones avanzadas; no sustituye a la verificación
   *     de permisos en el backend.
   */
  public boolean isAdmin() {
    return this.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
  }
}
