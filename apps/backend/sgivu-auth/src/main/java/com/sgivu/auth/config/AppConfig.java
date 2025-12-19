package com.sgivu.auth.config;

import com.sgivu.auth.client.UserClient;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Configuración transversal: clientes REST hacia otros microservicios, cifrado de contraseñas y
 * circuitos de resiliencia.
 */
@Configuration
public class AppConfig {

  @Value("${service.internal.secret-key}")
  private String internalServiceKey;

  private final ServicesProperties servicesProperties;

  public AppConfig(ServicesProperties servicesProperties) {
    this.servicesProperties = servicesProperties;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  @LoadBalanced
  RestClient.Builder restClientBuilder() {
    return RestClient.builder();
  }

  /**
   * Cliente tipado para consultar usuarios en {@code sgivu-user}, inyectando la cabecera interna
   * necesaria para llamadas de servicio a servicio.
   */
  @Bean
  UserClient userClient(RestClient.Builder restClientBuilder) {
    RestClient restClient =
        restClientBuilder
            .baseUrl(servicesProperties.getMap().get("sgivu-user").getUrl())
            .defaultHeader("X-Internal-Service-Key", internalServiceKey)
            .build();

    RestClientAdapter adapter = RestClientAdapter.create(restClient);
    HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
    return factory.createClient(UserClient.class);
  }

  /**
   * Circuito de Resilience4j para el cliente de usuarios: abre con 50% de fallos y corta llamadas
   * tras 3 segundos para que el login no bloquee ventas o contratos.
   */
  @Bean
  Customizer<Resilience4JCircuitBreakerFactory> userServiceCircuitBreakerCustomizer() {
    CircuitBreakerConfig circuitBreakerConfig =
        CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(20))
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(20)
            .minimumNumberOfCalls(10)
            .permittedNumberOfCallsInHalfOpenState(3)
            .build();

    TimeLimiterConfig timeLimiterConfig =
        TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(3)).build();

    return factory ->
        factory.configure(
            builder ->
                builder
                    .circuitBreakerConfig(circuitBreakerConfig)
                    .timeLimiterConfig(timeLimiterConfig)
                    .build(),
            "userServiceCircuitBreaker");
  }
}
