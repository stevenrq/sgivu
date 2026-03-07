package com.sgivu.gateway.security;

import com.sgivu.gateway.config.AngularClientProperties;
import com.sgivu.gateway.config.ServicesProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.security.web.server.authentication.logout.DelegatingServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.SecurityContextServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

/**
 * Configuración de seguridad del Gateway (BFF).
 *
 * <p>Define <strong>dos cadenas de filtros</strong> con propósitos distintos:
 *
 * <ul>
 *   <li><strong>Chain 1 (Order 1) — {@code /v1/**}</strong>: protege las APIs de negocio como
 *       Resource Server (JWT). La autenticación proviene de la WebSession establecida por Chain 2
 *       durante el login OAuth2. Retorna 401 sin redirección cuando la sesión expira.
 *   <li><strong>Chain 2 (Order 2) — rutas generales</strong>: maneja el flujo OAuth2
 *       (login/logout/callback). Usa {@code oauth2Login()} con PKCE para el Authorization Code flow
 *       y gestiona el ciclo de vida completo de la sesión.
 * </ul>
 *
 * <h3>Flujo de autenticación</h3>
 *
 * <ol>
 *   <li>Angular SPA llama a {@code /auth/session} → si no hay sesión, recibe 401
 *   <li>Angular redirige al usuario a {@code /oauth2/authorization/sgivu-gateway}
 *   <li>Chain 2 inicia el flujo Authorization Code + PKCE hacia el auth server
 *   <li>Tras login exitoso, se crea la sesión en Redis y se redirige a Angular
 *   <li>Requests posteriores a {@code /v1/**} usan la sesión Redis (Chain 1)
 * </ol>
 *
 * <h3>Token Relay</h3>
 *
 * <p>Las rutas en {@code GatewayRoutesConfig} usan el filtro {@code tokenRelay()} que obtiene el
 * access_token de la sesión y lo agrega como Bearer header. El {@link
 * ReactiveOAuth2AuthorizedClientManager} configurado aquí incluye un {@link
 * RefreshTokenReactiveOAuth2AuthorizedClientProvider} que renueva tokens expirados automáticamente
 * usando el refresh_token almacenado en la sesión.
 *
 * @see com.sgivu.gateway.config.GatewayRoutesConfig
 * @see com.sgivu.gateway.config.RedisSessionConfig
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
  private static final String LOGOUT_URL = "/logout";

  private final ServicesProperties servicesProperties;
  private final AngularClientProperties angularClientProperties;

  public SecurityConfig(
      ServicesProperties servicesProperties, AngularClientProperties angularClientProperties) {
    this.servicesProperties = servicesProperties;
    this.angularClientProperties = angularClientProperties;
  }

  /**
   * <strong>Chain 1 (Order 1)</strong> — Protege las APIs de negocio ({@code /v1/**}).
   *
   * <p>Actúa como Resource Server validando JWT, pero la autenticación real proviene de la
   * WebSession (establecida por Chain 2). Retorna HTTP 401 directamente sin redirección, lo que
   * permite a Angular manejar la re-autenticación de forma controlada.
   *
   * <p>CSRF está deshabilitado porque la SPA usa tokens Bearer y la cookie de sesión es HttpOnly
   * con SameSite=Lax, lo que protege contra CSRF en navegadores modernos.
   */
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

    log.info("Security Chain 1 (API /v1/**) configured — entry point: 401 UNAUTHORIZED");
    return http.build();
  }

  /**
   * <strong>Chain 2 (Order 2)</strong> — Maneja flujos OAuth2 y rutas generales.
   *
   * <p>Configura:
   *
   * <ul>
   *   <li>{@code oauth2Login}: flujo Authorization Code + PKCE con redirect a Angular tras login
   *   <li>{@code logout}: cadena de 3 handlers (revocación de tokens → limpieza de SecurityContext
   *       → invalidación de sesión Redis) seguida de redirect OIDC logout al auth server
   *   <li>Entry point 401 (sin redirect): Angular decide cuándo iniciar re-autenticación
   * </ul>
   */
  @Bean
  @Order(2)
  SecurityWebFilterChain securityWebFilterChain(
      ServerHttpSecurity http,
      ReactiveClientRegistrationRepository clientRegistrationRepository,
      ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver,
      ServerOAuth2AuthorizedClientRepository authorizedClientRepository,
      ServerLogoutHandler tokenRevocationLogoutHandler) {
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
            logout -> {
              // Handler que invalida la sesión Redis (WebSession) como paso final del logout.
              // Se ejecuta después de revocar tokens y limpiar el SecurityContext para garantizar
              // que la sesión no pueda reutilizarse incluso si los pasos anteriores fallan.
              ServerLogoutHandler sessionInvalidationHandler =
                  (webFilterExchange, auth) ->
                      webFilterExchange
                          .getExchange()
                          .getSession()
                          .doOnNext(
                              session ->
                                  log.info(
                                      "Logout: invalidating Redis session [id={}]",
                                      session.getId()))
                          .flatMap(WebSession::invalidate);
              logout
                  .requiresLogout(ServerWebExchangeMatchers.pathMatchers(LOGOUT_URL))
                  .logoutHandler(
                      new DelegatingServerLogoutHandler(
                          tokenRevocationLogoutHandler,
                          new SecurityContextServerLogoutHandler(),
                          sessionInvalidationHandler))
                  .logoutSuccessHandler(logoutSuccessHandler(clientRegistrationRepository));
            })
        .authorizeExchange(
            exchanges ->
                exchanges
                    .pathMatchers(
                        "/docs/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/webjars/**")
                    .permitAll()
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

    log.info("Security Chain 2 (OAuth2 login/logout + general) configured");
    return http.build();
  }

  /**
   * Resolver de authorization requests con PKCE habilitado.
   *
   * <p>PKCE (Proof Key for Code Exchange) es obligatorio para clientes públicos y recomendado para
   * confidenciales. Protege contra ataques de interceptación del authorization code.
   */
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
  ReactiveJwtDecoder jwtDecoder() {
    return NimbusReactiveJwtDecoder.withIssuerLocation(
            servicesProperties.getMap().get("sgivu-auth").getUrl())
        .build();
  }

  /**
   * Tras login OAuth2 exitoso, redirige a Angular ({@code /callback}) donde la SPA extrae la
   * información de sesión llamando a {@code /auth/session}.
   */
  @Bean
  ServerAuthenticationSuccessHandler authenticationSuccessHandler() {
    return new RedirectServerAuthenticationSuccessHandler(
        angularClientProperties.getUrl().concat("/callback"));
  }

  /**
   * Handler de éxito post-logout con dos estrategias:
   *
   * <ul>
   *   <li><strong>Con OidcUser disponible</strong>: usa {@code end_session_endpoint} del auth
   *       server (OIDC RP-Initiated Logout) con {@code id_token_hint} y {@code
   *       post_logout_redirect_uri} hacia Angular.
   *   <li><strong>Sin OidcUser (sesión expirada)</strong>: redirige a {@code /sso-logout} del auth
   *       server como fallback. Esto es necesario porque sin la sesión no hay {@code id_token_hint}
   *       y el endpoint OIDC estándar fallaría. El {@code SsoLogoutController} en el auth server
   *       invalida la sesión JDBC y redirige a Angular.
   * </ul>
   *
   * <p><strong>¿Por qué es necesario el fallback?</strong> Cuando la sesión Redis del gateway
   * expira, no hay {@code OidcUser} en el {@code SecurityContext}. Sin fallback, el handler OIDC
   * fallaría al intentar obtener el {@code id_token_hint}, dejando la sesión del auth server activa
   * y causando re-autenticación automática silenciosa en vez de un logout limpio.
   */
  @Bean
  ServerLogoutSuccessHandler logoutSuccessHandler(
      ReactiveClientRegistrationRepository clientRegistrationRepository) {
    OidcClientInitiatedServerLogoutSuccessHandler handler =
        new OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrationRepository);
    handler.setPostLogoutRedirectUri(angularClientProperties.getUrl().concat("/login"));

    String angularLoginUrl = angularClientProperties.getUrl().concat("/login");
    String authUrl = servicesProperties.getMap().get("sgivu-auth").getUrl();
    String ssoLogoutFallbackUrl =
        authUrl
            + "/sso-logout?redirect_uri="
            + URLEncoder.encode(angularLoginUrl, StandardCharsets.UTF_8);
    handler.setLogoutSuccessUrl(URI.create(ssoLogoutFallbackUrl));

    log.info(
        "Logout success handler configured — OIDC redirect to Angular /login, "
            + "fallback SSO logout: {}",
        ssoLogoutFallbackUrl);
    return handler;
  }

  /**
   * Repositorio de clientes autorizados basado en WebSession.
   *
   * <p>Almacena los tokens OAuth2 ({@code access_token}, {@code refresh_token}) dentro de la sesión
   * Redis del gateway. Esto permite que el filtro {@code tokenRelay()} los recupere para
   * inyectarlos como Bearer header en las peticiones a microservicios.
   */
  @Bean
  ServerOAuth2AuthorizedClientRepository authorizedClientRepository() {
    return new WebSessionServerOAuth2AuthorizedClientRepository();
  }

  @Bean
  ServerLogoutHandler tokenRevocationLogoutHandler(
      ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {
    return new TokenRevocationServerLogoutHandler(authorizedClientRepository);
  }

  /**
   * Manager de clientes autorizados con soporte de refresh automático de tokens.
   *
   * <p>Combina dos providers:
   *
   * <ul>
   *   <li>{@link AuthorizationCodeReactiveOAuth2AuthorizedClientProvider}: para el flujo inicial
   *   <li>{@link RefreshTokenReactiveOAuth2AuthorizedClientProvider}: renueva el access_token
   *       usando el refresh_token cuando el primero expira. El {@code clockSkew} de 5 segundos
   *       permite renovar ligeramente antes de la expiración real.
   * </ul>
   *
   * <h3>Manejo de {@code invalid_grant}</h3>
   *
   * <p>Cuando el auth server rechaza el refresh_token (token revocado, auth server reiniciado,
   * sesión JDBC expirada), se recibe un error {@code invalid_grant}. En ese caso se retorna {@code
   * Mono.empty()}, lo que causa:
   *
   * <ul>
   *   <li>En {@code tokenRelay()}: el request se envía sin Bearer → el microservicio devuelve 401
   *   <li>En {@code /auth/session}: el endpoint devuelve 401
   *   <li>Angular detecta el 401 e inicia re-autenticación
   * </ul>
   *
   * <p>Este flujo es intencional: no se puede recuperar de un refresh_token inválido sin
   * re-autenticación completa del usuario.
   */
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
            .doOnNext(
                client -> {
                  // Log exitoso: útil para confirmar que el token relay funciona y trazar
                  // renovaciones de tokens. Incluye la expiración del access_token para
                  // correlacionar con la timeline de sesión.
                  Instant expiresAt = client.getAccessToken().getExpiresAt();
                  log.debug(
                      "OAuth2 client authorized [registration={}, accessTokenExpiresAt={}]",
                      client.getClientRegistration().getRegistrationId(),
                      expiresAt);
                })
            .onErrorResume(
                ClientAuthorizationException.class,
                ex -> {
                  String errorCode =
                      ex.getError() != null ? ex.getError().getErrorCode() : "unknown";
                  String description =
                      ex.getError() != null ? ex.getError().getDescription() : "no description";

                  if (OAuth2ErrorCodes.INVALID_GRANT.equals(errorCode)) {
                    // El refresh_token fue rechazado por el auth server. Causas comunes:
                    // - El auth server fue reiniciado y las autorizaciones en BD se perdieron
                    // - El refresh_token fue revocado (logout en otro dispositivo)
                    // - La sesión JDBC del auth server expiró y limpió la autorización
                    // Se retorna Mono.empty() para que el flujo termine en 401 → re-autenticación
                    log.warn(
                        "OAuth2 token refresh failed — invalid_grant [registration={}, "
                            + "description={}]. The user session will require re-authentication.",
                        Optional.ofNullable(authorizeRequest.getClientRegistrationId())
                            .orElse("N/A"),
                        description);
                    return Mono.empty();
                  }

                  log.error(
                      "OAuth2 client authorization error [registration={}, errorCode={}, "
                          + "description={}]",
                      Optional.ofNullable(authorizeRequest.getClientRegistrationId()).orElse("N/A"),
                      errorCode,
                      description,
                      ex);
                  return Mono.error(ex);
                });
  }

  /**
   * Convierte los claims del JWT en authorities de Spring Security.
   *
   * <p>Extrae la claim {@code rolesAndPermissions} (emitida por el auth server con roles y permisos
   * del usuario) y las mapea a {@link GrantedAuthority} para que {@code @PreAuthorize} funcione en
   * los microservicios (a través del token relay).
   */
  private Mono<JwtAuthenticationToken> convert(Jwt source) {
    List<String> rolesAndPermissionsClaim = source.getClaimAsStringList("rolesAndPermissions");

    if (rolesAndPermissionsClaim == null || rolesAndPermissionsClaim.isEmpty()) {
      log.debug("JWT without rolesAndPermissions claim [sub={}]", source.getSubject());
      return Mono.just(new JwtAuthenticationToken(source, List.of()));
    }

    List<GrantedAuthority> rolesAndPermissions =
        rolesAndPermissionsClaim.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

    log.debug(
        "JWT converted [sub={}, authorities={}]", source.getSubject(), rolesAndPermissions.size());
    return Mono.just(new JwtAuthenticationToken(source, rolesAndPermissions));
  }
}
