package com.sgivu.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgivu.auth.entity.Client;
import com.sgivu.auth.repository.ClientRepository;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

public class JpaRegisteredClientRepositoryTest {

  @Mock private ClientRepository clientRepository;

  @InjectMocks private JpaRegisteredClientRepository repository;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Nested
  @DisplayName("toEntity(RegisteredClient)")
  class ToEntityTests {

    private Client invokeToEntity(RegisteredClient rc) throws Exception {
      Method m =
          JpaRegisteredClientRepository.class.getDeclaredMethod("toEntity", RegisteredClient.class);
      m.setAccessible(true);
      return (Client) m.invoke(repository, rc);
    }

    @Test
    @DisplayName("Debe convertir RegisteredClient a entidad Client con todos los campos")
    void shouldConvertRegisteredClientToEntityFull() throws Exception {
      Instant now = Instant.now();

      Map<String, Object> clientSettings = new HashMap<>();
      clientSettings.put("require_auth_time", true);
      Map<String, Object> tokenSettings = new HashMap<>();
      tokenSettings.put("access_token_time_to_live", 3600);

      RegisteredClient rc =
          RegisteredClient.withId("id-1")
              .clientId("client-id")
              .clientIdIssuedAt(now)
              .clientSecret("secret")
              .clientSecretExpiresAt(now.plusSeconds(3600))
              .clientName("My Client")
              .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .redirectUri("https://app.example.com/callback")
              .postLogoutRedirectUri("https://app.example.com/logout")
              .scope("read")
              .scope("write")
              .clientSettings(ClientSettings.withSettings(clientSettings).build())
              .tokenSettings(TokenSettings.withSettings(tokenSettings).build())
              .build();

      Client entity = invokeToEntity(rc);

      assertEquals("id-1", entity.getId());
      assertEquals("client-id", entity.getClientId());
      assertEquals(now, entity.getClientIdIssuedAt());
      assertEquals("secret", entity.getClientSecret());
      assertEquals("My Client", entity.getClientName());

      // listas separadas por comas - comprobar contenido
      assertTrue(entity.getClientAuthenticationMethods().contains("client_secret_basic"));
      assertTrue(entity.getAuthorizationGrantTypes().contains("authorization_code"));
      assertTrue(entity.getRedirectUris().contains("https://app.example.com/callback"));
      assertTrue(entity.getPostLogoutRedirectUris().contains("https://app.example.com/logout"));
      assertTrue(entity.getScopes().contains("read"));
      assertTrue(entity.getScopes().contains("write"));

      // Las configuraciones del cliente y del token deben ser cadenas JSON que contengan nuestras
      // claves
      ObjectMapper om = new ObjectMapper();
      Map<?, ?> cs = om.readValue(entity.getClientSettings(), Map.class);
      assertEquals(true, cs.get("require_auth_time"));

      Map<?, ?> ts = om.readValue(entity.getTokenSettings(), Map.class);
      assertEquals(3600, ts.get("access_token_time_to_live"));
    }

    @Test
    @DisplayName("Debe convertir RegisteredClient a entidad Client con campos mínimos")
    void shouldConvertRegisteredClientToEntityMinimal() throws Exception {
      RegisteredClient rc =
          RegisteredClient.withId("id-2")
              .clientId("cid")
              .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
              .build();

      Client entity = invokeToEntity(rc);

      assertEquals("id-2", entity.getId());
      assertEquals("cid", entity.getClientId());
      assertNotNull(entity.getClientSettings());
      assertNotNull(entity.getTokenSettings());
    }
  }

  @Nested
  @DisplayName("toObject(Client)")
  class ToObjectTests {

    private RegisteredClient invokeToObject(Client client) throws Exception {
      Method m = JpaRegisteredClientRepository.class.getDeclaredMethod("toObject", Client.class);
      m.setAccessible(true);
      return (RegisteredClient) m.invoke(repository, client);
    }

