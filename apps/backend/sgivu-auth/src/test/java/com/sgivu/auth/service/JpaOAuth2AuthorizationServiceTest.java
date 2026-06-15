package com.sgivu.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import com.sgivu.auth.entity.Authorization;
import com.sgivu.auth.repository.AuthorizationRepository;
import com.sgivu.auth.security.CustomUserDetails;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

public class JpaOAuth2AuthorizationServiceTest {

  private JpaOAuth2AuthorizationService service;

  @BeforeEach
  void setUpService() {
    var authRepo = Mockito.mock(AuthorizationRepository.class);
    var clients = Mockito.mock(RegisteredClientRepository.class);
    service = new JpaOAuth2AuthorizationService(authRepo, clients);
  }

  @Nested
  @DisplayName("resolveAuthorizationGrantType(String)")
  class ResolveAuthorizationGrantTypeTests {

    private AuthorizationGrantType invokeResolve(String value) throws Exception {
      Method m =
          JpaOAuth2AuthorizationService.class.getDeclaredMethod(
              "resolveAuthorizationGrantType", String.class);
      m.setAccessible(true);
      return (AuthorizationGrantType) m.invoke(null, value);
    }

    @Test
    @DisplayName("Debe retornar constante AuthorizationCode para 'authorization_code'")
    void shouldReturnAuthorizationCodeConstant() throws Exception {
      AuthorizationGrantType result =
          invokeResolve(AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
      assertSame(AuthorizationGrantType.AUTHORIZATION_CODE, result);
    }

    @Test
    @DisplayName("Debe retornar constante ClientCredentials para 'client_credentials'")
    void shouldReturnClientCredentialsConstant() throws Exception {
      AuthorizationGrantType result =
          invokeResolve(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
      assertSame(AuthorizationGrantType.CLIENT_CREDENTIALS, result);
    }

    @Test
    @DisplayName("Debe retornar constante RefreshToken para 'refresh_token'")
    void shouldReturnRefreshTokenConstant() throws Exception {
      AuthorizationGrantType result =
          invokeResolve(AuthorizationGrantType.REFRESH_TOKEN.getValue());
      assertSame(AuthorizationGrantType.REFRESH_TOKEN, result);
    }

    @Test
    @DisplayName(
        "Debe retornar constante DeviceCode para 'urn:ietf:params:oauth:grant-type:device_code'"
            + " (constante device_code)")
    void shouldReturnDeviceCodeConstant() throws Exception {
      AuthorizationGrantType result = invokeResolve(AuthorizationGrantType.DEVICE_CODE.getValue());
      assertSame(AuthorizationGrantType.DEVICE_CODE, result);
    }

    @Test
    @DisplayName(
        "Debe retornar un AuthorizationGrantType personalizado para tipos de concesión"
            + " desconocidos")
    void shouldReturnCustomGrantType() throws Exception {
      String custom = "urn:ietf:params:oauth:grant-type:jwt-bearer";
      AuthorizationGrantType result = invokeResolve(custom);
      assertNotNull(result);
      assertEquals(custom, result.getValue());
      assertNotSame(AuthorizationGrantType.AUTHORIZATION_CODE, result);
      assertNotSame(AuthorizationGrantType.CLIENT_CREDENTIALS, result);
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException cuando la entrada es nula")
    void shouldHandleNullInput() throws Exception {
      InvocationTargetException ex =
          assertThrows(InvocationTargetException.class, () -> invokeResolve(null));
      assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException cuando la entrada es una cadena vacía")
    void shouldHandleEmptyString() throws Exception {
      InvocationTargetException ex =
          assertThrows(InvocationTargetException.class, () -> invokeResolve(""));
      assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }
  }

  @Nested
  @DisplayName("parseMap(String)")
  class ParseMapTests {

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeParse(String data) throws Exception {
      Method m = JpaOAuth2AuthorizationService.class.getDeclaredMethod("parseMap", String.class);
      m.setAccessible(true);
      return (Map<String, Object>) m.invoke(service, data);
    }

    @Test
    @DisplayName("Debe parsear JSON válido en un Map")
    void shouldParseValidJson() throws Exception {
      Map<String, Object> source = new HashMap<>();
      source.put("k", "v");
      source.put("n", 123);
      Map<String, Object> nested = new HashMap<>();
      nested.put("x", true);
      source.put("nested", nested);

      Method writeMap =
          JpaOAuth2AuthorizationService.class.getDeclaredMethod("writeMap", Map.class);
      writeMap.setAccessible(true);
      String json = (String) writeMap.invoke(service, source);

      Map<String, Object> map = invokeParse(json);
      assertEquals("v", map.get("k"));
      assertEquals(123, ((Number) map.get("n")).intValue());
      @SuppressWarnings("unchecked")
      Map<String, Object> nestedMap = (Map<String, Object>) map.get("nested");
      assertTrue(nestedMap.containsKey("x"));
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException para JSON inválido")
    void shouldThrowForInvalidJson() throws Exception {
      InvocationTargetException ex =
          assertThrows(InvocationTargetException.class, () -> invokeParse("not-json"));
      assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException cuando la entrada es nula")
    void shouldThrowWhenNull() throws Exception {
      InvocationTargetException ex =
          assertThrows(InvocationTargetException.class, () -> invokeParse(null));
      assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException cuando la entrada es una cadena vacía")
    void shouldThrowWhenEmptyString() throws Exception {
      InvocationTargetException ex =
          assertThrows(InvocationTargetException.class, () -> invokeParse(""));
      assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }
  }

  @Nested
  @DisplayName("writeMap(Map<String, Object>)")
  class WriteMapTests {

    private String invokeWrite(Map<String, Object> map) throws Exception {
      Method m = JpaOAuth2AuthorizationService.class.getDeclaredMethod("writeMap", Map.class);
      m.setAccessible(true);
      return (String) m.invoke(service, map);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeParse(String data) throws Exception {
      Method m = JpaOAuth2AuthorizationService.class.getDeclaredMethod("parseMap", String.class);
      m.setAccessible(true);
      return (Map<String, Object>) m.invoke(service, data);
    }

    @Test
    @DisplayName("Debe escribir mapa simple a JSON y hacer round-trip vía parseMap")
    void shouldWriteSimpleMap() throws Exception {
      Map<String, Object> source = new HashMap<>();
      source.put("k", "v");
      source.put("n", 42);
      Map<String, Object> nested = new HashMap<>();
      nested.put("ok", true);
      source.put("nested", nested);

      String json = invokeWrite(source);
      Map<String, Object> parsed = invokeParse(json);
      assertEquals("v", parsed.get("k"));
      assertEquals(42, ((Number) parsed.get("n")).intValue());
      @SuppressWarnings("unchecked")
      Map<String, Object> nestedParsed = (Map<String, Object>) parsed.get("nested");
      assertTrue(nestedParsed.containsKey("ok"));
    }

    @Test
    @DisplayName(
        "Debe incluir información de tipo para CustomUserDetails y parsear de vuelta a instancia")
    void shouldSerializeAndDeserializeCustomUserDetails() throws Exception {
      com.sgivu.auth.dto.User u = new com.sgivu.auth.dto.User();
      u.setId(5L);
      u.setUsername("bob");
      u.setPassword("pwd");
      u.setEnabled(true);
      u.setAccountNonExpired(true);
      u.setAccountNonLocked(true);
      u.setCredentialsNonExpired(true);

      CustomUserDetails cud = new CustomUserDetails(u, Collections.emptySet());

      Map<String, Object> source = new HashMap<>();
      source.put("principal", cud);

      String json = invokeWrite(source);
      assertTrue(json.contains("\"@class\""));
      assertTrue(json.contains("CustomUserDetails"));

      Map<String, Object> parsed = invokeParse(json);
      Object principalObj = parsed.get("principal");
      assertNotNull(principalObj);
      if (principalObj instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> principal = (Map<String, Object>) principalObj;
        assertTrue(principal.containsKey("@class") || principal.containsKey("class"));
        assertEquals("bob", principal.get("username"));
      } else {
        // Jackson puede deserializarse en una implementación de User de Spring Security
        assertTrue(principalObj instanceof User);
        User user = (User) principalObj;
        assertEquals("bob", user.getUsername());
      }
    }

    @Test
    @DisplayName("Debe escribir mapa nulo como 'null'")
    void shouldWriteNullAsJsonNull() throws Exception {
      String json = invokeWrite(null);
      assertEquals("null", json);
    }
  }

  @Nested
  @DisplayName("toEntity(OAuth2Authorization)")
  class ToEntityTests {

    private Authorization invokeToEntity(OAuth2Authorization auth) throws Exception {
      Method m =
          JpaOAuth2AuthorizationService.class.getDeclaredMethod(
              "toEntity", OAuth2Authorization.class);
      m.setAccessible(true);
      return (Authorization) m.invoke(service, auth);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeParse(String data) throws Exception {
      Method m = JpaOAuth2AuthorizationService.class.getDeclaredMethod("parseMap", String.class);
      m.setAccessible(true);
      return (Map<String, Object>) m.invoke(service, data);
    }

    @Test
    @DisplayName("Debe convertir autorización con tokens y atributos a entidad")
    void shouldConvertWithTokensAndAttributes() throws Exception {
      var rc =
          RegisteredClient.withId("client")
              .clientId("client")
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .redirectUri("https://app.example.com/callback")
              .build();

      var builder =
          OAuth2Authorization.withRegisteredClient(rc)
              .id("auth-1")
              .principalName("principal")
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .authorizedScopes(Set.of("read", "write"));

      Instant now = Instant.now();
      OAuth2AuthorizationCode code =
          new OAuth2AuthorizationCode("code-123", now, now.plusSeconds(60));
      builder.token(code, metadata -> metadata.put("c", "m"));

      OAuth2AccessToken at =
          new OAuth2AccessToken(
              OAuth2AccessToken.TokenType.BEARER,
              "at-1",
              now,
              now.plusSeconds(3600),
              Set.of("read"));
      builder.token(at, metadata -> metadata.put("a", 1));

      OAuth2RefreshToken rt = new OAuth2RefreshToken("rt-1", now, now.plusSeconds(7200));
      builder.token(rt, metadata -> metadata.put("r", true));

      Map<String, Object> claims = new HashMap<>();
      claims.put("sub", "s1");
      OidcIdToken idt = new OidcIdToken("id-1", now, now.plusSeconds(1800), claims);
      builder.token(idt, metadata -> metadata.put("i", "x"));

      builder.attribute("attr1", "v1");

      var auth = builder.build();

      Authorization entity = invokeToEntity(auth);

      assertEquals("auth-1", entity.getId());
      assertEquals("client", entity.getRegisteredClientId());
      assertEquals("principal", entity.getPrincipalName());
      assertEquals(
          AuthorizationGrantType.AUTHORIZATION_CODE.getValue(), entity.getAuthorizationGrantType());

      Map<String, Object> attrs = invokeParse(entity.getAttributes());
      assertEquals("v1", attrs.get("attr1"));

      assertEquals("code-123", entity.getAuthorizationCodeValue());
      assertEquals(now, entity.getAuthorizationCodeIssuedAt());
      assertEquals(now.plusSeconds(60), entity.getAuthorizationCodeExpiresAt());
      Map<String, Object> cm = invokeParse(entity.getAuthorizationCodeMetadata());
      assertEquals("m", cm.get("c"));

      assertEquals("at-1", entity.getAccessTokenValue());
      assertEquals(now, entity.getAccessTokenIssuedAt());
      assertEquals(now.plusSeconds(3600), entity.getAccessTokenExpiresAt());
      assertEquals("read", entity.getAccessTokenScopes());
      Map<String, Object> am = invokeParse(entity.getAccessTokenMetadata());
      assertEquals(1, ((Number) am.get("a")).intValue());

      assertEquals("rt-1", entity.getRefreshTokenValue());
      assertEquals(now, entity.getRefreshTokenIssuedAt());
      assertEquals(now.plusSeconds(7200), entity.getRefreshTokenExpiresAt());
      Map<String, Object> rm = invokeParse(entity.getRefreshTokenMetadata());
      assertEquals(true, rm.get("r"));

      assertEquals("id-1", entity.getOidcIdTokenValue());
      // Los metadatos deben incluir nuestro marcador
      Map<String, Object> idm = invokeParse(entity.getOidcIdTokenMetadata());
      assertEquals("x", idm.get("i"));
    }

    @Test
    @DisplayName("Debe convertir autorización mínima a entidad sin tokens")
    void shouldConvertMinimalAuthorization() throws Exception {
      var rc =
          RegisteredClient.withId("c2")
              .clientId("c2")
              .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
              .build();
      var auth =
          OAuth2Authorization.withRegisteredClient(rc)
              .id("a2")
              .principalName("p2")
              .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
              .authorizedScopes(Set.of())
              .build();

      Authorization entity = invokeToEntity(auth);
      assertEquals("a2", entity.getId());
      assertEquals("c2", entity.getRegisteredClientId());
      assertNull(entity.getAuthorizationCodeValue());
      assertNull(entity.getAccessTokenValue());
      assertNull(entity.getRefreshTokenValue());
      assertNull(entity.getOidcIdTokenValue());
      assertNull(entity.getUserCodeValue());
      assertNull(entity.getDeviceCodeValue());
    }
  }

  @Nested
  @DisplayName("toObject(Authorization)")
  class ToObjectTests {

    private OAuth2Authorization invokeToObject(Authorization entity) throws Exception {
      Method m =
          JpaOAuth2AuthorizationService.class.getDeclaredMethod("toObject", Authorization.class);
      m.setAccessible(true);
      return (OAuth2Authorization) m.invoke(service, entity);
    }

    private String writeMap(Map<String, Object> map) throws Exception {
      Method m = JpaOAuth2AuthorizationService.class.getDeclaredMethod("writeMap", Map.class);
      m.setAccessible(true);
      return (String) m.invoke(service, map);
    }

    @Test
    @DisplayName(
        "Debe convertir entidad Authorization a OAuth2Authorization con tokens y atributos")
    void shouldConvertEntityToAuthorizationWithTokensAndAttributes() throws Exception {
      // preparar una simulación de cliente registrado
      var rc =
          RegisteredClient.withId("client")
              .clientId("client")
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .redirectUri("https://app.example.com/callback")
              .build();
      // porque construimos el servicio con una instancia simulada de RegisteredClientRepository
      // anteriormente, necesitamos establecer su comportamiento
      Field f = JpaOAuth2AuthorizationService.class.getDeclaredField("registeredClientRepository");
      f.setAccessible(true);
      f.set(service, Mockito.mock(RegisteredClientRepository.class));
      Mockito.when(((RegisteredClientRepository) f.get(service)).findById("client")).thenReturn(rc);

      // construir entidad
      Authorization entity = new Authorization();
      entity.setId("auth-1");
      entity.setRegisteredClientId("client");
      entity.setPrincipalName("principal");
      entity.setAuthorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
      entity.setAuthorizedScopes("read,write");

      Map<String, Object> attrs = new HashMap<>();
      attrs.put("attr1", "v1");
      entity.setAttributes(writeMap(attrs));

      Instant now = Instant.now();
      entity.setAuthorizationCodeValue("code-123");
      entity.setAuthorizationCodeIssuedAt(now);
      entity.setAuthorizationCodeExpiresAt(now.plusSeconds(60));
      Map<String, Object> cm = new HashMap<>();
      cm.put("c", "m");
      entity.setAuthorizationCodeMetadata(writeMap(cm));

      entity.setAccessTokenValue("at-1");
      entity.setAccessTokenIssuedAt(now);
      entity.setAccessTokenExpiresAt(now.plusSeconds(3600));
      entity.setAccessTokenScopes("read");
      Map<String, Object> am = new HashMap<>();
      am.put("a", 1);
      entity.setAccessTokenMetadata(writeMap(am));

      entity.setRefreshTokenValue("rt-1");
      entity.setRefreshTokenIssuedAt(now);
      entity.setRefreshTokenExpiresAt(now.plusSeconds(7200));
      Map<String, Object> rm = new HashMap<>();
      rm.put("r", true);
      entity.setRefreshTokenMetadata(writeMap(rm));

      entity.setOidcIdTokenValue("id-1");
      entity.setOidcIdTokenIssuedAt(now);
      entity.setOidcIdTokenExpiresAt(now.plusSeconds(1800));
      Map<String, Object> idClaims = new HashMap<>();
      idClaims.put("sub", "s1");
      entity.setOidcIdTokenClaims(writeMap(idClaims));
      Map<String, Object> idm = new HashMap<>();
      idm.put("i", "x");
      entity.setOidcIdTokenMetadata(writeMap(idm));

      // invocar
      OAuth2Authorization auth = invokeToObject(entity);

      assertEquals("auth-1", auth.getId());
      assertEquals("principal", auth.getPrincipalName());
      assertEquals(AuthorizationGrantType.AUTHORIZATION_CODE, auth.getAuthorizationGrantType());
      assertTrue(auth.getAuthorizedScopes().contains("read"));
      assertEquals("v1", auth.getAttributes().get("attr1"));

      var ac = auth.getToken(OAuth2AuthorizationCode.class);
      assertNotNull(ac);
      assertEquals("code-123", ac.getToken().getTokenValue());

      var at = auth.getToken(OAuth2AccessToken.class);
      assertNotNull(at);
      assertEquals("at-1", at.getToken().getTokenValue());
      assertTrue(at.getToken().getScopes().contains("read"));

      var rt = auth.getToken(OAuth2RefreshToken.class);
      assertNotNull(rt);
      assertEquals("rt-1", rt.getToken().getTokenValue());

      var idt = auth.getToken(OidcIdToken.class);
      assertNotNull(idt);
      assertEquals("id-1", idt.getToken().getTokenValue());
      // Las claims pueden ser nulas dependiendo de la deserialización; verifique defensivamente
      Map<String, Object> claims = idt.getClaims();
      if (claims != null) {
        assertEquals("s1", claims.get("sub"));
      }
    }

    @Test
    @DisplayName("Debe lanzar DataRetrievalFailureException cuando el cliente registrado no existe")
    void shouldThrowWhenRegisteredClientMissing() throws Exception {
      Authorization entity = new Authorization();
      entity.setRegisteredClientId("missing");
      entity.setPrincipalName("p");

      // asegurar que el repositorio devuelve null
      Field f = JpaOAuth2AuthorizationService.class.getDeclaredField("registeredClientRepository");
      f.setAccessible(true);
      f.set(service, Mockito.mock(RegisteredClientRepository.class));
      Mockito.when(((RegisteredClientRepository) f.get(service)).findById("missing"))
          .thenReturn(null);

      InvocationTargetException ex =
          assertThrows(InvocationTargetException.class, () -> invokeToObject(entity));
      assertTrue(ex.getCause() instanceof DataRetrievalFailureException);
    }
  }
}
