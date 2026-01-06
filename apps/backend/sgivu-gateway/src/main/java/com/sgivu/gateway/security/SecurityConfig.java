package com.sgivu.gateway.security;

import com.sgivu.gateway.config.AngularClientProperties;
import com.sgivu.gateway.config.ServicesProperties;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.AuthorizationCodeReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.DelegatingReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.RefreshTokenReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

/**
 * Configura el API Gateway como BFF con soporte OAuth2/OIDC y resource server JWT. Controla acceso
 * a los dominios de SGIVU (inventario de vehículos usados, contratos, compras/ventas y predicción
 * de demanda) y aplica CORS estricto hacia el front Angular corporativo. El login se delega a
 * `sgivu-auth` y los tokens se propagan hacia los microservicios desde el gateway.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  private static final String LOGOUT_URL = "/logout";

  private final ServicesProperties servicesProperties;
  private final AngularClientProperties angularClientProperties;

  public SecurityConfig(
      ServicesProperties servicesProperties, AngularClientProperties angularClientProperties) {
    this.servicesProperties = servicesProperties;
    this.angularClientProperties = angularClientProperties;
  }

  @Bean
  @Order(1)
  SecurityWebFilterChain apiSecurityWebFilterChain(ServerHttpSecurity http) {
    http.securityMatcher(ServerWebExchangeMatchers.pathMatchers("/v1/**"))
        .cors(corsSpec -> corsSpec.configurationSource(corsConfigurationSource()))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .oauth2Client(Customizer.withDefaults())
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(this::convert)))
        .exceptionHandling(
            exceptions ->
                exceptions.authenticationEntryPoint(
                    new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))
        .authorizeExchange(
            exchanges ->
                exchanges.pathMatchers("/v1/auth/**").permitAll().anyExchange().authenticated());
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityWebFilterChain securityWebFilterChain(
      ServerHttpSecurity http,
      ReactiveClientRegistrationRepository clientRegistrationRepository,
      ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver,
      ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {
    http.cors(corsSpec -> corsSpec.configurationSource(corsConfigurationSource()))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .oauth2Login(
            oauth2 ->
                oauth2
                    .authorizationRequestResolver(authorizationRequestResolver)
                    .authorizedClientRepository(authorizedClientRepository)
                    .authenticationSuccessHandler(authenticationSuccessHandler()))
        .oauth2Client(Customizer.withDefaults())
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(this::convert)))
        .exceptionHandling(
            exceptions ->
                exceptions.authenticationEntryPoint(
                    new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))
        .logout(
            logout ->
                logout
                    .requiresLogout(ServerWebExchangeMatchers.pathMatchers(LOGOUT_URL))
                    .logoutSuccessHandler(logoutSuccessHandler(clientRegistrationRepository)))
        .authorizeExchange(
            exchanges ->
                exchanges
                    .pathMatchers("/oauth2/**", "/login/**")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, LOGOUT_URL)
                    .permitAll()
                    .pathMatchers(HttpMethod.POST, LOGOUT_URL)
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/authorized")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/auth")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/auth/session")
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
  ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver(
      ReactiveClientRegistrationRepository clientRegistrationRepository) {
    DefaultServerOAuth2AuthorizationRequestResolver resolver =
        new DefaultServerOAuth2AuthorizationRequestResolver(clientRegistrationRepository);
    resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
    return resolver;
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

  @Bean
  ServerAuthenticationSuccessHandler authenticationSuccessHandler() {
    return new RedirectServerAuthenticationSuccessHandler(
        angularClientProperties.getUrl().concat("/callback"));
  }

  @Bean
  ServerLogoutSuccessHandler logoutSuccessHandler(
      ReactiveClientRegistrationRepository clientRegistrationRepository) {
    OidcClientInitiatedServerLogoutSuccessHandler handler =
        new OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrationRepository);
    handler.setPostLogoutRedirectUri(angularClientProperties.getUrl().concat("/login"));
    handler.setLogoutSuccessUrl(URI.create(angularClientProperties.getUrl().concat("/login")));
    return handler;
  }

  @Bean
  public ServerOAuth2AuthorizedClientRepository authorizedClientRepository() {
    return new WebSessionServerOAuth2AuthorizedClientRepository();
  }

  @Bean
  ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
      ReactiveClientRegistrationRepository clientRegistrationRepository,
      ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {

    AuthorizationCodeReactiveOAuth2AuthorizedClientProvider authorizationCodeProvider =
        new AuthorizationCodeReactiveOAuth2AuthorizedClientProvider();
    RefreshTokenReactiveOAuth2AuthorizedClientProvider refreshTokenProvider =
        new RefreshTokenReactiveOAuth2AuthorizedClientProvider();
    refreshTokenProvider.setClockSkew(Duration.ofSeconds(5));
    ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider =
        new DelegatingReactiveOAuth2AuthorizedClientProvider(
            authorizationCodeProvider, refreshTokenProvider);
    DefaultReactiveOAuth2AuthorizedClientManager authorizedClientManager =
        new DefaultReactiveOAuth2AuthorizedClientManager(
            clientRegistrationRepository, authorizedClientRepository);
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

    return authorizeRequest ->
        authorizedClientManager
            .authorize(authorizeRequest)
            .onErrorResume(
                ClientAuthorizationException.class,
                ex -> {
                  String errorCode = ex.getError() != null ? ex.getError().getErrorCode() : null;
                  if (OAuth2ErrorCodes.INVALID_GRANT.equals(errorCode)) {
                    return Mono.empty();
                  }
                  return Mono.error(ex);
                });
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
