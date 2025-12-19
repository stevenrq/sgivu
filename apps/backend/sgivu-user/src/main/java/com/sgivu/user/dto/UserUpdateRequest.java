package com.sgivu.user.dto;

import com.sgivu.user.entity.Address;
import com.sgivu.user.entity.Role;
import com.sgivu.user.util.RolePermissionUtils;
import com.sgivu.user.validation.ValidationGroups;
import com.sgivu.user.validation.annotation.NoSpecialCharacters;
import com.sgivu.user.validation.annotation.PasswordStrength;
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

/**
 * Datos permitidos para actualizar un usuario existente.
 *
 * <p>Aplicable tanto para autoedición del usuario como para ajustes administrativos (roles,
 * credenciales). Utiliza grupos de validación para diferenciar creación y actualización.
 */
@Getter
@Setter
@ToString
public class UserUpdateRequest {

  @Size(min = 3, max = 20)
  @NotBlank
  @Column(nullable = false, length = 20)
  private String firstName;

  @Size(min = 3, max = 20)
  @NotBlank
  @Column(name = "last_name", nullable = false, length = 20)
  private String lastName;

  private Address address;

  @NotNull
  @Column(name = "phone_number", nullable = false, unique = true, length = 10)
  private Long phoneNumber;

  @Email
  @Size(min = 16, max = 40)
  @NotBlank
  @Column(nullable = false, unique = true, length = 40)
  private String email;

  @Size(min = 6, max = 20)
  @NoSpecialCharacters
  @NotBlank
  @Column(nullable = false, unique = true, length = 20)
  private String username;

  /**
   * La longitud de 60 se utiliza porque BCrypt generará una cadena de longitud 60. Se valida para
   * longitud mínima, fuerza y no estar en blanco en la creación.
   *
   * <p>La longitud debe manejarse correctamente en el frontend.
   */
  @Size(min = 6, max = 60, groups = ValidationGroups.Create.class)
  @PasswordStrength(groups = ValidationGroups.Create.class)
  @NotBlank(groups = ValidationGroups.Create.class)
  @Column(nullable = false, length = 60)
  private String password;

  @Transient private boolean admin;

  private Set<Role> roles;

  /**
   * Expone nombres de roles y permisos para sincronizar con el catálogo persistido antes de
   * guardar.
   *
   * @return conjunto de authorities solicitadas.
   * @apiNote Permite que el servicio valide que solo roles existentes sean aplicados.
   */
  public Set<String> getRolesAndPermissions() {
    return RolePermissionUtils.getRolesAndPermissions(this.roles);
  }
}