    @Test
    @DisplayName("Debe convertir entidad Client a RegisteredClient con todos los campos")
    void shouldConvertClientEntityToRegisteredClientFull() throws Exception {
      Client client = new Client();
      Instant now = Instant.now();
      client.setId("id-1");
      client.setClientId("client-id");
      client.setClientIdIssuedAt(now);
      client.setClientSecret("secret");
      client.setClientSecretExpiresAt(now.plusSeconds(3600));
      client.setClientName("My Client");
      client.setClientAuthenticationMethods("client_secret_basic,client_secret_post");
      client.setAuthorizationGrantTypes("authorization_code,refresh_token");
      client.setRedirectUris("https://app.example.com/callback");
      client.setPostLogoutRedirectUris("https://app.example.com/logout");
      client.setScopes("read,write");

      Map<String, Object> clientSettings = new HashMap<>();
      clientSettings.put("require_auth_time", true);
      Map<String, Object> tokenSettings = new HashMap<>();
      tokenSettings.put("access_token_time_to_live", 3600);

      // Utilice el writeMap propio del repositorio (ObjectMapper configurado) para producir JSON
      // compatible
      Method writeMap =
          JpaRegisteredClientRepository.class.getDeclaredMethod("writeMap", java.util.Map.class);
      writeMap.setAccessible(true);
      String clientSettingsJson = (String) writeMap.invoke(repository, clientSettings);
      String tokenSettingsJson = (String) writeMap.invoke(repository, tokenSettings);
      client.setClientSettings(clientSettingsJson);
      client.setTokenSettings(tokenSettingsJson);

      RegisteredClient rc = invokeToObject(client);

      assertEquals("id-1", rc.getId());
      assertEquals("client-id", rc.getClientId());
      assertEquals(now, rc.getClientIdIssuedAt());
      assertEquals("secret", rc.getClientSecret());
      assertEquals("My Client", rc.getClientName());

      assertTrue(
          rc.getClientAuthenticationMethods().stream()
              .anyMatch(
                  mth ->
                      ClientAuthenticationMethod.CLIENT_SECRET_BASIC
                          .getValue()
                          .equals(mth.getValue())));
      assertTrue(
          rc.getAuthorizationGrantTypes().stream()
              .anyMatch(
                  gt ->
                      AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(gt.getValue())));
      assertTrue(rc.getRedirectUris().contains("https://app.example.com/callback"));
      assertTrue(rc.getPostLogoutRedirectUris().contains("https://app.example.com/logout"));
      assertTrue(rc.getScopes().contains("read"));
      assertTrue(rc.getScopes().contains("write"));

      assertEquals(true, rc.getClientSettings().getSettings().get("require_auth_time"));
      assertEquals(3600, rc.getTokenSettings().getSettings().get("access_token_time_to_live"));
    }

    @Test
    @DisplayName(
        "Debe lanzar IllegalArgumentException cuando el JSON de clientSettings es inválido")
    void shouldThrowWhenClientSettingsInvalid() throws Exception {
      Client client = new Client();
      client.setId("id-x");
      client.setClientId("cid");
      client.setAuthorizationGrantTypes("client_credentials");
      client.setClientSettings("not-json");
      client.setTokenSettings("also-not-json");

      InvocationTargetException ex =
          assertThrows(InvocationTargetException.class, () -> invokeToObject(client));
      assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }
  }

  @Nested
  @DisplayName("resolveAuthorizationGrantType(String)")
  class ResolveAuthorizationGrantTypeTests {

    private AuthorizationGrantType invokeResolve(String value) throws Exception {
      Method m =
          JpaRegisteredClientRepository.class.getDeclaredMethod(
              "resolveAuthorizationGrantType", String.class);
      m.setAccessible(true);
      return (AuthorizationGrantType) m.invoke(null, value);
    }

