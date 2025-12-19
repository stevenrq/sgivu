package com.sgivu.gateway.security;

import com.sgivu.gateway.config.AngularClientProperties;
import com.sgivu.gateway.config.ServicesProperties;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

/**
 * Configura el API Gateway como resource server JWT. Controla acceso a los dominios de SGIVU
 * (inventario de vehículos usados, contratos, compras/ventas y predicción de demanda) y aplica CORS
 * estricto hacia el front Angular corporativo. La validación de tokens se delega al emisor
 * `sgivu-auth`; el Gateway solo enruta peticiones cuando el usuario tiene roles/permisos presentes
 * en el JWT.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  private final ServicesProperties servicesProperties;
  private final AngularClientProperties angularClientProperties;

  public SecurityConfig(
      ServicesProperties servicesProperties, AngularClientProperties angularClientProperties) {
    this.servicesProperties = servicesProperties;
    this.angularClientProperties = angularClientProperties;
  }

  @Bean
  SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    http.cors(corsSpec -> corsSpec.configurationSource(corsConfigurationSource()))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(this::convert)))
        .authorizeExchange(
            exchanges ->
                exchanges
                    .pathMatchers(HttpMethod.POST, "/logout")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/authorized")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/auth")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/user")
                    .permitAll()
                    .pathMatchers("/fallback/**")
                    .permitAll()
                    .pathMatchers("/v1/auth/**")
                    .permitAll()
                    .pathMatchers("/v1/users/**")
                    .authenticated()
                    .pathMatchers("/v1/roles/**")
                    .authenticated()
                    .pathMatchers("/v1/permissions/**")
                    .authenticated()
                    .pathMatchers("/v1/persons/**")
                    .authenticated()
                    .pathMatchers("/v1/companies/**")
                    .authenticated()
                    .pathMatchers("/v1/vehicles/**")
                    .authenticated()
                    .pathMatchers("/v1/cars/**")
                    .authenticated()
                    .pathMatchers("/v1/motorcycles/**")
                    .authenticated()
                    .pathMatchers("/v1/purchase-sales/**")
                    .authenticated()
                    .pathMatchers("/v1/ml/**")
                    .authenticated()
                    .anyExchange()
                    .denyAll());
    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(angularClientProperties.getUrl()));
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public ReactiveJwtDecoder jwtDecoder() {
    return NimbusReactiveJwtDecoder.withIssuerLocation(
            servicesProperties.getMap().get("sgivu-auth").getUrl())
        .build();
  }

  private Mono<JwtAuthenticationToken> convert(Jwt source) {
    List<String> rolesAndPermissionsClaim = source.getClaimAsStringList("rolesAndPermissions");

    if (rolesAndPermissionsClaim == null || rolesAndPermissionsClaim.isEmpty()) {
      return Mono.just(new JwtAuthenticationToken(source, List.of()));
    }

    List<GrantedAuthority> rolesAndPermissions =
        rolesAndPermissionsClaim.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    return Mono.just(new JwtAuthenticationToken(source, rolesAndPermissions));
  }
}
