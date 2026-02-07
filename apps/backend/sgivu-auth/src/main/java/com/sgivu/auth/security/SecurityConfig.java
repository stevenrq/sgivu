package com.sgivu.auth.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.sgivu.auth.config.AngularClientProperties;
import com.sgivu.auth.config.IssuerProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

  private static final String LOGIN_PATH = "/login";

  private final IssuerProperties issuerProperties;
  private final AngularClientProperties angularClientProperties;
  private final JwtProperties jwtProperties;
  private final ResourceLoader resourceLoader;

  private final UserDetailsService userDetailsService;

  public SecurityConfig(
      IssuerProperties issuerProperties,
      AngularClientProperties angularClientProperties,
      JwtProperties jwtProperties,
      ResourceLoader resourceLoader,
      UserDetailsService userDetailsService) {
    this.issuerProperties = issuerProperties;
    this.angularClientProperties = angularClientProperties;
    this.jwtProperties = jwtProperties;
    this.resourceLoader = resourceLoader;
    this.userDetailsService = userDetailsService;
  }

  @Bean
  WebSecurityCustomizer webSecurityCustomizer() {
    return web -> web.ignoring().requestMatchers("/error");
  }

  @Bean
  @Order(1)
  SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
        new OAuth2AuthorizationServerConfigurer();

    http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
        .with(
            authorizationServerConfigurer,
            authorizationServer -> authorizationServer.oidc(Customizer.withDefaults()))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/error", LOGIN_PATH)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint(LOGIN_PATH),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
    return http.cors(Customizer.withDefaults()).build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, SessionRegistry sessionRegistry)
      throws Exception {
    http.authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                    .permitAll()
                    .requestMatchers("/.well-known/**")
                    .permitAll()
                    .requestMatchers("/api/validate-credentials", "/sso-logout")
                    .permitAll()
                    .requestMatchers(
                        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/validate-credentials", "/sso-logout"))
        .formLogin(
            formLogin ->
                formLogin
                    .loginPage(LOGIN_PATH)
                    .permitAll()
                    .defaultSuccessUrl(LOGIN_PATH, true)
                    .failureHandler(customAuthenticationFailureHandler()))
        .sessionManagement(session -> session.maximumSessions(5).sessionRegistry(sessionRegistry));
    return http.cors(Customizer.withDefaults()).build();
  }

  @Bean
  AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder().issuer(issuerProperties.getUrl()).build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");
    // Permitir Swagger UI servido por el gateway en localhost:8080 para pruebas locales
    config.setAllowedOriginPatterns(
        List.of(angularClientProperties.getUrl(), "http://localhost:8080"));
    config.setAllowCredentials(true);
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  JWKSource<SecurityContext> jwkSource() {
    try {
      String keyAlias = jwtProperties.key().alias();
      Assert.hasText(keyAlias, "El alias JWT no puede estar vacío.");
      Resource resource = resourceLoader.getResource(jwtProperties.keyStore().location());
      KeyStore keyStore = KeyStore.getInstance("JKS");
      keyStore.load(resource.getInputStream(), jwtProperties.keyStore().password().toCharArray());

      RSAPrivateKey privateKey =
          (RSAPrivateKey) keyStore.getKey(keyAlias, jwtProperties.key().password().toCharArray());

      Certificate certificate = keyStore.getCertificate(keyAlias);
      RSAPublicKey publicKey = (RSAPublicKey) certificate.getPublicKey();

      RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(keyAlias).build();

      JWKSet jwkSet = new JWKSet(rsaKey);
      return new ImmutableJWKSet<>(jwkSet);

    } catch (Exception ex) {
      throw new IllegalStateException(
          "Error al cargar el almacén de claves desde la ubicación y contraseña proporcionadas."
              + " Revise las propiedades JWT en su archivo de configuración.",
          ex);
    }
  }

  @Bean
  OAuth2TokenGenerator<?> tokenGenerator() {
    JwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource());
    JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
    jwtGenerator.setJwtCustomizer(jwtCustomizer(userDetailsService));
    OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
    OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
    return new DelegatingOAuth2TokenGenerator(
        jwtGenerator, accessTokenGenerator, refreshTokenGenerator);
  }

  @Bean
  OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer(UserDetailsService userDetailsService) {
    return context -> {
      JwtClaimsSet.Builder claims = context.getClaims();
      Authentication principal = context.getPrincipal();

      String keyAlias = jwtProperties.key().alias();
      if (StringUtils.hasText(keyAlias)) {
        context.getJwsHeader().keyId(keyAlias);
      }

      String username = principal.getName();
      CustomUserDetails customUserDetails =
          (CustomUserDetails) userDetailsService.loadUserByUsername(username);
      Long userId = customUserDetails.getId();
      Assert.notNull(userId, "El ID del usuario no debe ser nulo");

      Set<String> rolesAndPermissions =
          principal.getAuthorities().stream()
              .map(GrantedAuthority::getAuthority)
              .map(
                  roleOrPermission -> {
                    if (roleOrPermission.matches("^[A-Z_]+$")) {
                      return "ROLE_" + roleOrPermission;
                    }
                    return roleOrPermission;
                  })
              .collect(Collectors.toSet());

      if (context.getTokenType().equals(OAuth2TokenType.ACCESS_TOKEN)) {
        claims
            .claim("sub", userId)
            .claim("username", username)
            .claim("rolesAndPermissions", rolesAndPermissions)
            .claim("isAdmin", rolesAndPermissions.contains("ROLE_ADMIN"));

      } else if (context.getTokenType().getValue().equals(OidcParameterNames.ID_TOKEN)) {
        claims.claim("userId", userId);
        // El id_token debe tener un TTL >= al refresh_token, ya que el OidcUser (y su id_token)
        // nunca se actualiza durante el refresh de tokens en Spring Security. El id_token original
        // del login se usa como id_token_hint durante el RP-Initiated Logout (OIDC), por lo que
        // debe seguir siendo válido mientras la sesión esté activa.
        claims.expiresAt(Instant.now().plus(Duration.ofDays(30)));
      }
    };
  }

  @Bean
  SessionRegistry sessionRegistry(
      FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
    return new SpringSessionBackedSessionRegistry<>(sessionRepository);
  }

  @Bean
  HttpSessionEventPublisher httpSessionEventPublisher() {
    return new HttpSessionEventPublisher();
  }

  @Bean
  JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
  }

  /**
   * Manejador personalizado para fallos de autenticación en el form-login, que redirige a la página
   * de login con un parámetro de error específico según el tipo de excepción lanzada.
   */
  @Bean
  SimpleUrlAuthenticationFailureHandler customAuthenticationFailureHandler() {
    return new SimpleUrlAuthenticationFailureHandler() {

      @Override
      public void onAuthenticationFailure(
          HttpServletRequest request,
          HttpServletResponse response,
          AuthenticationException exception)
          throws IOException, ServletException {

        String errorParam =
            switch (exception) {
              case DisabledException disabledException -> "disabled";
              case LockedException lockedException -> "locked";
              case AccountExpiredException accountExpiredException -> "expired";
              case CredentialsExpiredException credentialsExpiredException -> "credentials";
              case BadCredentialsException badCredentialsException -> "bad_credentials";
              case InternalAuthenticationServiceException internalAuthenticationServiceException ->
                  "service_unavailable";
              case null, default -> "unknown";
            };

        setDefaultFailureUrl("/login?error=" + errorParam);
        super.onAuthenticationFailure(request, response, exception);
      }
    };
  }
}
