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
        TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(10)).build();
    TimeLimiterConfig mlTimeLimiterConfig =
        TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(30)).build();
    // Con ~14.000 muestras y RandomizedSearchCV (10 iter × 3 folds × 2 modelos) el
    // reentrenamiento tarda ~15 min. 1800 s (30 min) da margen holgado.
    TimeLimiterConfig mlRetrainTimeLimiterConfig =
        TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(1800)).build();

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
      factory.configure(
          builder ->
              builder
                  .circuitBreakerConfig(circuitBreakerConfig)
                  .timeLimiterConfig(mlTimeLimiterConfig)
                  .build(),
          "mlServiceCircuitBreaker");
      factory.configure(
          builder ->
              builder
                  .circuitBreakerConfig(circuitBreakerConfig)
                  .timeLimiterConfig(mlRetrainTimeLimiterConfig)
                  .build(),
          "mlRetrainCircuitBreaker");
    };
  }
}
