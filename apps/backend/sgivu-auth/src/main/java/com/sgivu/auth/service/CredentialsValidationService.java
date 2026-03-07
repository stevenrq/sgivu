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

@Service
public class CredentialsValidationService {

  private static final Logger logger = LoggerFactory.getLogger(CredentialsValidationService.class);
  private static final String INVALID_CREDENTIALS = "invalid_credentials";

  private final UserClient userClient;
  private final PasswordEncoder passwordEncoder;
  private final Tracer tracer;

  public CredentialsValidationService(
      UserClient userClient, PasswordEncoder passwordEncoder, Tracer tracer) {
    this.userClient = userClient;
    this.passwordEncoder = passwordEncoder;
    this.tracer = tracer;
  }

  public CredentialsValidationResponse validateCredentials(String username, String password) {
    return executeWithSpan(
        "validateCredentials",
        () -> {
          try {
            User user = fetchUser(username);

            if (user == null) {
              logger.warn("User '{}' not found during credential validation.", username);
              return new CredentialsValidationResponse(false, INVALID_CREDENTIALS);
            }

            CredentialsValidationResponse statusCheck = checkUserStatus(user);
            if (statusCheck != null) {
              return statusCheck;
            }

            if (!passwordEncoder.matches(password, user.getPassword())) {
              logger.warn("Invalid password for user '{}'.", username);
              return new CredentialsValidationResponse(false, INVALID_CREDENTIALS);
            }

            return new CredentialsValidationResponse(true, "");
          } catch (UsernameNotFoundException ex) {
            return new CredentialsValidationResponse(false, INVALID_CREDENTIALS);
          } catch (ServiceUnavailableException ex) {
            logger.error("User service unavailable during credential validation.");
            return new CredentialsValidationResponse(false, "service_unavailable");
          } catch (Exception ex) {
            logger.error("Unexpected error during credential validation: {}", ex.toString());
            return new CredentialsValidationResponse(false, "unexpected_error");
          }
        });
  }

  private CredentialsValidationResponse checkUserStatus(User user) {
    if (!user.isEnabled()) {
      return new CredentialsValidationResponse(false, "disabled");
    }

    if (!user.isAccountNonExpired()) {
      return new CredentialsValidationResponse(false, "expired");
    }

    if (!user.isAccountNonLocked()) {
      return new CredentialsValidationResponse(false, "locked");
    }

    if (!user.isCredentialsNonExpired()) {
      return new CredentialsValidationResponse(false, "credentials");
    }

    return null;
  }

  @CircuitBreaker(name = "userServiceCircuitBreaker", fallbackMethod = "fallbackFetchUser")
  private User fetchUser(String username) {
    Span span = tracer.nextSpan().name("fetchUser").start();
    try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
      try {
        return userClient.findByUsername(username);
      } catch (HttpClientErrorException.NotFound ex) {
        span.tag("error", "no_encontrado");
        logger.warn("User '{}' not found (404) during validation.", username);
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
        "Fallback triggered during credential validation for user '{}'. Cause: {}",
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
