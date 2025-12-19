package com.sgivu.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** URL pública del Authorization Server usada en discovery y validación de tokens. */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "issuer")
public class IssuerProperties {

  private String url;
}
