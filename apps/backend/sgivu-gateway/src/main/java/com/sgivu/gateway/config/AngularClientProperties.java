package com.sgivu.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propiedades del cliente Angular oficial de SGIVU. Se usa para limitar CORS al dominio autorizado
 * donde operan los asesores y analistas de inventario.
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "angular-client")
public class AngularClientProperties {

  private String url;
}
