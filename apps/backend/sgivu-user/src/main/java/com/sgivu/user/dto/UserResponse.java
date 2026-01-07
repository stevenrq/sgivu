package com.sgivu.user.dto;

import com.sgivu.user.entity.Address;
import com.sgivu.user.entity.Role;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista expuesta del usuario para respuestas públicas e internas.
 *
 * <p>Se usa por el Authorization Server para poblar claims y por backoffice para visualizar
 * atributos relevantes sin exponer detalles internos de seguridad.
 */
@Data
@NoArgsConstructor
public class UserResponse {

  private Long id;
  private Long nationalId;
  private String firstName;
  private String lastName;
  private Address address;
  private Long phoneNumber;
  private String email;
  private String username;
  private String password;
  private boolean enabled;
  private boolean accountNonExpired;
  private boolean accountNonLocked;
  private boolean credentialsNonExpired;
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
