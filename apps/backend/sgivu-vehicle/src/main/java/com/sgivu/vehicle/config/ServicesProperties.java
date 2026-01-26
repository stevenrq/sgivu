package com.sgivu.vehicle.config;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
