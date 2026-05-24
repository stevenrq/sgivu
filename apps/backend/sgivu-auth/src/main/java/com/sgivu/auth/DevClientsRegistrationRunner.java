package com.sgivu.auth;

import com.sgivu.auth.service.JpaRegisteredClientRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Component;

/** Registra clientes OAuth2 de prueba (Postman, OAuth2 Debugger) solo en el perfil dev. */
@Component
@Profile("dev")
public class DevClientsRegistrationRunner implements CommandLineRunner {

  private static final Logger logger = LoggerFactory.getLogger(DevClientsRegistrationRunner.class);
  private final JpaRegisteredClientRepository jpaRegisteredClientRepository;
  private final PasswordEncoder passwordEncoder;

  public DevClientsRegistrationRunner(
      JpaRegisteredClientRepository jpaRegisteredClientRepository,
      PasswordEncoder passwordEncoder) {
    this.jpaRegisteredClientRepository = jpaRegisteredClientRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(String... args) throws Exception {
    registerPostmanClient();
    registerOAuth2DebuggerClient();
  }

  private void registerPostmanClient() {
    if (this.jpaRegisteredClientRepository.findByClientId("postman-client") != null) {
      logger.info("Client 'postman-client' is already registered.");
      return;
    }
    logger.info("Registering dev client 'postman-client'...");
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
            .tokenSettings(ClientRegistrationRunner.tokenSettings())
            .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
            .build();
    this.jpaRegisteredClientRepository.save(postmanClient);
    logger.info("Dev client 'postman-client' registered successfully.");
  }

  private void registerOAuth2DebuggerClient() {
    if (this.jpaRegisteredClientRepository.findByClientId("oauth2-debugger-client") != null) {
      logger.info("Client 'oauth2-debugger-client' is already registered.");
      return;
    }
    logger.info("Registering dev client 'oauth2-debugger-client'...");
    RegisteredClient debuggerClient =
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
            .tokenSettings(ClientRegistrationRunner.tokenSettings())
            .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
            .build();
    this.jpaRegisteredClientRepository.save(debuggerClient);
    logger.info("Dev client 'oauth2-debugger-client' registered successfully.");
  }
}
