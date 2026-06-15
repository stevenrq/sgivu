package com.sgivu.auth.service;

import com.sgivu.auth.client.UserClient;
import com.sgivu.auth.dto.CredentialsValidationResponse;
import com.sgivu.auth.dto.User;
import com.sgivu.auth.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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

  public CredentialsValidationService(UserClient userClient, PasswordEncoder passwordEncoder) {
    this.userClient = userClient;
    this.passwordEncoder = passwordEncoder;
  }

  public CredentialsValidationResponse validateCredentials(String username, String password) {
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
    try {
      return userClient.findByUsername(username);
    } catch (HttpClientErrorException.NotFound ex) {
      logger.warn("User '{}' not found (404) during validation.", username);
      throw new UsernameNotFoundException("Usuario no encontrado: " + username, ex);
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
}
