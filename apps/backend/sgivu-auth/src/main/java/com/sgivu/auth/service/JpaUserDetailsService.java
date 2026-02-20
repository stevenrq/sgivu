package com.sgivu.auth.service;

import com.sgivu.auth.client.UserClient;
import com.sgivu.auth.dto.User;
import com.sgivu.auth.exception.ServiceUnavailableException;
import com.sgivu.auth.security.CustomUserDetails;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class JpaUserDetailsService implements UserDetailsService {

  private static final Logger logger = LoggerFactory.getLogger(JpaUserDetailsService.class);

  private final UserClient userClient;
  private final Tracer tracer;

  public JpaUserDetailsService(UserClient userClient, Tracer tracer) {
    this.userClient = userClient;
    this.tracer = tracer;
  }

  @Override
  public UserDetails loadUserByUsername(String username) {
    return executeWithSpan(
        "loadUserByUsername",
        () -> {
          User user = fetchUser(username);

          if (user == null) {
            logger.warn("User '{}' not found (UserClient returned null).", username);
            throw new UsernameNotFoundException("Usuario no encontrado: " + username);
          }

          Span current = tracer.currentSpan();
          if (current != null) {
            current.tag("authentication.event", "usuario_encontrado");
            current.tag("authentication.username", username);
            if (user.getId() != null) {
              current.tag("authentication.user_id", String.valueOf(user.getId()));
            }
            try {
              String rolesCsv = String.join(",", user.getRolesAndPermissions());
              current.tag("authentication.roles", rolesCsv);
            } catch (Exception e) {
              logger.debug("Could not serialize roles for span: {}", e.getMessage());
            }
            current.event("Registro de usuario cargado desde el servicio de usuarios");
          }

          return new CustomUserDetails(user, mapToGrantedAuthorities(user));
        });
  }

  private <T> T executeWithSpan(String spanName, SpanCallable<T> callable) {
    Span span = tracer.nextSpan().name(spanName).start();
    try (Tracer.SpanInScope spanScope = tracer.withSpan(span)) {
      return callable.call();
    } finally {
      span.end();
    }
  }

  @CircuitBreaker(name = "userServiceCircuitBreaker", fallbackMethod = "fallbackFetchUser")
  private User fetchUser(String username) {
    Span span = tracer.nextSpan().name("fetchUser").start();
    try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
      try {
        return userClient.findByUsername(username);
      } catch (HttpClientErrorException.NotFound ex) {
        span.tag("error", "no_encontrado");
        logger.warn("User '{}' not found (404 UserClient).", username);
        throw new UsernameNotFoundException("Usuario no encontrado: " + username, ex);
      } catch (Exception ex) {
        span.error(ex);
        throw ex;
      }
    } finally {
      span.end();
    }
  }

  @SuppressWarnings("unused")
  private User fallbackFetchUser(String username, Throwable ex) {
    return executeWithSpan(
        "fallbackFetchUser",
        () -> {
          logger.error("Fallback triggered for user '{}'. Cause: {}", username, ex.toString());
          throw new ServiceUnavailableException(
              "Servicio de usuarios no disponible. Por favor, intente más tarde.", ex);
        });
  }

  private Set<GrantedAuthority> mapToGrantedAuthorities(User user) {
    return user.getRolesAndPermissions().stream()
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toSet());
  }

  @FunctionalInterface
  private interface SpanCallable<T> {
    T call();
  }
}
