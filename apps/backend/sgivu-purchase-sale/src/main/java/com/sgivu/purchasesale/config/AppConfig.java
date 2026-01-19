package com.sgivu.purchasesale.config;

import com.sgivu.purchasesale.client.ClientServiceClient;
import com.sgivu.purchasesale.client.UserServiceClient;
import com.sgivu.purchasesale.client.VehicleServiceClient;
import com.sgivu.purchasesale.security.JwtAuthorizationInterceptor;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Configura los clientes HTTP declarativos que conectan con los microservicios SGIVU (clientes,
 * usuarios e inventario de vehículos). Propaga el JWT actual y la clave interna para autorizar
 * llamadas de servicio a servicio respetando la política de seguridad de la plataforma.
 */
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

  /**
   * Proxy hacia el microservicio de clientes. Incluye la cabecera interna para autorizarse como
   * servicio confiable.
   *
   * @param restClientBuilder builder con balanceo y propagación de JWT
   * @return cliente declarativo de clientes
   */
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

  /**
   * Proxy hacia el microservicio de usuarios (gestores internos).
   *
   * @param restClientBuilder builder base para construir el cliente HTTP
   * @return cliente declarativo de usuarios
   */
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

  /**
   * Proxy hacia el microservicio de inventario de vehículos usados.
   *
   * @param restClientBuilder builder base para invocar inventario
   * @return cliente declarativo de vehículos
   */
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

  /**
   * Obtiene la URL base de un servicio a partir de su clave en la configuración.
   *
   * @param serviceKey clave del servicio
   * @return URL base del servicio
   * @throws IllegalStateException si no se encuentra la configuración del servicio o su URL
   */
  private @NonNull String serviceUrl(String serviceKey) {
    var svc = servicesProperties.getMap().get(serviceKey);
    if (svc == null) {
      throw new IllegalStateException(
          "Falta la configuración del servicio para la clave: " + serviceKey);
    }

    String url = svc.getUrl();
    if (!StringUtils.hasText(url)) {
      throw new IllegalStateException("Falta la URL del servicio para la clave: " + serviceKey);
    }

    return Objects.requireNonNull(url);
  }
}
