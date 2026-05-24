package com.sgivu.auth.service;

import com.sgivu.auth.client.UserClient;
import com.sgivu.auth.dto.User;
import com.sgivu.auth.exception.ServiceUnavailableException;
import com.sgivu.auth.security.CustomUserDetails;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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

  public JpaUserDetailsService(UserClient userClient) {
    this.userClient = userClient;
  }

  @Override
  public UserDetails loadUserByUsername(String username) {
    User user = fetchUser(username);

    if (user == null) {
      logger.warn("User '{}' not found (UserClient returned null).", username);
      throw new UsernameNotFoundException("Usuario no encontrado: " + username);
    }

    return new CustomUserDetails(user, mapToGrantedAuthorities(user));
  }

  @CircuitBreaker(name = "userServiceCircuitBreaker", fallbackMethod = "fallbackFetchUser")
  private User fetchUser(String username) {
    try {
      return userClient.findByUsername(username);
    } catch (HttpClientErrorException.NotFound ex) {
      logger.warn("User '{}' not found (404 UserClient).", username);
      throw new UsernameNotFoundException("Usuario no encontrado: " + username, ex);
    }
  }

  @SuppressWarnings("unused")
  private User fallbackFetchUser(String username, Throwable ex) {
    logger.error("Fallback triggered for user '{}'. Cause: {}", username, ex.toString());
    throw new ServiceUnavailableException(
        "Servicio de usuarios no disponible. Por favor, intente más tarde.", ex);
  }

  private Set<GrantedAuthority> mapToGrantedAuthorities(User user) {
    return user.getRolesAndPermissions().stream()
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toSet());
  }
}
