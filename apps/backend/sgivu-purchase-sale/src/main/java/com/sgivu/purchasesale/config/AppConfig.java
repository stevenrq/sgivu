package com.sgivu.purchasesale.config;

import com.sgivu.purchasesale.client.ClientServiceClient;
import com.sgivu.purchasesale.client.UserServiceClient;
import com.sgivu.purchasesale.client.VehicleServiceClient;
import com.sgivu.purchasesale.config.ServicesProperties.ServiceInfo;
import com.sgivu.purchasesale.security.JwtAuthorizationInterceptor;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class AppConfig {

  private static final String INTERNAL_SERVICE_KEY_HEADER = "X-Internal-Service-Key";
  private final ServicesProperties servicesProperties;

  @Value("${service.internal.secret-key}")
  private String internalServiceKey;

  public AppConfig(ServicesProperties servicesProperties) {
    this.servicesProperties = servicesProperties;
  }

  /**
   * Builder para RestClient con load balancer y propagación de JWT. Permite que las llamadas
   * salientes honren la identidad del usuario autenticado y pasen por el Discovery Client de Spring
   * Cloud. Se inyecta explícitamente con @Qualifier en clientes de servicios de negocio.
   *
   * @param jwtAuthorizationInterceptor interceptor que copia el JWT vigente
   * @return builder preconfigurado listo para clonar
   */
  @Bean("loadBalancedRestClientBuilder")
  @LoadBalanced
  RestClient.Builder loadBalancedRestClientBuilder(
      JwtAuthorizationInterceptor jwtAuthorizationInterceptor) {
    return RestClient.builder().requestInterceptors(list -> list.add(jwtAuthorizationInterceptor));
  }

  /**
   * Builder para RestClient sin balanceo de carga. Marcado como @Primary para que Eureka y otros
   * componentes de infraestructura lo usen por defecto, evitando la circularidad donde Eureka
   * necesita LoadBalancer pero LoadBalancer necesita Eureka ya inicializado.
   *
   * @return builder sin balanceo de carga
   */
  @Bean
  @Primary
  RestClient.Builder restClientBuilder() {
    return RestClient.builder();
  }

  @Bean
  ClientServiceClient clientServiceClient(
      @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder restClientBuilder) {
    RestClient restClient =
        restClientBuilder
            .clone()
            .baseUrl(serviceUrl("sgivu-client"))
            .defaultHeader(INTERNAL_SERVICE_KEY_HEADER, internalServiceKey)
            .build();

    RestClientAdapter adapter = RestClientAdapter.create(restClient);
    HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
    return factory.createClient(ClientServiceClient.class);
  }

  @Bean
  UserServiceClient userServiceClient(
      @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder restClientBuilder) {
    RestClient restClient =
        restClientBuilder
            .clone()
            .baseUrl(serviceUrl("sgivu-user"))
            .defaultHeader(INTERNAL_SERVICE_KEY_HEADER, internalServiceKey)
            .build();

    RestClientAdapter adapter = RestClientAdapter.create(restClient);
    HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
    return factory.createClient(UserServiceClient.class);
  }

  @Bean
  VehicleServiceClient vehicleServiceClient(
      @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder restClientBuilder) {
    RestClient restClient =
        restClientBuilder
            .clone()
            .baseUrl(serviceUrl("sgivu-vehicle"))
            .defaultHeader(INTERNAL_SERVICE_KEY_HEADER, internalServiceKey)
            .build();

    RestClientAdapter adapter = RestClientAdapter.create(restClient);
    HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
    return factory.createClient(VehicleServiceClient.class);
  }

  private String serviceUrl(String serviceKey) {
    ServiceInfo serviceInfo = servicesProperties.getMap().get(serviceKey);
    if (serviceInfo == null) {
      throw new IllegalStateException("Missing service configuration for key: " + serviceKey);
    }

    String url = serviceInfo.getUrl();
    if (!StringUtils.hasText(url)) {
      throw new IllegalStateException("Missing service URL for key: " + serviceKey);
    }

    return Objects.requireNonNull(url);
  }
}
