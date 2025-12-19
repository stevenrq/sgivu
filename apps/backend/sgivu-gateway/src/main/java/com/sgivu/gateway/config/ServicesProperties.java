package com.sgivu.gateway.config;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propiedades externas de endpoints de microservicios SGIVU. Permite resolver URLs de autorización,
 * inventario, clientes y compra-venta para integrar el Gateway con ambientes on-prem o cloud sin
 * recompilar.
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "services")
public class ServicesProperties {

  /**
   * Mapa dinámico de servicios. La clave es el identificador del servicio (ej: sgivu-auth) usado
   * por discovery y como issuer de JWT.
   */
  private Map<String, ServiceInfo> map;

  @Setter
  @Getter
  public static class ServiceInfo {
    private String name;
    private String url;
  }
}
