package com.sgivu.purchasesale.security;

import com.sgivu.purchasesale.config.InternalServiceAuthorizationManager;
import com.sgivu.purchasesale.config.ServicesProperties;
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

/**
 * Configura el microservicio como recurso protegido por JWT emitidos por el Authorization Server de
 * SGIVU. Combina autorización por roles/permisos con un canal dedicado para llamadas internas
 * autenticadas mediante clave compartida.
 */
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

  /**
   * Define las reglas de seguridad HTTP: expone health/info sin autenticación, protege el resto con
   * JWT y habilita acceso combinado para clientes externos autenticados o servicios internos
   * firmados con cabecera {@code X-Internal-Service-Key}.
   */
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(convert())))
        .authorizeHttpRequests(
            authz ->
                authz
                    .requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    // Servicios internos (clave) o clientes autenticados pueden acceder
                    .requestMatchers("/v1/purchase-sales/**")
                    .access(internalOrAuthenticatedAuthorizationManager())
                    .anyRequest()
                    .authenticated())
        .csrf(AbstractHttpConfigurer::disable)
        // Inyecta Authentication con permisos cuando se usa la clave interna
        .addFilterBefore(
            internalServiceAuthenticationFilter, BearerTokenAuthenticationFilter.class);

    return http.build();
  }

  /** Autoriza llamadas provenientes de servicios internos confiables o de clientes autenticados. */
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

  /**
   * Configura el decodificador JWT con la URL del Authorization Server de SGIVU.
   *
   * @return {@link JwtDecoder} inicializado con el issuer configurado
   */
  @Bean
  JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withIssuerLocation(
            servicesProperties.getMap().get("sgivu-auth").getUrl())
        .build();
  }

  /**
   * Convierte el claim {@code rolesAndPermissions} en una lista de {@link SimpleGrantedAuthority}
   * para que Spring Security pueda evaluarlos en anotaciones {@code @PreAuthorize}.
   *
   * @return convertidor JWT listo para extraer roles y permisos del token
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
