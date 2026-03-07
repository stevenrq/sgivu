package com.sgivu.vehicle.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro que autentica llamadas internas entre microservicios usando una clave compartida enviada
 * en la cabecera {@code X-Internal-Service-Key}.
 *
 * <p>Si el valor de la cabecera coincide con la propiedad de configuración {@code
 * service.internal.secret-key} y no existe una autenticación previa en el {@link
 * org.springframework.security.core.context.SecurityContextHolder}, este filtro crea una
 * autenticación {@link
 * org.springframework.security.authentication.UsernamePasswordAuthenticationToken} con el principal
 * {@code "internal-service"} y los permisos necesarios para operar sobre recursos de vehículos
 * ({@code car:read}, {@code car:create}, {@code car:update}, {@code car:delete}, {@code
 * motorcycle:read}, {@code motorcycle:create}, {@code motorcycle:update}, {@code
 * motorcycle:delete}, {@code vehicle:read}, {@code vehicle:create}, {@code vehicle:delete}).
 *
 * <p>Extiende {@link org.springframework.web.filter.OncePerRequestFilter} para garantizar que la
 * lógica se ejecute una sola vez por petición y se registra como bean de Spring mediante {@link
 * org.springframework.stereotype.Component}.
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
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
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
