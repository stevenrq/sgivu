package com.sgivu.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sgivu.gateway.config.AngularClientProperties;
import com.sgivu.gateway.config.ServicesProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository;

class SecurityConfigTest {

  private final ReactiveWebApplicationContextRunner contextRunner =
      new ReactiveWebApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(SecurityConfig.class))
          .withUserConfiguration(TestConfig.class);

  @Test
  void authorizedClientRepositoryIsWebSessionBased() {
    this.contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(ServerOAuth2AuthorizedClientRepository.class);
          ServerOAuth2AuthorizedClientRepository repository =
              context.getBean(ServerOAuth2AuthorizedClientRepository.class);
          assertThat(repository)
              .isInstanceOf(WebSessionServerOAuth2AuthorizedClientRepository.class);
        });
  }

  @Configuration
  static class TestConfig {
    @Bean
    public ServicesProperties servicesProperties() {
      ServicesProperties props = mock(ServicesProperties.class);
      ServicesProperties.ServiceInfo info = new ServicesProperties.ServiceInfo();
      info.setUrl("http://sgivu-auth:9000");
      when(props.getMap()).thenReturn(Map.of("sgivu-auth", info));
      return props;
    }

    @Bean
    public AngularClientProperties angularClientProperties() {
      AngularClientProperties properties = new AngularClientProperties();
      properties.setUrl("http://localhost:4200");
      return properties;
    }

    @Bean
    public ReactiveClientRegistrationRepository clientRegistrationRepository() {
      return mock(ReactiveClientRegistrationRepository.class);
    }
  }
}
