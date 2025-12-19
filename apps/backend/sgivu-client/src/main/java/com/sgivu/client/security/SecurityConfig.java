package com.sgivu.client.security;

import com.sgivu.client.config.InternalServiceAuthorizationManager;
import com.sgivu.client.config.ServicesProperties;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Configura el servicio como Resource Server JWT y permite llamadas internas con clave compartida.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final InternalServiceAuthorizationManager internalServiceAuthManager;
  private final ServicesProperties servicesProperties;

  public SecurityConfig(
      InternalServiceAuthorizationManager internalServiceAuthManager,
      ServicesProperties servicesProperties) {
    this.internalServiceAuthManager = internalServiceAuthManager;
    this.servicesProperties = servicesProperties;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(convert())))
        .authorizeHttpRequests(
            authz ->
                authz
                    .requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .requestMatchers("/v1/persons/**", "/v1/companies/**")
                    .access(internalOrAuthenticatedAuthorizationManager())
                    .anyRequest()
                    .authenticated())
        .csrf(AbstractHttpConfigurer::disable);

    return http.build();
  }

  /**
   * Permite acceso tanto por clave interna como por JWT, evitando bloquear flujos servicio a
   * servicio cuando no hay contexto de usuario.
   */
  @Bean
  AuthorizationManager<RequestAuthorizationContext> internalOrAuthenticatedAuthorizationManager() {
    AuthorizationManager<RequestAuthorizationContext> authenticatedManager =
        (authenticationSupplier, context) -> {
          Authentication authentication = authenticationSupplier.get();
          boolean isAuthenticated =
              authentication != null
                  && authentication.isAuthenticated()
                  && !(authentication instanceof AnonymousAuthenticationToken);
          return new AuthorizationDecision(isAuthenticated);
        };

    return AuthorizationManagers.anyOf(internalServiceAuthManager, authenticatedManager);
  }

  /** Issuer resuelto desde configuración externa para adaptar el decoder por entorno. */
  @Bean
  JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withIssuerLocation(
            servicesProperties.getMap().get("sgivu-auth").getUrl())
        .build();
  }

  /**
   * Mapea el claim {@code rolesAndPermissions} a autoridades para evitar mapeos estáticos locales.
   */
  @Bean
  JwtAuthenticationConverter convert() {
    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();

    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          List<String> rolesAndPermissions = jwt.getClaimAsStringList("rolesAndPermissions");

          if (rolesAndPermissions == null || rolesAndPermissions.isEmpty()) {
            return List.of();
          }

          return rolesAndPermissions.stream()
              .map(SimpleGrantedAuthority::new)
              .collect(Collectors.toList());
        });
    return jwtAuthenticationConverter;
  }
}
