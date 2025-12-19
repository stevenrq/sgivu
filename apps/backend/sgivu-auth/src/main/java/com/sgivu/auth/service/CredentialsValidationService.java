package com.sgivu.auth.service;

import com.sgivu.auth.client.UserClient;
import com.sgivu.auth.dto.CredentialsValidationResponse;
import com.sgivu.auth.dto.User;
import com.sgivu.auth.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Servicio para validar credenciales en tiempo real usando el cliente de usuarios y el codificador
 * de contraseñas.
 */
@Service
public class CredentialsValidationService {

  private static final Logger logger = LoggerFactory.getLogger(CredentialsValidationService.class);

  private final UserClient userClient;
  private final PasswordEncoder passwordEncoder;
  private final Tracer tracer;

  public CredentialsValidationService(
      UserClient userClient, PasswordEncoder passwordEncoder, Tracer tracer) {
    this.userClient = userClient;
    this.passwordEncoder = passwordEncoder;
    this.tracer = tracer;
  }

  /**
   * Valida las credenciales de un usuario. Retorna diferentes razones de fallo según el resultado:
   * usuario no encontrado, contraseña inválida, cuenta deshabilitada, etc.
   *
   * @param username nombre de usuario a validar.
   * @param password contraseña a validar (en texto plano).
   * @return CredentialsValidationResponse con el resultado de la validación.
   */
  public CredentialsValidationResponse validateCredentials(String username, String password) {
    return executeWithSpan(
        "validateCredentials",
        () -> {
          try {
            User user = fetchUser(username);

            if (user == null) {
              logger.warn(
                  "Usuario '{}' no encontrado durante validación de credenciales.", username);
              return new CredentialsValidationResponse(false, "invalid_credentials");
            }

            // Validar si la cuenta está habilitada
            if (!user.isEnabled()) {
              return new CredentialsValidationResponse(false, "disabled");
            }

            // Validar si la cuenta no está expirada
            if (!user.isAccountNonExpired()) {
              return new CredentialsValidationResponse(false, "expired");
            }

            // Validar si la cuenta no está bloqueada
            if (!user.isAccountNonLocked()) {
              return new CredentialsValidationResponse(false, "locked");
            }

            // Validar si las credenciales no están expiradas
            if (!user.isCredentialsNonExpired()) {
              return new CredentialsValidationResponse(false, "credentials");
            }

            // Comparar la contraseña proporcionada con el hash almacenado
            if (!passwordEncoder.matches(password, user.getPassword())) {
              logger.warn("Contraseña inválida para el usuario '{}'.", username);
              return new CredentialsValidationResponse(false, "invalid_credentials");
            }

            // Todas las validaciones pasaron
            logger.info("Credenciales válidas para el usuario '{}'.", username);
            return new CredentialsValidationResponse(true, "");
          } catch (UsernameNotFoundException ex) {
            return new CredentialsValidationResponse(false, "invalid_credentials");
          } catch (ServiceUnavailableException ex) {
            logger.error("Servicio de usuarios no disponible durante validación de credenciales.");
            return new CredentialsValidationResponse(false, "service_unavailable");
          } catch (Exception ex) {
            logger.error(
                "Error inesperado durante la validación de credenciales: {}", ex.toString());
            return new CredentialsValidationResponse(false, "unexpected_error");
          }
        });
  }

  @SuppressWarnings("unused")
  @CircuitBreaker(name = "userServiceCircuitBreaker", fallbackMethod = "fallbackFetchUser")
  private User fetchUser(String username) {
    Span span = tracer.nextSpan().name("fetchUser").start();
    try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
      try {
        return userClient.findByUsername(username);
      } catch (HttpClientErrorException.NotFound ex) {
        span.tag("error", "no_encontrado");
        logger.warn("Usuario '{}' no encontrado (404) durante validación.", username);
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
    logger.error(
        "Fallback activado durante validación de credenciales para usuario '{}'. Causa: {}",
        username,
        ex.toString());
    throw new ServiceUnavailableException(
        "Servicio de usuarios no disponible. Por favor, intente más tarde.", ex);
  }

  private <T> T executeWithSpan(String spanName, SpanCallable<T> callable) {
    Span span = tracer.nextSpan().name(spanName).start();
    try (Tracer.SpanInScope spanScope = tracer.withSpan(span)) {
      return callable.call();
    } finally {
      span.end();
    }
  }

  @FunctionalInterface
  private interface SpanCallable<T> {
    T call();
  }
}
