package com.sgivu.auth.security;

import com.sgivu.auth.dto.User;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;

/**
 * Adaptador entre el DTO {@link com.sgivu.auth.dto.User} y Spring Security que expone el ID del
 * usuario como principal para sesión y trazabilidad.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class CustomUserDetails extends org.springframework.security.core.userdetails.User
    implements AuthenticatedPrincipal {

  private final Long id;

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

  @Override
  public String getName() {
    return getUsername();
  }
}
