package com.sgivu.user.security;

import com.sgivu.user.config.InternalServiceAuthorizationManager;
import com.sgivu.user.config.ServicesProperties;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Configuración central de seguridad para exponer el servicio como resource server con JWT y un
 * canal interno reforzado por clave compartida. El objetivo es aislar operaciones sensibles sobre
 * usuarios y roles cuando el Gateway o {@code sgivu-auth} necesitan acceder sin credenciales de
 * usuario final.
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
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Define las reglas HTTP: salud pública, ruta interna para enriquecer tokens y resto protegido
   * por JWT.
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
                    // Ruta reservada a sgivu-auth para enriquecer tokens internos
                    .requestMatchers("/v1/users/username/**")
                    .access(internalServiceAuthManager)
                    .requestMatchers("/v1/users/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .csrf(AbstractHttpConfigurer::disable);

    return http.build();
  }

  /**
   * Autoriza llamadas provenientes de servicios internos confiables o de clientes autenticados.
   *
   * <p>Se usa para endpoints donde el API Gateway puede enrutar tráfico tanto autenticado como
   * interno (por ejemplo, sincronizaciones entre microservicios).
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

  /** Decodificador JWT apuntando al emisor publicado por {@code sgivu-auth}. */
  @Bean
  JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withIssuerLocation(
            servicesProperties.getMap().get("sgivu-auth").getUrl())
        .build();
  }

  /**
   * Mapea el claim {@code rolesAndPermissions} a authorities de Spring sin reinterpretar el token;
   * si el claim viene vacío no se asignan authorities para evitar accesos implícitos.
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
