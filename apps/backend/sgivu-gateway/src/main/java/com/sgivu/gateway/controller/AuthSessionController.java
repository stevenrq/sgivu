package com.sgivu.gateway.controller;

import com.sgivu.gateway.controller.api.AuthSessionApi;
import com.sgivu.gateway.dto.AuthSessionResponse;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class AuthSessionController implements AuthSessionApi {

  private final ReactiveJwtDecoder jwtDecoder;
  private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

  public AuthSessionController(
      ReactiveJwtDecoder jwtDecoder,
      ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
    this.jwtDecoder = jwtDecoder;
    this.authorizedClientManager = authorizedClientManager;
  }

  @Override
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
}
