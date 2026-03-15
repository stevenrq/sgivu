package com.sgivu.auth.security;

import com.sgivu.auth.dto.User;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;

/**
 * Detalles de usuario personalizados para integración con Spring Security.
 *
 * <p>Esta clase adapta el DTO de dominio {@link com.sgivu.auth.dto.User} al modelo de Spring
 * Security extendiendo {@link org.springframework.security.core.userdetails.User} e implementando
 * {@link org.springframework.security.core.AuthenticatedPrincipal}. Además de los datos habituales
 * de Spring Security, expone el {@code id} de dominio del usuario.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class CustomUserDetails extends org.springframework.security.core.userdetails.User
    implements AuthenticatedPrincipal {

  private final Long id;

  /**
   * Construye los detalles de usuario a partir del DTO de dominio.
   *
   * @param user objeto {@link User} que contiene los datos del usuario
   * @param authorities colección de {@link GrantedAuthority} que representa los permisos del
   *     usuario
   */
  public CustomUserDetails(User user, Collection<? extends GrantedAuthority> authorities) {
    super(
        user.getUsername(),
        user.getPassword(),
        user.isEnabled(),
        user.isAccountNonExpired(),
        user.isCredentialsNonExpired(),
        user.isAccountNonLocked(),
        authorities);
    this.id = user.getId();
  }

  /**
   * Devuelve el nombre del principal autenticado (el nombre de usuario).
   *
   * @return el nombre de usuario
   */
  @Override
  public String getName() {
    return getUsername();
  }
}
