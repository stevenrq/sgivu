package com.sgivu.user.dto;

import com.sgivu.user.entity.Address;
import com.sgivu.user.entity.Role;
import com.sgivu.user.util.RolePermissionUtils;
import com.sgivu.user.validation.ValidationGroups;
import com.sgivu.user.validation.annotation.NoSpecialCharacters;
import com.sgivu.user.validation.annotation.PasswordStrength;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Schema(description = "DTO para la actualización de datos de un usuario existente")
public class UserUpdateRequest {

  @Schema(
      description = "Nombre del usuario",
      example = "Juan",
      requiredMode = Schema.RequiredMode.REQUIRED,
      minLength = 3,
      maxLength = 20)
  @Size(min = 3, max = 20)
  @NotBlank
  @Column(nullable = false, length = 20)
  private String firstName;

  @Schema(
      description = "Apellido del usuario",
      example = "Pérez",
      requiredMode = Schema.RequiredMode.REQUIRED,
      minLength = 3,
      maxLength = 20)
  @Size(min = 3, max = 20)
  @NotBlank
  @Column(name = "last_name", nullable = false, length = 20)
  private String lastName;

  @Schema(description = "Dirección física del usuario")
  private Address address;

  @Schema(
      description = "Número de teléfono (10 dígitos)",
      example = "3001234567",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  @Column(name = "phone_number", nullable = false, unique = true, length = 10)
  private Long phoneNumber;

  @Schema(
      description = "Correo electrónico válido",
      example = "juan.perez@ejemplo.com",
      requiredMode = Schema.RequiredMode.REQUIRED,
      minLength = 16,
      maxLength = 40)
  @Email
  @Size(min = 16, max = 40)
  @NotBlank
  @Column(nullable = false, unique = true, length = 40)
  private String email;

  @Schema(
      description = "Nombre de usuario único para autenticación (sin caracteres especiales)",
      example = "jperez",
      requiredMode = Schema.RequiredMode.REQUIRED,
      minLength = 6,
      maxLength = 20)
  @Size(min = 6, max = 20)
  @NoSpecialCharacters
  @NotBlank
  @Column(nullable = false, unique = true, length = 20)
  private String username;

  @Schema(
      description = "Contraseña (requerida solo en creación, debe cumplir política de fortaleza)",
      example = "SecureP@ss123",
      minLength = 6,
      maxLength = 60)
  @Size(min = 6, max = 60, groups = ValidationGroups.Create.class)
  @PasswordStrength(groups = ValidationGroups.Create.class)
  @NotBlank(groups = ValidationGroups.Create.class)
  @Column(nullable = false, length = 60)
  private String password;

  @Schema(hidden = true)
  @Transient
  private boolean admin;

  @Schema(description = "Conjunto de roles a asignar al usuario")
  private Set<Role> roles;

  public Set<String> getRolesAndPermissions() {
    return RolePermissionUtils.getRolesAndPermissions(this.roles);
  }
}
