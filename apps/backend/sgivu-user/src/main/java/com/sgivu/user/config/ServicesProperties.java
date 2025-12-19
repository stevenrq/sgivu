package com.sgivu.user.config;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Mapea desde configuración los endpoints internos conocidos del ecosistema SGIVU.
 *
 * <p>Se consume principalmente para resolver la URL del Authorization Server y validar JWT
 * firmados, pero permite registrar otros servicios (p. ej. FastAPI de predicción) para construir
 * clientes HTTP seguros sin acoplarlos a valores hardcodeados.
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "services")
public class ServicesProperties {

  /**
   * Mapa dinámico de servicios; la clave es el identificador lógico del servicio (ej. {@code
   * sgivu-auth} o {@code sgivu-prediction}) y el valor contiene metadatos mínimos para accederlo.
   */
  private Map<String, ServiceInfo> map;

  @Setter
  @Getter
  public static class ServiceInfo {
    /** Nombre legible del servicio para auditoría y métricas. */
    private String name;
    /** URL base publicada en el Discovery/Config Server para comunicaciones inter-servicio. */
    private String url;
  }
}
