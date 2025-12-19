package com.sgivu.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sgivu.auth.entity.AuthorizationConsent;
import com.sgivu.auth.repository.AuthorizationConsentRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

@ExtendWith(MockitoExtension.class)
class JpaOAuth2AuthorizationConsentServiceTest {

  @Mock private AuthorizationConsentRepository authorizationConsentRepository;

  @Mock private JpaRegisteredClientRepository registeredClientRepository;

  private JpaOAuth2AuthorizationConsentService service;

  @BeforeEach
  void setUp() {
    service =
        new JpaOAuth2AuthorizationConsentService(
            authorizationConsentRepository, registeredClientRepository);
  }

  @Test
  void save_persistsAuthoritiesAndPrincipal() {
    OAuth2AuthorizationConsent consent =
        OAuth2AuthorizationConsent.withId("client-1", "alice")
            .authority(new SimpleGrantedAuthority("ROLE_USER"))
            .authority(new SimpleGrantedAuthority("inventory:read"))
            .build();

    service.save(consent);

    ArgumentCaptor<AuthorizationConsent> captor =
        ArgumentCaptor.forClass(AuthorizationConsent.class);
    verify(authorizationConsentRepository).save(captor.capture());
    AuthorizationConsent stored = captor.getValue();

    assertEquals("client-1", stored.getRegisteredClientId());
    assertEquals("alice", stored.getPrincipalName());
    assertTrue(
        stored.getAuthorities().contains("ROLE_USER")
            && stored.getAuthorities().contains("inventory:read"));
  }

  @Test
  void findById_whenRegisteredClientMissing_throwsDataRetrievalFailureException() {
    AuthorizationConsent consentEntity = new AuthorizationConsent();
    consentEntity.setRegisteredClientId("ghost-client");
    consentEntity.setPrincipalName("bob");
    consentEntity.setAuthorities("ROLE_USER");

    when(authorizationConsentRepository.findByRegisteredClientIdAndPrincipalName(
            "ghost-client", "bob"))
        .thenReturn(Optional.of(consentEntity));
    when(registeredClientRepository.findById("ghost-client")).thenReturn(null);

    assertThrows(
        DataRetrievalFailureException.class, () -> service.findById("ghost-client", "bob"));
    verify(registeredClientRepository).findById("ghost-client");
  }

  @Test
  void findById_whenRegisteredClientExists_returnsConsentWithAuthorities() {
    AuthorizationConsent consentEntity = new AuthorizationConsent();
    consentEntity.setRegisteredClientId("client-ok");
    consentEntity.setPrincipalName("carol");
    consentEntity.setAuthorities("ROLE_USER,inventory:read");

    when(authorizationConsentRepository.findByRegisteredClientIdAndPrincipalName(
            "client-ok", "carol"))
        .thenReturn(Optional.of(consentEntity));
    RegisteredClient registeredClient = registeredClient("client-ok");
    when(registeredClientRepository.findById("client-ok")).thenReturn(registeredClient);

    OAuth2AuthorizationConsent result = service.findById("client-ok", "carol");

    assertEquals("client-ok", result.getRegisteredClientId());
    assertEquals("carol", result.getPrincipalName());
    assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("inventory:read")));
  }

  private RegisteredClient registeredClient(String id) {
    return RegisteredClient.withId(id)
        .clientId(id)
        .clientSecret("secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("https://app/callback")
        .clientSettings(ClientSettings.builder().build())
        .tokenSettings(TokenSettings.builder().build())
        .build();
  }
}
