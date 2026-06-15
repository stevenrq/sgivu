package com.sgivu.gateway.filter;

import com.sgivu.gateway.config.AngularClientProperties;
import com.sgivu.gateway.config.ServicesProperties;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Filtro WebFlux que verifica la disponibilidad de {@code sgivu-auth} antes de iniciar el flujo
 * OAuth2. Intercepta {@code GET /oauth2/authorization/**} y realiza un probe desde el gateway
 * (dentro de la red Docker) al endpoint {@code /actuator/health} del auth server.
 *
 * <p>Si {@code sgivu-auth} no está disponible, redirige directamente al cliente Angular en lugar de
 * enviar el navegador a {@code localhost:9000/login}, donde recibiría un "Unable to connect" sin
 * posibilidad de recuperación automática. Cuando el cliente Angular recibe la redirección, {@code
 * LoginComponent} vuelve a llamar a {@code startLoginFlow()}, que detecta auth caído y muestra el
 * {@code ServiceUnavailableComponent}.
 *
 * <p>El check se hace desde el gateway (red interna Docker) y no desde el navegador, eliminando los
 * problemas de resolución DNS y CORS del probe directo. El timeout de 2 s evita penalizar el flujo
 * de login normal en más de 2 ms en condiciones normales (el auth server suele responder en < 10 ms
 * dentro de Docker).
 */
@Component
public class AuthServerAvailabilityFilter implements WebFilter, Ordered {

  private static final String OAUTH2_AUTHORIZATION_PATH = "/oauth2/authorization/";
  private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(2);

  private final WebClient webClient;
  private final String authHealthUrl;
  private final String angularLoginUrl;

  public AuthServerAvailabilityFilter(
      WebClient.Builder webClientBuilder,
      ServicesProperties servicesProperties,
      AngularClientProperties angularClientProperties) {
    ServicesProperties.ServiceInfo authService = servicesProperties.getMap().get("sgivu-auth");
    String authBaseUrl = authService != null ? authService.getUrl() : "http://sgivu-auth:9000";
    this.authHealthUrl = authBaseUrl + "/actuator/health";
    this.angularLoginUrl = angularClientProperties.getUrl() + "/login";
    this.webClient = webClientBuilder.build();
  }

  @Override
  @NonNull
  public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
    String path = exchange.getRequest().getPath().value();
    if (!path.startsWith(OAUTH2_AUTHORIZATION_PATH)) {
      return chain.filter(exchange);
    }

    return isAuthServerUp()
        .flatMap(
            isUp -> {
              if (isUp) {
                return chain.filter(exchange);
              }
              exchange.getResponse().setStatusCode(HttpStatus.FOUND);
              exchange.getResponse().getHeaders().setLocation(URI.create(angularLoginUrl));
              return exchange.getResponse().setComplete();
            });
  }

  /** Prioridad alta para ejecutarse antes que la cadena de seguridad de Spring Security. */
  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 100;
  }

  private Mono<Boolean> isAuthServerUp() {
    return webClient
        .get()
        .uri(authHealthUrl)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
        .timeout(HEALTH_CHECK_TIMEOUT)
        .map(body -> "UP".equals(String.valueOf(body.getOrDefault("status", ""))))
        .onErrorReturn(false);
  }
}
