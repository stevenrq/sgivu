package com.sgivu.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
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
}
