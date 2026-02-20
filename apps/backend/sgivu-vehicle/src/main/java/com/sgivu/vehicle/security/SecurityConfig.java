package com.sgivu.vehicle.security;

import com.sgivu.vehicle.config.InternalServiceAuthorizationManager;
import com.sgivu.vehicle.config.ServicesProperties;
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
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final InternalServiceAuthorizationManager internalServiceAuthManager;
  private final ServicesProperties servicesProperties;
  private final InternalServiceAuthenticationFilter internalServiceAuthenticationFilter;

  public SecurityConfig(
      InternalServiceAuthorizationManager internalServiceAuthManager,
      ServicesProperties servicesProperties,
      InternalServiceAuthenticationFilter internalServiceAuthenticationFilter) {
    this.internalServiceAuthManager = internalServiceAuthManager;
    this.servicesProperties = servicesProperties;
    this.internalServiceAuthenticationFilter = internalServiceAuthenticationFilter;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(convert())))
        .authorizeHttpRequests(
            authz ->
                authz
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**")
                    .permitAll()
                    // Solo los servicios internos o clientes autenticados pueden acceder
                    .requestMatchers("/v1/cars/**", "/v1/motorcycles/**")
                    .access(internalOrAuthenticatedAuthorizationManager())
                    .anyRequest()
                    .authenticated())
        .csrf(AbstractHttpConfigurer::disable)
        .addFilterBefore(
            internalServiceAuthenticationFilter, BearerTokenAuthenticationFilter.class);

    return http.build();
  }

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

  @Bean
  JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withIssuerLocation(
            servicesProperties.getMap().get("sgivu-auth").getUrl())
        .build();
  }

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
