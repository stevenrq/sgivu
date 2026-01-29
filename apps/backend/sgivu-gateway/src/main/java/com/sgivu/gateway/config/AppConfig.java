package com.sgivu.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.time.Duration;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

  @Bean
  Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCircuitBreakerCustomizer() {
    CircuitBreakerConfig circuitBreakerConfig =
        CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(5))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .permittedNumberOfCallsInHalfOpenState(3)
            .build();

    TimeLimiterConfig timeLimiterConfig =
        TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(3)).build();
    TimeLimiterConfig mlTimeLimiterConfig =
        TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(30)).build();

    return factory -> {
      factory.configure(
          builder ->
              builder
                  .circuitBreakerConfig(circuitBreakerConfig)
                  .timeLimiterConfig(timeLimiterConfig)
                  .build(),
          "authServiceCircuitBreaker",
          "userServiceCircuitBreaker",
          "clientServiceCircuitBreaker",
          "vehicleServiceCircuitBreaker",
          "purchaseSaleServiceCircuitBreaker");
      // Retrain de ML puede demorar más de 3s; aumentamos el timeout específico.
      factory.configure(
          builder ->
              builder
                  .circuitBreakerConfig(circuitBreakerConfig)
                  .timeLimiterConfig(mlTimeLimiterConfig)
                  .build(),
          "mlServiceCircuitBreaker");
    };
  }
}
