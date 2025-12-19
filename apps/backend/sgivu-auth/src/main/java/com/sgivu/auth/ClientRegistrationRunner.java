package com.sgivu.auth;

import com.sgivu.auth.config.AngularClientProperties;
import com.sgivu.auth.service.JpaRegisteredClientRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

/**
 * Siembra los clientes OIDC que usan el portal Angular, entornos locales y herramientas de QA en el
 * arranque del Authorization Server, persistiendo la configuración en {@link
 * JpaRegisteredClientRepository}.
 */
@Component
public class ClientRegistrationRunner implements CommandLineRunner {

  private static final Logger logger = LoggerFactory.getLogger(ClientRegistrationRunner.class);
  private final JpaRegisteredClientRepository jpaRegisteredClientRepository;
  private final PasswordEncoder passwordEncoder;
  private final AngularClientProperties angularClientProperties;

  public ClientRegistrationRunner(
      JpaRegisteredClientRepository registeredClientRepository,
      PasswordEncoder passwordEncoder,
      AngularClientProperties angularClientProperties) {
    this.jpaRegisteredClientRepository = registeredClientRepository;
    this.passwordEncoder = passwordEncoder;
    this.angularClientProperties = angularClientProperties;
  }

  @Override
  public void run(String... args) throws Exception {

    if (this.jpaRegisteredClientRepository.findByClientId("angular-client") == null) {
      String angularClientUrl = angularClientProperties.getUrl();
      logger.info("Registrando el cliente 'angular-client'...");
      RegisteredClient angularClient =
          RegisteredClient.withId(UUID.randomUUID().toString())
              .clientId("angular-client")
              .clientIdIssuedAt(Instant.now())
              .clientName("Cliente público Angular")
              .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
              .redirectUri(angularClientUrl.concat("/callback"))
              .postLogoutRedirectUri(angularClientUrl.concat("/login"))
              .scope(OidcScopes.OPENID)
              .scope(OidcScopes.PROFILE)
              .scope(OidcScopes.EMAIL)
              .scope(OidcScopes.PHONE)
              .scope(OidcScopes.ADDRESS)
              .scope("offline_access")
              .scope("api")
              .scope("read")
              .scope("write")
              .tokenSettings(tokenSettings())
              .clientSettings(
                  ClientSettings.builder()
                      .requireAuthorizationConsent(true)
                      .requireProofKey(true)
                      .build())
              .build();

      this.jpaRegisteredClientRepository.save(angularClient);

      logger.info("Cliente 'angular-client' registrado correctamente.");
    } else {
      logger.info("El cliente 'angular-client' ya está registrado.");
    }

    if (this.jpaRegisteredClientRepository.findByClientId("angular-local") == null
        && this.angularClientProperties.getUrl().startsWith("http://localhost")) {
      logger.info("Registrando el cliente 'angular-local'...");
      RegisteredClient angularLocal =
          RegisteredClient.withId(UUID.randomUUID().toString())
              .clientId("angular-local")
              .clientIdIssuedAt(Instant.now())
              .clientName("Cliente Angular Local")
              .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
              .redirectUri("http://localhost:4200/callback")
              .postLogoutRedirectUri("http://localhost:4200/login")
              .scope(OidcScopes.OPENID)
              .scope(OidcScopes.PROFILE)
              .scope(OidcScopes.EMAIL)
              .scope(OidcScopes.PHONE)
              .scope(OidcScopes.ADDRESS)
              .scope("offline_access")
              .scope("api")
              .scope("read")
              .scope("write")
              .tokenSettings(tokenSettings())
              .clientSettings(
                  ClientSettings.builder()
                      .requireAuthorizationConsent(true)
                      .requireProofKey(true)
                      .build())
              .build();

      this.jpaRegisteredClientRepository.save(angularLocal);
      logger.info("Cliente 'angular-local' registrado correctamente.");
    } else {
      logger.info("El cliente 'angular-local' ya está registrado.");
    }

    if (this.jpaRegisteredClientRepository.findByClientId("postman-client") == null) {
      logger.info("Registrando el cliente 'postman-client'...");
      RegisteredClient postmanClient =
          RegisteredClient.withId(UUID.randomUUID().toString())
              .clientId("postman-client")
              .clientIdIssuedAt(Instant.now())
              .clientName("Cliente confidencial Postman")
              .clientSecret(passwordEncoder.encode("postman-secret"))
              .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
              .redirectUri("https://oauth.pstmn.io/v1/callback")
              .scope(OidcScopes.OPENID)
              .scope("api")
              .tokenSettings(tokenSettings())
              .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
              .build();
      this.jpaRegisteredClientRepository.save(postmanClient);
      logger.info("Cliente 'postman-client' registrado correctamente.");
    } else {
      logger.info("El cliente 'postman-client' ya está registrado.");
    }

    if (this.jpaRegisteredClientRepository.findByClientId("oauth2-debugger-client") == null) {
      logger.info("Registrando el cliente 'oauth2-debugger-client'...");
      RegisteredClient postmanClient =
          RegisteredClient.withId(UUID.randomUUID().toString())
              .clientId("oauth2-debugger-client")
              .clientIdIssuedAt(Instant.now())
              .clientName("Cliente confidencial OAuth2 Debugger")
              .clientSecret(passwordEncoder.encode("oauth2-debugger-secret"))
              .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
              .redirectUri("https://oauthdebugger.com/debug")
              .scope(OidcScopes.OPENID)
              .scope("api")
              .tokenSettings(tokenSettings())
              .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
              .build();
      this.jpaRegisteredClientRepository.save(postmanClient);
      logger.info("Cliente 'oauth2-debugger-client' registrado correctamente.");
    } else {
      logger.info("El cliente 'oauth2-debugger-client' ya está registrado.");
    }
  }

  private TokenSettings tokenSettings() {
    return TokenSettings.builder()
        // TTL altos facilitan los flujos de venta; reducirlos si se habilita rotación de refresh
        // tokens para limitar exposición.
        .accessTokenTimeToLive(Duration.ofDays(1))
        .refreshTokenTimeToLive(Duration.ofDays(30))
        .build();
  }
}
