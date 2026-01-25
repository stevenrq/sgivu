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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Configuración de seguridad del microservicio de vehículos.
 *
 * <p>Actúa como recurso protegido OAuth2 validando JWT emitidos por el Authorization Server de
 * SGIVU. Combina autenticación estándar con autorización basada en clave interna para permitir
 * integraciones servicio-a-servicio.
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
   * Define la cadena de filtros, habilita el Resource Server JWT y autoriza endpoints de inventario
   * combinando claves internas y JWT.
   *
   * @param http configurador de seguridad HTTP
   * @return {@link SecurityFilterChain} listo para Spring Security
   */
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
                    // Servicios internos (clave) o clientes autenticados pueden acceder
                    .requestMatchers("/v1/cars/**", "/v1/motorcycles/**")
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
   * Decoder JWT usando el issuer configurado en {@link ServicesProperties}.
   *
   * @return decodificador Nimbus
   */
  @Bean
  JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withIssuerLocation(
            servicesProperties.getMap().get("sgivu-auth").getUrl())
        .build();
  }

  /**
   * Convierte el claim rolesAndPermissions en una lista de SimpleGrantedAuthority
   *
   * <p>Extrae roles/permisos emitidos por el Authorization Server para aplicar autorizaciones en
   * controladores (vía @PreAuthorize).
   *
   * @return un {@link JwtAuthenticationConverter} configurado que extrae las autoridades del claim
   *     JWT "rolesAndPermissions" para ser utilizadas por Spring Security para la autorización.
   * @see JwtAuthenticationConverter
   * @see SimpleGrantedAuthority
   * @see GrantedAuthority
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
