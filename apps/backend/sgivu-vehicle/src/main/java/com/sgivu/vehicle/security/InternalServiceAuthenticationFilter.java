package com.sgivu.vehicle.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica llamadas internas basadas en la cabecera X-Internal-Service-Key.
 *
 * <p>Cuando la clave coincide, se inyecta un Authentication con los permisos necesarios para que
 * las anotaciones {@code @PreAuthorize} permitan el acceso sin JWT.
 */
@Component
public class InternalServiceAuthenticationFilter extends OncePerRequestFilter {

  private static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";

  private final String internalServiceKey;
  private final List<SimpleGrantedAuthority> internalAuthorities =
      List.of(
          new SimpleGrantedAuthority("car:read"),
          new SimpleGrantedAuthority("car:create"),
          new SimpleGrantedAuthority("car:update"),
          new SimpleGrantedAuthority("car:delete"),
          new SimpleGrantedAuthority("motorcycle:read"),
          new SimpleGrantedAuthority("motorcycle:create"),
          new SimpleGrantedAuthority("motorcycle:update"),
          new SimpleGrantedAuthority("motorcycle:delete"),
          new SimpleGrantedAuthority("vehicle:read"),
          new SimpleGrantedAuthority("vehicle:create"),
          new SimpleGrantedAuthority("vehicle:delete"));

  public InternalServiceAuthenticationFilter(
      @Value("${service.internal.secret-key}") String internalServiceKey) {
    this.internalServiceKey = internalServiceKey;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    if (shouldAuthenticate(request)) {
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken("internal-service", null, internalAuthorities);
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }

  private boolean shouldAuthenticate(HttpServletRequest request) {
    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      return false;
    }
    String providedKey = request.getHeader(INTERNAL_KEY_HEADER);
    return internalServiceKey.equals(providedKey);
  }
}
