package com.sgivu.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sgivu.auth.entity.Client;
import com.sgivu.auth.repository.ClientRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

@ExtendWith(MockitoExtension.class)
class JpaRegisteredClientRepositoryTest {

  @Mock private ClientRepository clientRepository;

  private JpaRegisteredClientRepository repository;

  @BeforeEach
  void setUp() {
    repository = new JpaRegisteredClientRepository(clientRepository);
  }

  @Test
  void save_convertsRegisteredClientToEntity() {
    ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);

    RegisteredClient registeredClient = registeredClient();
    repository.save(registeredClient);

    verify(clientRepository).save(captor.capture());
    Client entity = captor.getValue();

    assertEquals(registeredClient.getId(), entity.getId());
    assertTrue(entity.getClientAuthenticationMethods().contains("client_secret_post"));
    assertTrue(entity.getAuthorizationGrantTypes().contains("refresh_token"));

    String clientSettingsJson = entity.getClientSettings();
    assertTrue(clientSettingsJson.contains("settings.client.require-authorization-consent"));
    assertTrue(clientSettingsJson.contains("setting.custom"));

    String tokenSettingsJson = entity.getTokenSettings();
    assertTrue(tokenSettingsJson.contains("settings.token.reuse-refresh-tokens"));
    assertTrue(tokenSettingsJson.contains("settings.token.access-token-time-to-live"));
  }

  @Test
  void findByClientId_rebuildsRegisteredClientWithSettings() {
    ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
    when(clientRepository.save(any(Client.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RegisteredClient original = registeredClient();
    repository.save(original);
    verify(clientRepository).save(captor.capture());
    Client persisted = captor.getValue();

    when(clientRepository.findByClientId(original.getClientId()))
        .thenReturn(Optional.of(persisted));

    RegisteredClient rebuilt = repository.findByClientId(original.getClientId());

    assertNotNull(rebuilt);
    assertEquals(original.getClientId(), rebuilt.getClientId());
    assertEquals(
        original.getAuthorizationGrantTypes().size(), rebuilt.getAuthorizationGrantTypes().size());
    assertEquals(
        original.getClientSettings().isRequireAuthorizationConsent(),
        rebuilt.getClientSettings().isRequireAuthorizationConsent());
    assertEquals(
        original.getTokenSettings().getAccessTokenTimeToLive(),
        rebuilt.getTokenSettings().getAccessTokenTimeToLive());
    assertTrue(rebuilt.getScopes().containsAll(Set.of("openid", "inventory:read")));
  }

  private RegisteredClient registeredClient() {
    return RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId("test-client")
        .clientIdIssuedAt(Instant.parse("2024-01-01T00:00:00Z"))
        .clientSecret("{noop}secret")
        .clientSecretExpiresAt(Instant.parse("2024-12-31T00:00:00Z"))
        .clientName("Test Client")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("https://app/redirect")
        .postLogoutRedirectUri("https://app/logout")
        .scope("openid")
        .scope("inventory:read")
        .clientSettings(
            ClientSettings.builder()
                .requireAuthorizationConsent(true)
                .setting("setting.custom", "value")
                .build())
        .tokenSettings(
            TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(30))
                .reuseRefreshTokens(false)
                .build())
        .build();
  }
}
