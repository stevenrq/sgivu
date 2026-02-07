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
 * <p>Este handler se ejecuta <strong>antes</strong> de que {@code
 * OidcClientInitiatedServerLogoutSuccessHandler} redirija al endpoint de logout del auth server.
 * Garantiza que los tokens queden invalidados en la tabla {@code authorizations} de la base de
 * datos del auth server, evitando que queden tokens activos después de cerrar sesión.
 *
 * <p>El flujo es:
 *
 * <ol>
 *   <li>Spring Security invoca este handler como parte de la cadena de logout
 *   <li>Se obtiene el {@link OAuth2AuthorizedClient} de la sesión
 *   <li>Se envía la petición de revocación al endpoint {@code /oauth2/revoke} del auth server
 *   <li>Se elimina el {@link OAuth2AuthorizedClient} del repositorio (WebSession)
 *   <li>Finalmente, el {@code OidcClientInitiatedServerLogoutSuccessHandler} redirige al auth
 *       server para cerrar la sesión OIDC
 * </ol>
 *
 * @see org.springframework.security.web.server.authentication.logout.ServerLogoutHandler
 * @see ServerOAuth2AuthorizedClientRepository
 */
public class TokenRevocationServerLogoutHandler implements ServerLogoutHandler {

  private static final Logger log =
      LoggerFactory.getLogger(TokenRevocationServerLogoutHandler.class);

  private final ServerOAuth2AuthorizedClientRepository authorizedClientRepository;
  private final WebClient webClient;

  public TokenRevocationServerLogoutHandler(
      ServerOAuth2AuthorizedClientRepository authorizedClientRepository, WebClient webClient) {
    this.authorizedClientRepository = authorizedClientRepository;
    this.webClient = webClient;
  }

  @Override
  public Mono<Void> logout(WebFilterExchange exchange, Authentication authentication) {
    if (!(authentication instanceof OAuth2AuthenticationToken oauth2Token)) {
      return Mono.empty();
    }

    String registrationId = oauth2Token.getAuthorizedClientRegistrationId();
    ServerWebExchange serverExchange = exchange.getExchange();

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
                // Fallback: construir la URL desde el issuer-uri
                String issuerUri =
                    authorizedClient.getClientRegistration().getProviderDetails().getIssuerUri();
                revocationEndpoint = issuerUri + "/oauth2/revoke";
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
              }

              return revokeAccess
                  .then(revokeRefresh)
                  .then(
                      authorizedClientRepository.removeAuthorizedClient(
                          registrationId, authentication, serverExchange))
                  .doOnSuccess(v -> log.info("Tokens revocados y cliente autorizado removido."));
            })
        .onErrorResume(
            ex -> {
              log.warn(
                  "Error durante la revocación de tokens en el logout. "
                      + "Continuando con el logout de todas formas.",
                  ex);
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

    return webClient
        .post()
        .uri(revocationEndpoint)
        .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(BodyInserters.fromFormData(body))
        .retrieve()
        .toBodilessEntity()
        .doOnSuccess(response -> log.debug("Token ({}) revocado exitosamente.", tokenTypeHint))
        .doOnError(
            ex -> log.warn("Error al revocar token ({}): {}", tokenTypeHint, ex.getMessage()))
        .onErrorResume(ex -> Mono.empty())
        .then();
  }
}
