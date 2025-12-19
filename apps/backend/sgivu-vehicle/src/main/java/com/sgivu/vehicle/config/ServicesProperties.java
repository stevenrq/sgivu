package com.sgivu.vehicle.config;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Mapea la configuración de endpoints de otros microservicios SGIVU.
 *
 * <p>Facilita la obtención de URLs (ej. Authorization Server) sin hardcodear valores en la lógica
 * de seguridad o clientes HTTP.
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "services")
public class ServicesProperties {

  private Map<String, ServiceInfo> map;

  /**
   * Datos de un servicio remoto del ecosistema.
   *
   * <p>Incluye nombre lógico y URL base usada por clientes HTTP o decodificadores JWT.
   */
  @Setter
  @Getter
  public static class ServiceInfo {
    private String name;
    private String url;
  }
}
