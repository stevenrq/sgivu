package com.sgivu.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Configuración del cliente confidencial usado por el gateway como BFF. */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "gateway-client")
public class GatewayClientProperties {

  private String url;
  private String secret;
}
