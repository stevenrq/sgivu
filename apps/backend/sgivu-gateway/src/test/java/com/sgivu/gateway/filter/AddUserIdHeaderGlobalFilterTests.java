package com.sgivu.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AddUserIdHeaderGlobalFilterTests {

  private final AddUserIdHeaderGlobalFilter filter = new AddUserIdHeaderGlobalFilter();

  @Test
  void shouldPropagateUserIdHeaderWhenSubjectPresent() {
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("user-123").build();
    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

    MockServerHttpRequest request = MockServerHttpRequest.get("/v1/vehicles").build();
    ServerWebExchange exchange =
        MockServerWebExchange.from(request).mutate().principal(Mono.just(authentication)).build();

    AtomicReference<ServerWebExchange> exchangeInChain = new AtomicReference<>();
    GatewayFilterChain chain =
        ex -> {
          exchangeInChain.set(ex);
          return Mono.empty();
        };

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    HttpHeaders headers = exchangeInChain.get().getRequest().getHeaders();
    assertThat(headers.getFirst("X-User-ID")).isEqualTo("user-123");
  }

  @Test
  void shouldNotMutateHeadersWhenSubjectMissing() {
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("aud", "gateway").build();
    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

    MockServerHttpRequest request = MockServerHttpRequest.get("/v1/vehicles").build();
    ServerWebExchange exchange =
        MockServerWebExchange.from(request).mutate().principal(Mono.just(authentication)).build();

    AtomicReference<ServerWebExchange> exchangeInChain = new AtomicReference<>();
    GatewayFilterChain chain =
        ex -> {
          exchangeInChain.set(ex);
          return Mono.empty();
        };

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    HttpHeaders headers = exchangeInChain.get().getRequest().getHeaders();
    assertThat(headers.getFirst("X-User-ID")).isNull();
  }

  @Test
  void shouldPropagateUserIdHeaderFromOidcUser() {
    OidcIdToken idToken =
        new OidcIdToken(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("sub", "user-sub", "userId", 456L));
    DefaultOidcUser oidcUser =
        new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken);
    OAuth2AuthenticationToken authentication =
        new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "sgivu-gateway");

    MockServerHttpRequest request = MockServerHttpRequest.get("/v1/vehicles").build();
    ServerWebExchange exchange =
        MockServerWebExchange.from(request).mutate().principal(Mono.just(authentication)).build();

    AtomicReference<ServerWebExchange> exchangeInChain = new AtomicReference<>();
    GatewayFilterChain chain =
        ex -> {
          exchangeInChain.set(ex);
          return Mono.empty();
        };

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    HttpHeaders headers = exchangeInChain.get().getRequest().getHeaders();
    assertThat(headers.getFirst("X-User-ID")).isEqualTo("456");
  }
}
