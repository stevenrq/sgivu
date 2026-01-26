package com.sgivu.auth.config;

import com.sgivu.auth.client.UserClient;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

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

  /**
   * RestClient.Builder con balanceo de carga para llamadas entre microservicios. Este builder
   * resuelve nombres de servicio (ej. sgivu-user) via Eureka/LoadBalancer.
   */
  @Bean
  @LoadBalanced
  RestClient.Builder loadBalancedRestClientBuilder() {
    return RestClient.builder();
  }

  /**
   * RestClient.Builder sin balanceo de carga para conexiones directas (infraestructura). Marcado
   * como {@code @Primary} para que Eureka lo use por defecto, evitando el problema circular donde
   * Eureka client necesita LoadBalancer, pero LoadBalancer necesita Eureka para resolver servicios.
   */
  @Bean
  @Primary
  RestClient.Builder restClientBuilder() {
    return RestClient.builder();
  }

  @Bean
  UserClient userClient(
      @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder restClientBuilder) {
    RestClient restClient =
        restClientBuilder
            .baseUrl(servicesProperties.getMap().get("sgivu-user").getUrl())
            .defaultHeader("X-Internal-Service-Key", internalServiceKey)
            .build();

    RestClientAdapter adapter = RestClientAdapter.create(restClient);
    HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
    return factory.createClient(UserClient.class);
  }

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
