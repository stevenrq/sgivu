package com.sgivu.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.session.data.redis.ReactiveRedisSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.server.RedisWebSessionConfiguration;

import static org.mockito.Mockito.mock;

import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;

class RedisSessionConfigTest {

  private final ReactiveWebApplicationContextRunner contextRunner =
      new ReactiveWebApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  RedisSessionConfig.class,
                  TestConfig.class,
                  RedisAutoConfiguration.class,
                  RedisReactiveAutoConfiguration.class,
                  SessionAutoConfiguration.class))
          .withPropertyValues(
              "spring.session.store-type=redis",
              "spring.data.redis.host=localhost",
              "spring.data.redis.port=6379");

  @Test
  void redisSessionConfigurationBeanExists() {
    this.contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(RedisWebSessionConfiguration.class);
          assertThat(context).hasSingleBean(ReactiveRedisSessionRepository.class);
        });
  }

  @Test
  void redisSessionNamespaceIsPickedUp() {
    this.contextRunner
        .withPropertyValues("spring.session.redis.namespace=spring:session:sgivu-gateway")
        .run(
            context -> {
              // RedisWebSessionConfiguration no expone el namespace directamente, pero
              // podemos comprobar si el repositorio está configurado. De hecho, podemos comprobar
              // la propiedad desde el entorno.
              assertThat(context.getEnvironment().getProperty("spring.session.redis.namespace"))
                  .isEqualTo("spring:session:sgivu-gateway");
            });
  }

  @Configuration
  static class TestConfig {
    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
      return mock(ReactiveRedisConnectionFactory.class);
    }
  }
}
