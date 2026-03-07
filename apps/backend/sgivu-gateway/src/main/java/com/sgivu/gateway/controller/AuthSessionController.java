package com.sgivu.gateway.controller;

import com.sgivu.gateway.controller.api.AuthSessionApi;
import com.sgivu.gateway.dto.AuthSessionResponse;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Controlador BFF que expone el estado de sesión autenticada para la SPA Angular.
 *
 * <p>Este endpoint es el punto de entrada principal para que Angular determine si el usuario tiene
 * una sesión activa y obtenga sus claims (roles, permisos, username). El flujo es:
 *
 * <ol>
 *   <li>Angular llama a {@code GET /auth/session} en cada navegación (o al iniciar la app)
 *   <li>Si hay sesión Redis válida con {@code OAuth2AuthenticationToken}: se recupera el {@code
 *       access_token} vía el {@code authorizedClientManager}, que lo renueva automáticamente si
 *       expiró (usando el refresh_token)
 *   <li>Se decodifica el JWT para extraer claims y retornar {@link AuthSessionResponse}
 *   <li>Si no hay sesión o los tokens son inválidos: se retorna 401 y Angular inicia
 *       re-autenticación
 * </ol>
 *
 * <p><strong>Nota sobre el token refresh</strong>: la llamada a {@code authorizedClientManager
 * .authorize()} es la que dispara el refresh transparente del access_token. Si el refresh_token
 * también es inválido (error {@code invalid_grant}), el manager retorna {@code Mono.empty()} y este
 * endpoint devuelve 401.
 */
@RestController
public class AuthSessionController implements AuthSessionApi {

  private static final Logger log = LoggerFactory.getLogger(AuthSessionController.class);

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
      log.debug(
          "Session check: no authentication — user is not logged in [path={}]",
          exchange.getRequest().getPath());
      return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    // Caso 1: Request con JWT directo (Bearer token) — no requiere sesión Redis,
    // los claims están disponibles directamente en el token.
    if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
      log.debug(
          "Session check: resolved via JWT Bearer token [sub={}]",
          jwtAuthenticationToken.getToken().getSubject());
      return Mono.just(ResponseEntity.ok(fromJwt(jwtAuthenticationToken.getToken())));
    }

    // Caso 2: Request con sesión Redis (flujo BFF) — el access_token está en la sesión.
    // Se usa el authorizedClientManager para obtenerlo (y renovarlo si expiró).
    if (authentication instanceof OAuth2AuthenticationToken oauth2AuthenticationToken) {
      String registrationId = oauth2AuthenticationToken.getAuthorizedClientRegistrationId();
      log.debug(
          "Session check: resolving via OAuth2 session [registration={}, principal={}]",
          registrationId,
          oauth2AuthenticationToken.getName());

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
                  log.warn(
                      "Session check: authorized client has no access_token "
                          + "[registration={}]. Returning 401.",
                      registrationId);
                  return Mono.empty();
                }
                Instant expiresAt = client.getAccessToken().getExpiresAt();
                log.debug(
                    "Session check: access_token obtained [registration={}, expiresAt={}]",
                    registrationId,
                    expiresAt);
                return jwtDecoder
                    .decode(client.getAccessToken().getTokenValue())
                    .map(this::fromJwt);
              })
          .map(ResponseEntity::ok)
          .switchIfEmpty(
              Mono.fromCallable(
                  () -> {
                    // Mono.empty() llega aquí cuando:
                    // - El authorizedClientManager retornó empty (invalid_grant en refresh)
                    // - El client o access_token era null
                    log.warn(
                        "Session check: could not resolve session — no valid access_token "
                            + "available [registration={}]. This typically means the "
                            + "refresh_token was rejected (invalid_grant).",
                        registrationId);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .<AuthSessionResponse>build();
                  }))
          .onErrorResume(
              ex -> {
                // Errores posibles: JWT decode failure, network issues, etc.
                log.error(
                    "Session check: unexpected error resolving session "
                        + "[registration={}, error={}]",
                    registrationId,
                    ex.getMessage(),
                    ex);
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
              });
    }

    log.warn(
        "Session check: unsupported authentication type [type={}]",
        authentication.getClass().getSimpleName());
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
