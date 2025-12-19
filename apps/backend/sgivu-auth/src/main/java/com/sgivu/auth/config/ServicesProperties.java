package com.sgivu.auth.config;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Catálogo de endpoints de microservicios SGIVU para construir clientes REST balanceados. */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "services")
public class ServicesProperties {

  private Map<String, ServiceInfo> map;

  @Setter
  @Getter
  public static class ServiceInfo {
    private String name;
    private String url;
  }
}
