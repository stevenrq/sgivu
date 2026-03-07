package com.sgivu.gateway.filter;

import java.security.Principal;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filtro global que añade el header "X-User-ID" a las peticiones que provienen de usuarios
 * autenticados. Detecta el identificador de usuario a partir de:
 *
 * <ul>
 *   <li>{@link JwtAuthenticationToken}: se usa el subject del token JWT.
 *   <li>{@link OAuth2AuthenticationToken} con {@link OidcUser}: se busca la claim "userId" (String
 *       o Number) y en su defecto se usa el subject del OIDC.
 * </ul>
 *
 * El filtro es reactivo y no bloqueante: si no hay principal disponible deja la petición sin
 * modificar. Añade el header "X-User-ID" a la petición mutada cuando se resuelve un id válido.
 */
@Component
public class AddUserIdHeaderGlobalFilter implements GlobalFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return exchange
        .getPrincipal()
        .flatMap(
            principal -> {
              String userId = resolveUserId(principal);
              if (userId == null) {
                return chain.filter(exchange);
              }
              ServerWebExchange mutatedExchange =
                  exchange.mutate().request(request -> request.header("X-User-ID", userId)).build();
              return chain.filter(mutatedExchange);
            })
        .switchIfEmpty(chain.filter(exchange));
  }

  @Override
  public int getOrder() {
    return 1;
  }

  private String resolveUserId(Principal principal) {
    if (principal instanceof JwtAuthenticationToken jwtAuthenticationToken) {
      return jwtAuthenticationToken.getToken().getSubject();
    }

    if (principal instanceof OAuth2AuthenticationToken oauth2AuthenticationToken
        && oauth2AuthenticationToken.getPrincipal() instanceof OidcUser oidcUser) {
      Object userIdClaim = oidcUser.getClaims().get("userId");
      if (userIdClaim instanceof String userId) {
        return userId;
      }
      if (userIdClaim instanceof Number userId) {
        return userId.toString();
      }
      return oidcUser.getSubject();
    }

    return null;
  }
}
