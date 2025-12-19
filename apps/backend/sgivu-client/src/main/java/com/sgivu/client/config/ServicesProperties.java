package com.sgivu.client.config;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Propiedades para resolver URLs de servicios SGIVU en tiempo de ejecución. */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "services")
public class ServicesProperties {

  /** Servicios registrados (auth, inventario, predicción) parametrizados por entorno. */
  private Map<String, ServiceInfo> map;

  @Setter
  @Getter
  public static class ServiceInfo {
    private String name;
    private String url;
  }
}
