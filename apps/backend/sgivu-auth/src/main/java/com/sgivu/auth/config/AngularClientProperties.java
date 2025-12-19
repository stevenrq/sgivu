package com.sgivu.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** URL base del portal Angular usada en redirecciones y configuración de CORS. */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "angular-client")
public class AngularClientProperties {

  private String url;
}
