package com.sgivu.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Propaga el claim `sub` del JWT al header `X-User-ID` para que los microservicios puedan auditar y
 * correlacionar peticiones sin revalidar el token, incluso en flujos asincrónicos.
 */
@Component
public class AddUserIdHeaderGlobalFilter implements GlobalFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return exchange
        .getPrincipal()
        .cast(JwtAuthenticationToken.class)
        .map(JwtAuthenticationToken::getToken)
        .flatMap(
            jwt -> {
              String userId = jwt.getSubject();
              ServerWebExchange mutatedExchange = exchange;
              if (userId != null) {
                mutatedExchange =
                    exchange
                        .mutate()
                        .request(request -> request.header("X-User-ID", userId))
                        .build();
              }
              return chain.filter(mutatedExchange);
            });
  }

  @Override
  public int getOrder() {
    return 1;
  }
}
