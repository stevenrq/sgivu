package com.sgivu.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

public class OidcIdTokenSyncingRefreshTokenResponseClientTest {

  @Mock private ReactiveOAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> delegate;

  @Mock private ReactiveJwtDecoder jwtDecoder;
  @Mock private ServerSecurityContextRepository securityContextRepository;
  @Mock private ServerWebExchange exchange;
  @Mock private OAuth2RefreshTokenGrantRequest grantRequest;

  private OidcIdTokenSyncingRefreshTokenResponseClient client;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    client =
        new OidcIdTokenSyncingRefreshTokenResponseClient(
            delegate, jwtDecoder, securityContextRepository);
  }

  @Nested
  @DisplayName("getTokenResponse(OAuth2RefreshTokenGrantRequest)")
  class GetTokenResponseTests {

    @Test
    @DisplayName("Debe sincronizar el id_token del OidcUser cuando la respuesta lo incluye")
    void shouldSyncOidcUserWhenResponseContainsIdToken() {
      String newIdTokenValue = "new.id.token";
      OAuth2AccessTokenResponse response = buildResponseWithIdToken(newIdTokenValue);
      OAuth2AuthenticationToken oldAuth = buildOAuth2Authentication("old.id.token");
      SecurityContext securityContext = new SecurityContextImpl(oldAuth);

      when(delegate.getTokenResponse(grantRequest)).thenReturn(Mono.just(response));
      when(jwtDecoder.decode(newIdTokenValue)).thenReturn(Mono.just(buildJwt(newIdTokenValue)));
      when(securityContextRepository.save(eq(exchange), any(SecurityContext.class)))
          .thenReturn(Mono.empty());

      Mono<OAuth2AccessTokenResponse> mono =
          client
              .getTokenResponse(grantRequest)
              .contextWrite(
                  ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
              .contextWrite(Context.of(ServerWebExchange.class, exchange));

      StepVerifier.create(mono).expectNext(response).verifyComplete();

      ArgumentCaptor<SecurityContext> savedContext = ArgumentCaptor.forClass(SecurityContext.class);
      verify(securityContextRepository).save(eq(exchange), savedContext.capture());
      Authentication saved = savedContext.getValue().getAuthentication();
      assertThat(saved).isInstanceOf(OAuth2AuthenticationToken.class);
      OAuth2AuthenticationToken savedOauth = (OAuth2AuthenticationToken) saved;
      assertThat(savedOauth.getPrincipal()).isInstanceOf(DefaultOidcUser.class);
      DefaultOidcUser newUser = (DefaultOidcUser) savedOauth.getPrincipal();
      assertThat(newUser.getIdToken().getTokenValue()).isEqualTo(newIdTokenValue);
      assertThat(savedOauth.getAuthorizedClientRegistrationId())
          .isEqualTo(oldAuth.getAuthorizedClientRegistrationId());
    }

    @Test
    @DisplayName("No debe tocar el contexto cuando la respuesta no incluye id_token")
    void shouldNotSyncWhenResponseHasNoIdToken() {
      OAuth2AccessTokenResponse response = buildResponseWithoutIdToken();
      when(delegate.getTokenResponse(grantRequest)).thenReturn(Mono.just(response));

      StepVerifier.create(client.getTokenResponse(grantRequest))
          .expectNext(response)
          .verifyComplete();

      verify(securityContextRepository, never()).save(any(), any());
    }

    @Test
    @DisplayName("No debe sincronizar cuando la Authentication no es OAuth2AuthenticationToken")
    void shouldNotSyncWhenAuthenticationIsNotOAuth2() {
      OAuth2AccessTokenResponse response = buildResponseWithIdToken("any.id.token");
      SecurityContext securityContext =
          new SecurityContextImpl(new TestingAuthenticationToken("user", "pwd"));

      when(delegate.getTokenResponse(grantRequest)).thenReturn(Mono.just(response));

      Mono<OAuth2AccessTokenResponse> mono =
          client
              .getTokenResponse(grantRequest)
              .contextWrite(
                  ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
              .contextWrite(Context.of(ServerWebExchange.class, exchange));

      StepVerifier.create(mono).expectNext(response).verifyComplete();

      verify(securityContextRepository, never()).save(any(), any());
    }

    @Test
    @DisplayName("Debe propagar errores del delegate sin tocar el contexto")
    void shouldPropagateDelegateError() {
      RuntimeException boom = new RuntimeException("boom");
      when(delegate.getTokenResponse(grantRequest)).thenReturn(Mono.error(boom));

      StepVerifier.create(client.getTokenResponse(grantRequest))
          .expectErrorMessage("boom")
          .verify();

      verify(securityContextRepository, never()).save(any(), any());
    }

    @Test
    @DisplayName("No debe persistir el contexto cuando no hay ServerWebExchange en el contexto")
    void shouldNotPersistWhenExchangeIsMissingFromReactorContext() {
      String newIdTokenValue = "new.id.token";
      OAuth2AccessTokenResponse response = buildResponseWithIdToken(newIdTokenValue);
      OAuth2AuthenticationToken oldAuth = buildOAuth2Authentication("old.id.token");
      SecurityContext securityContext = new SecurityContextImpl(oldAuth);

      when(delegate.getTokenResponse(grantRequest)).thenReturn(Mono.just(response));
      when(jwtDecoder.decode(newIdTokenValue)).thenReturn(Mono.just(buildJwt(newIdTokenValue)));

      Mono<OAuth2AccessTokenResponse> mono =
          client
              .getTokenResponse(grantRequest)
              .contextWrite(
                  ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));

      StepVerifier.create(mono).expectNext(response).verifyComplete();

      verify(securityContextRepository, never()).save(any(), any());
    }
  }

  private OAuth2AccessTokenResponse buildResponseWithIdToken(String idTokenValue) {
    Map<String, Object> additionalParameters = new HashMap<>();
    additionalParameters.put(OidcParameterNames.ID_TOKEN, idTokenValue);
    return OAuth2AccessTokenResponse.withToken("access-token-value")
        .tokenType(OAuth2AccessToken.TokenType.BEARER)
        .expiresIn(1800)
        .additionalParameters(additionalParameters)
        .build();
  }

  private OAuth2AccessTokenResponse buildResponseWithoutIdToken() {
    return OAuth2AccessTokenResponse.withToken("access-token-value")
        .tokenType(OAuth2AccessToken.TokenType.BEARER)
        .expiresIn(1800)
        .build();
  }

  private OAuth2AuthenticationToken buildOAuth2Authentication(String idTokenValue) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "steven");
    OidcIdToken idToken =
        new OidcIdToken(
            idTokenValue, Instant.now().minusSeconds(60), Instant.now().plusSeconds(600), claims);
    DefaultOidcUser principal =
        new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken, null, "sub");
    return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "sgivu-gateway");
  }

  private Jwt buildJwt(String tokenValue) {
    return Jwt.withTokenValue(tokenValue)
        .header("alg", "RS256")
        .claim("sub", "steven")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(1800))
        .build();
  }
}
