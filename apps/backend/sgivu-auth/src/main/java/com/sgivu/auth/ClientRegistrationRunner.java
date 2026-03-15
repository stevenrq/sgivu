package com.sgivu.auth;

import com.sgivu.auth.config.AngularClientProperties;
import com.sgivu.auth.config.GatewayClientProperties;
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

/** Registra clientes OAuth2 en la base de datos al iniciar la aplicación si no existen. */
@Component
public class ClientRegistrationRunner implements CommandLineRunner {

  private static final Logger logger = LoggerFactory.getLogger(ClientRegistrationRunner.class);
  private static final String GATEWAY_CLIENT_ID = "sgivu-gateway";
  private final JpaRegisteredClientRepository jpaRegisteredClientRepository;
  private final PasswordEncoder passwordEncoder;
  private final AngularClientProperties angularClientProperties;
  private final GatewayClientProperties gatewayClientProperties;

  public ClientRegistrationRunner(
      JpaRegisteredClientRepository registeredClientRepository,
      PasswordEncoder passwordEncoder,
      AngularClientProperties angularClientProperties,
      GatewayClientProperties gatewayClientProperties) {
    this.jpaRegisteredClientRepository = registeredClientRepository;
    this.passwordEncoder = passwordEncoder;
    this.angularClientProperties = angularClientProperties;
    this.gatewayClientProperties = gatewayClientProperties;
  }

  @Override
  public void run(String... args) throws Exception {

    if (this.jpaRegisteredClientRepository.findByClientId(GATEWAY_CLIENT_ID) == null) {
      String gatewayUrl = gatewayClientProperties.getUrl();
      logger.info("Registering client '{}'...", GATEWAY_CLIENT_ID);
      RegisteredClient gatewayClient =
          RegisteredClient.withId(UUID.randomUUID().toString())
              .clientId(GATEWAY_CLIENT_ID)
              .clientIdIssuedAt(Instant.now())
              .clientName("Gateway")
              .clientSecret(passwordEncoder.encode(gatewayClientProperties.getSecret()))
              .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
              .redirectUri(gatewayUrl.concat("/login/oauth2/code/").concat(GATEWAY_CLIENT_ID))
              .postLogoutRedirectUri(angularClientProperties.getUrl().concat("/login"))
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

      this.jpaRegisteredClientRepository.save(gatewayClient);
      logger.info("Client '{}' registered successfully.", GATEWAY_CLIENT_ID);
    } else {
      logger.info("Client '{}' is already registered.", GATEWAY_CLIENT_ID);
    }

    if (this.jpaRegisteredClientRepository.findByClientId("postman-client") == null) {
      logger.info("Registering client 'postman-client'...");
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
              .scope("offline_access")
              .scope("api")
              .tokenSettings(tokenSettings())
              .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
              .build();
      this.jpaRegisteredClientRepository.save(postmanClient);
      logger.info("Client 'postman-client' registered successfully.");
    } else {
      logger.info("Client 'postman-client' is already registered.");
    }

    if (this.jpaRegisteredClientRepository.findByClientId("oauth2-debugger-client") == null) {
      logger.info("Registering client 'oauth2-debugger-client'...");
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
              .scope("offline_access")
              .scope("api")
              .tokenSettings(tokenSettings())
              .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
              .build();
      this.jpaRegisteredClientRepository.save(postmanClient);
      logger.info("Client 'oauth2-debugger-client' registered successfully.");
    } else {
      logger.info("Client 'oauth2-debugger-client' is already registered.");
    }
  }

  private TokenSettings tokenSettings() {
    return TokenSettings.builder()
        .accessTokenTimeToLive(Duration.ofMinutes(30))
        .refreshTokenTimeToLive(Duration.ofDays(30))
        .reuseRefreshTokens(false)
        .build();
  }
}
