package com.sgivu.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sgivu.auth.entity.AuthorizationConsent;
import com.sgivu.auth.repository.AuthorizationConsentRepository;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

public class JpaOAuth2AuthorizationConsentServiceTest {

  @Mock private AuthorizationConsentRepository repo;

  @Mock private RegisteredClientRepository clients;

  @InjectMocks private JpaOAuth2AuthorizationConsentService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Nested
  @DisplayName("toEntity(OAuth2AuthorizationConsent)")
  class ToEntityTests {

    private AuthorizationConsent invokeToEntity(OAuth2AuthorizationConsent consent)
        throws Exception {
      Method m =
          JpaOAuth2AuthorizationConsentService.class.getDeclaredMethod(
              "toEntity", OAuth2AuthorizationConsent.class);
      m.setAccessible(true);
      return (AuthorizationConsent) m.invoke(service, consent);
    }

    @Test
    @DisplayName("Debe convertir OAuth2AuthorizationConsent a AuthorizationConsent con autoridades")
    void shouldConvertToEntityWithAuthorities() throws Exception {
      OAuth2AuthorizationConsent consent =
          OAuth2AuthorizationConsent.withId("client", "principal")
              .authority(new SimpleGrantedAuthority("read"))
              .authority(new SimpleGrantedAuthority("write"))
              .build();

      AuthorizationConsent entity = invokeToEntity(consent);

      assertEquals("client", entity.getRegisteredClientId());
      assertEquals("principal", entity.getPrincipalName());

      String auth = entity.getAuthorities();
      assertNotNull(auth);
      String[] parts = auth.split(",");
      Set<String> set = new HashSet<>(Arrays.asList(parts));
      assertTrue(set.contains("read"));
      assertTrue(set.contains("write"));
      assertEquals(2, set.size());
    }

    @Test
    @DisplayName("Debe convertir a entidad con una sola autoridad")
    void shouldConvertToEntityWithSingleAuthority() throws Exception {
      OAuth2AuthorizationConsent consent =
          OAuth2AuthorizationConsent.withId("c", "p")
              .authority(new SimpleGrantedAuthority("only"))
              .build();

      AuthorizationConsent entity = invokeToEntity(consent);

      assertEquals("c", entity.getRegisteredClientId());
      assertEquals("p", entity.getPrincipalName());
      assertNotNull(entity.getAuthorities());
      assertEquals("only", entity.getAuthorities());
    }
  }

  @Nested
  @DisplayName("toObject(AuthorizationConsent)")
  class ToObjectTests {

    private OAuth2AuthorizationConsent invokeToObject(AuthorizationConsent entity)
        throws Exception {
      Method m =
          JpaOAuth2AuthorizationConsentService.class.getDeclaredMethod(
              "toObject", AuthorizationConsent.class);
      m.setAccessible(true);
      return (OAuth2AuthorizationConsent) m.invoke(service, entity);
    }

    @Test
    @DisplayName("Debe convertir AuthorizationConsent a OAuth2AuthorizationConsent con autoridades")
    void shouldConvertToObjectWithAuthorities() throws Exception {
      AuthorizationConsent entity = new AuthorizationConsent();
      entity.setRegisteredClientId("client");
      entity.setPrincipalName("principal");
      entity.setAuthorities("read,write");

      RegisteredClient rc =
          RegisteredClient.withId("client")
              .clientId("client")
              .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
              .build();
      when(clients.findById("client")).thenReturn(rc);

      OAuth2AuthorizationConsent consent = invokeToObject(entity);

      assertEquals("client", consent.getRegisteredClientId());
      assertEquals("principal", consent.getPrincipalName());
      assertTrue(consent.getAuthorities().stream().anyMatch(a -> "read".equals(a.getAuthority())));
      assertTrue(consent.getAuthorities().stream().anyMatch(a -> "write".equals(a.getAuthority())));
    }

    @Test
    @DisplayName(
        "Debe lanzar DataRetrievalFailureException cuando el cliente registrado no se encuentra")
    void shouldThrowWhenRegisteredClientNotFound() throws Exception {
      AuthorizationConsent entity = new AuthorizationConsent();
      entity.setRegisteredClientId("missing");
      entity.setPrincipalName("p");

      when(clients.findById("missing")).thenReturn(null);

      InvocationTargetException ex =
          assertThrows(InvocationTargetException.class, () -> invokeToObject(entity));
      assertTrue(ex.getCause() instanceof DataRetrievalFailureException);
    }
  }
}
