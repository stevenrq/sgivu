package com.sgivu.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * {@link ServerLogoutHandler} que revoca los tokens OAuth2 (access_token y refresh_token) en el
 * servidor de autorización antes de completar el logout.
 *
 * <p>Este handler se ejecuta <strong>primero</strong> en la cadena delegada de logout ({@link
 * org.springframework.security.web.server.authentication.logout.DelegatingServerLogoutHandler}),
 * antes de limpiar el {@code SecurityContext} e invalidar la sesión Redis.
 *
 * <h3>Flujo de logout completo (en orden)</h3>
 *
 * <ol>
 *   <li><strong>Este handler</strong>: revoca tokens en {@code /oauth2/revoke} del auth server y
 *       elimina el {@link OAuth2AuthorizedClient} de la sesión
 *   <li>{@code SecurityContextServerLogoutHandler}: limpia el {@code SecurityContext}
 *   <li>Lambda en {@code SecurityConfig}: invalida la sesión Redis ({@code WebSession.invalidate})
 *   <li>{@code OidcClientInitiatedServerLogoutSuccessHandler}: redirige al auth server para OIDC
 *       logout, luego a Angular {@code /login}
 * </ol>
 *
 * <h3>Resiliencia</h3>
 *
 * <p>Si la revocación falla (auth server caído, network error), el error se loguea y el logout
 * continúa. Los tokens quedarán activos hasta su expiración natural, pero la sesión local se
 * destruye igualmente.
 *
 * @see com.sgivu.gateway.security.SecurityConfig#logoutSuccessHandler
 */
public class TokenRevocationServerLogoutHandler implements ServerLogoutHandler {

  private static final Logger log =
      LoggerFactory.getLogger(TokenRevocationServerLogoutHandler.class);

  private final ServerOAuth2AuthorizedClientRepository authorizedClientRepository;
  private final WebClient webClient;

  public TokenRevocationServerLogoutHandler(
      ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {
    this.authorizedClientRepository = authorizedClientRepository;
    this.webClient = WebClient.builder().build();
  }

  @Override
  public Mono<Void> logout(WebFilterExchange exchange, Authentication authentication) {
    if (!(authentication instanceof OAuth2AuthenticationToken oauth2Token)) {
      // No es una autenticación OAuth2 (podría ser JWT directo o anónimo).
      // Esto ocurre si la sesión ya expiró y no hay OidcUser disponible.
      log.warn(
          "Logout: authentication is not OAuth2AuthenticationToken [type={}]. "
              + "Token revocation skipped — tokens will expire naturally.",
          authentication != null ? authentication.getClass().getSimpleName() : "null");
      return Mono.empty();
    }

    String registrationId = oauth2Token.getAuthorizedClientRegistrationId();
    ServerWebExchange serverExchange = exchange.getExchange();
    log.info("Logout: starting token revocation [registration={}]", registrationId);

    return authorizedClientRepository
        .loadAuthorizedClient(registrationId, authentication, serverExchange)
        .flatMap(
            authorizedClient -> {
              String clientId = authorizedClient.getClientRegistration().getClientId();
              String clientSecret = authorizedClient.getClientRegistration().getClientSecret();
              String revocationEndpoint =
                  authorizedClient
                      .getClientRegistration()
                      .getProviderDetails()
                      .getConfigurationMetadata()
                      .getOrDefault("revocation_endpoint", "")
                      .toString();

              if (revocationEndpoint.isEmpty()) {
                String issuerUri =
                    authorizedClient.getClientRegistration().getProviderDetails().getIssuerUri();
                revocationEndpoint = issuerUri + "/oauth2/revoke";
                log.debug(
                    "Logout: revocation_endpoint not in provider metadata, "
                        + "falling back to issuer-based: {}",
                    revocationEndpoint);
              }

              Mono<Void> revokeAccess =
                  revokeToken(
                      revocationEndpoint,
                      authorizedClient.getAccessToken().getTokenValue(),
                      "access_token",
                      clientId,
                      clientSecret);

              Mono<Void> revokeRefresh = Mono.empty();
              var refreshToken = authorizedClient.getRefreshToken();
              if (refreshToken != null) {
                revokeRefresh =
                    revokeToken(
                        revocationEndpoint,
                        refreshToken.getTokenValue(),
                        "refresh_token",
                        clientId,
                        clientSecret);
              } else {
                log.debug("Logout: no refresh_token to revoke");
              }

              return revokeAccess
                  .then(revokeRefresh)
                  .then(
                      authorizedClientRepository.removeAuthorizedClient(
                          registrationId, authentication, serverExchange))
                  .doOnSuccess(
                      v ->
                          log.info(
                              "Logout: tokens revoked and authorized client removed "
                                  + "[registration={}]",
                              registrationId));
            })
        .switchIfEmpty(
            Mono.fromRunnable(
                () ->
                    log.warn(
                        "Logout: no authorized client found in session [registration={}]. "
                            + "Session may have already expired — nothing to revoke.",
                        registrationId)))
        .onErrorResume(
            ex -> {
              // La revocación falló pero el logout debe continuar. Los tokens expirarán
              // naturalmente según su TTL (access: 30min, refresh: 30d).
              log.warn(
                  "Logout: token revocation failed [registration={}]. "
                      + "Continuing with session cleanup. Error: {}",
                  registrationId,
                  ex.getMessage());
              return Mono.empty();
            });
  }

  private Mono<Void> revokeToken(
      String revocationEndpoint,
      String tokenValue,
      String tokenTypeHint,
      String clientId,
      String clientSecret) {

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("token", tokenValue);
    body.add("token_type_hint", tokenTypeHint);

    log.debug("Logout: revoking {} at {}", tokenTypeHint, revocationEndpoint);

    return webClient
        .post()
        .uri(revocationEndpoint)
        .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(BodyInserters.fromFormData(body))
        .retrieve()
        .toBodilessEntity()
        .doOnSuccess(
            response ->
                log.info(
                    "Logout: {} revoked successfully [endpoint={}]",
                    tokenTypeHint,
                    revocationEndpoint))
        .doOnError(
            ex ->
                log.warn(
                    "Logout: failed to revoke {} [endpoint={}, error={}]",
                    tokenTypeHint,
                    revocationEndpoint,
                    ex.getMessage()))
        .onErrorResume(ex -> Mono.empty())
        .then();
  }
}
