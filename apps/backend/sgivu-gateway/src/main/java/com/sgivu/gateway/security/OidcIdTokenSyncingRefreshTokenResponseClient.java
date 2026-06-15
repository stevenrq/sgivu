package com.sgivu.gateway.security;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Cliente de respuesta OAuth2 para el grant {@code refresh_token} que sincroniza el {@code
 * id_token} del {@link OidcUser} almacenado en la sesión del gateway con el {@code id_token} recién
 * emitido por el authorization server.
 *
 * <p><strong>Motivación</strong>: Spring Authorization Server genera un {@code id_token} nuevo cada
 * vez que el gateway refresca el access token (scope {@code openid}) y reemplaza el anterior en su
 * {@code OAuth2Authorization}. Spring Security del lado del cliente <em>no</em> actualiza el {@link
 * OidcUser} en el {@code SecurityContext} durante un refresh, por lo que el {@code id_token}
 * almacenado en sesión queda huérfano respecto al authorization server. Esto rompe el RP-Initiated
 * Logout (OIDC) porque el {@code id_token_hint} enviado al {@code /connect/logout} ya no se
 * encuentra indexado en {@code OAuth2AuthorizationService}, y el endpoint responde {@code
 * [invalid_token] id_token_hint}.
 *
 * <p>Este cliente envuelve al {@link WebClientReactiveRefreshTokenTokenResponseClient} estándar y,
 * tras cada refresh exitoso que incluya {@code id_token} en {@code additionalParameters},
 * reconstruye el {@link OAuth2AuthenticationToken} actual con un {@link DefaultOidcUser} que
 * contiene el {@link OidcIdToken} nuevo, y lo persiste vía {@link ServerSecurityContextRepository}
 * (respaldado en Redis). El {@link ServerWebExchange} necesario para persistir se recupera del
 * contexto de Reactor, propagado explícitamente por el bean {@code authorizedClientManager}.
 *
 * <p>El cumplimiento de OpenID Connect Session Management 1.0 queda intacto: el flujo estándar de
 * {@code /connect/logout} con {@code id_token_hint} sigue siendo la ruta principal.
 */
public class OidcIdTokenSyncingRefreshTokenResponseClient
    implements ReactiveOAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> {

  private static final Logger log =
      LoggerFactory.getLogger(OidcIdTokenSyncingRefreshTokenResponseClient.class);

  private final ReactiveOAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> delegate;
  private final ReactiveJwtDecoder jwtDecoder;
  private final ServerSecurityContextRepository securityContextRepository;

  public OidcIdTokenSyncingRefreshTokenResponseClient(
      ReactiveOAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> delegate,
      ReactiveJwtDecoder jwtDecoder,
      ServerSecurityContextRepository securityContextRepository) {
    this.delegate = delegate;
    this.jwtDecoder = jwtDecoder;
    this.securityContextRepository = securityContextRepository;
  }

  @Override
  public Mono<OAuth2AccessTokenResponse> getTokenResponse(
      OAuth2RefreshTokenGrantRequest grantRequest) {
    return delegate
        .getTokenResponse(grantRequest)
        .flatMap(response -> syncOidcUser(response).thenReturn(response));
  }

  private Mono<Void> syncOidcUser(OAuth2AccessTokenResponse response) {
    Map<String, Object> additionalParameters = response.getAdditionalParameters();
    if (additionalParameters == null) {
      return Mono.empty();
    }
    Object idTokenRaw = additionalParameters.get(OidcParameterNames.ID_TOKEN);
    if (!(idTokenRaw instanceof String idTokenValue) || !StringUtils.hasText(idTokenValue)) {
      return Mono.empty();
    }

    return ReactiveSecurityContextHolder.getContext()
        .flatMap(securityContext -> updateSecurityContext(securityContext, idTokenValue))
        .onErrorResume(
            ex -> {
              // No bloquear el refresh si la sincronización falla; el flujo principal sigue
              // funcionando y el usuario podrá cerrar sesión vía fallback /sso-logout.
              log.warn(
                  "Failed to sync refreshed id_token into OidcUser [error={}]. "
                      + "Logout via id_token_hint may fail until next successful sync.",
                  ex.getMessage());
              return Mono.empty();
            });
  }

  private Mono<Void> updateSecurityContext(SecurityContext securityContext, String idTokenValue) {
    Authentication authentication = securityContext.getAuthentication();
    if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
      return Mono.empty();
    }
    if (!(oauthToken.getPrincipal() instanceof OidcUser oldUser)) {
      return Mono.empty();
    }

    return jwtDecoder
        .decode(idTokenValue)
        .map(jwt -> buildOidcUser(oldUser, jwt))
        .map(newUser -> buildAuthentication(newUser, oauthToken))
        .flatMap(
            newAuth -> {
              securityContext.setAuthentication(newAuth);
              return Mono.deferContextual(
                      contextView -> {
                        ServerWebExchange exchange =
                            contextView.getOrDefault(ServerWebExchange.class, null);
                        if (exchange == null) {
                          log.debug(
                              "ServerWebExchange not present in Reactor context; "
                                  + "skipping SecurityContext persistence.");
                          return Mono.<Void>empty();
                        }
                        log.debug(
                            "Syncing refreshed id_token into OidcUser [registrationId={}]",
                            oauthToken.getAuthorizedClientRegistrationId());
                        return securityContextRepository.save(exchange, securityContext);
                      })
                  .then();
            });
  }

  private DefaultOidcUser buildOidcUser(OidcUser oldUser, Jwt jwt) {
    OidcIdToken newIdToken =
        new OidcIdToken(
            jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), jwt.getClaims());
    return new DefaultOidcUser(oldUser.getAuthorities(), newIdToken, oldUser.getUserInfo(), "sub");
  }

  private OAuth2AuthenticationToken buildAuthentication(
      DefaultOidcUser newUser, OAuth2AuthenticationToken oldToken) {
    return new OAuth2AuthenticationToken(
        newUser, newUser.getAuthorities(), oldToken.getAuthorizedClientRegistrationId());
  }
}
