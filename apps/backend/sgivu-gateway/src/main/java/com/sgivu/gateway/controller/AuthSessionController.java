package com.sgivu.gateway.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Expone la sesión autenticada del gateway (BFF) para que la SPA consulte estado y claims sin
 * manejar tokens en el navegador.
 */
@RestController
@RequestMapping("/auth")
public class AuthSessionController {

  private final ReactiveJwtDecoder jwtDecoder;
  private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

  public AuthSessionController(
      ReactiveJwtDecoder jwtDecoder,
      ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
    this.jwtDecoder = jwtDecoder;
    this.authorizedClientManager = authorizedClientManager;
  }

  @GetMapping("/session")
  public Mono<ResponseEntity<AuthSessionResponse>> session(
      Authentication authentication, ServerWebExchange exchange) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
      return Mono.just(ResponseEntity.ok(fromJwt(jwtAuthenticationToken.getToken())));
    }

    if (authentication instanceof OAuth2AuthenticationToken oauth2AuthenticationToken) {
      String registrationId = oauth2AuthenticationToken.getAuthorizedClientRegistrationId();
      OAuth2AuthorizeRequest authorizeRequest =
          OAuth2AuthorizeRequest.withClientRegistrationId(registrationId)
              .principal(oauth2AuthenticationToken)
              .attribute(ServerWebExchange.class.getName(), exchange)
              .build();
      return authorizedClientManager
          .authorize(authorizeRequest)
          .flatMap(
              client -> {
                if (client == null || client.getAccessToken() == null) {
                  return Mono.empty();
                }
                return jwtDecoder
                    .decode(client.getAccessToken().getTokenValue())
                    .map(this::fromJwt);
              })
          .map(ResponseEntity::ok)
          .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()))
          .onErrorResume(ex -> Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }

  private AuthSessionResponse fromJwt(Jwt jwt) {
    List<String> rolesAndPermissions = jwt.getClaimAsStringList("rolesAndPermissions");
    Boolean isAdmin = jwt.getClaimAsBoolean("isAdmin");
    return new AuthSessionResponse(
        true,
        jwt.getSubject(),
        jwt.getClaimAsString("username"),
        rolesAndPermissions == null ? List.of() : rolesAndPermissions,
        Boolean.TRUE.equals(isAdmin));
  }

  public record AuthSessionResponse(
      boolean authenticated,
      String userId,
      String username,
      List<String> rolesAndPermissions,
      boolean isAdmin) {}
}