    @Test
    @DisplayName("Debe retornar constante AuthorizationCode para 'authorization_code'")
    void shouldReturnAuthorizationCodeConstant() throws Exception {
      var result = invokeResolve(AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
      assertSame(AuthorizationGrantType.AUTHORIZATION_CODE, result);
    }

    @Test
    @DisplayName("Debe retornar constante ClientCredentials para 'client_credentials'")
    void shouldReturnClientCredentialsConstant() throws Exception {
      var result = invokeResolve(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
      assertSame(AuthorizationGrantType.CLIENT_CREDENTIALS, result);
    }

    @Test
    @DisplayName("Debe retornar constante RefreshToken para 'refresh_token'")
    void shouldReturnRefreshTokenConstant() throws Exception {
      var result = invokeResolve(AuthorizationGrantType.REFRESH_TOKEN.getValue());
      assertSame(AuthorizationGrantType.REFRESH_TOKEN, result);
    }

    @Test
    @DisplayName("Debe retornar AuthorizationGrantType personalizado para valor desconocido")
    void shouldReturnCustomGrantType() throws Exception {
      String custom = "urn:example:grant-type:custom";
      var result = invokeResolve(custom);
      assertNotNull(result);
      assertEquals(custom, result.getValue());
      assertNotSame(AuthorizationGrantType.AUTHORIZATION_CODE, result);
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException cuando la entrada es nula")
    void shouldThrowWhenInputNull() throws Exception {
      InvocationTargetException ex =
          assertThrows(InvocationTargetException.class, () -> invokeResolve(null));
      assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException cuando la entrada es una cadena vacía")
    void shouldThrowWhenEmptyString() throws Exception {
      InvocationTargetException ex =
          assertThrows(InvocationTargetException.class, () -> invokeResolve(""));
      assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }
  }

  @Nested
  @DisplayName("resolveClientAuthenticationMethod(String)")
  class ResolveClientAuthenticationMethodTests {

    private ClientAuthenticationMethod invokeResolve(String value) throws Exception {
      Method m =
          JpaRegisteredClientRepository.class.getDeclaredMethod(
              "resolveClientAuthenticationMethod", String.class);
      m.setAccessible(true);
      return (ClientAuthenticationMethod) m.invoke(null, value);
    }

    @Test
    @DisplayName("Debe retornar CLIENT_SECRET_BASIC cuando la entrada es 'client_secret_basic'")
    void shouldReturnClientSecretBasic() throws Exception {
      var result = invokeResolve(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue());
      assertSame(ClientAuthenticationMethod.CLIENT_SECRET_BASIC, result);
    }

    @Test
    @DisplayName("Debe retornar CLIENT_SECRET_POST cuando la entrada es 'client_secret_post'")
    void shouldReturnClientSecretPost() throws Exception {
      var result = invokeResolve(ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue());
      assertSame(ClientAuthenticationMethod.CLIENT_SECRET_POST, result);
    }

    @Test
    @DisplayName("Debe retornar NONE cuando la entrada es 'none'")
    void shouldReturnNone() throws Exception {
      var result = invokeResolve(ClientAuthenticationMethod.NONE.getValue());
      assertSame(ClientAuthenticationMethod.NONE, result);
    }

    @Test
    @DisplayName("Debe retornar ClientAuthenticationMethod personalizado para valor desconocido")
    void shouldReturnCustom() throws Exception {
      String custom = "custom_method";
      var result = invokeResolve(custom);
      assertNotNull(result);
      assertEquals(custom, result.getValue());
      assertNotSame(ClientAuthenticationMethod.CLIENT_SECRET_BASIC, result);
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException cuando la entrada es nula")
    void shouldThrowWhenInputNull() throws Exception {
      InvocationTargetException ex =
          assertThrows(InvocationTargetException.class, () -> invokeResolve(null));
      assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException cuando la entrada es una cadena vacía")
    void shouldThrowWhenEmptyString() throws Exception {
      InvocationTargetException ex =
          assertThrows(InvocationTargetException.class, () -> invokeResolve(""));
      assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }
  }
}
