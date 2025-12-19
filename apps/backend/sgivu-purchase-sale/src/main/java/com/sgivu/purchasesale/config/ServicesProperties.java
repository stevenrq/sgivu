package com.sgivu.purchasesale.config;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Mapa de endpoints de microservicios SGIVU configurados en application.yaml. Centraliza URLs y
 * evita propagar strings mágicos en la creación de clientes HTTP.
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "services")
public class ServicesProperties {

  private Map<String, ServiceInfo> map;

  /** Descriptor de un microservicio externo configurado en application.yaml. */
  @Setter
  @Getter
  public static class ServiceInfo {
    /** Nombre lógico del servicio (usado para identificación en logs/métricas). */
    private String name;
    /** URL base resuelta por Spring Cloud LoadBalancer. */
    private String url;
  }
}
