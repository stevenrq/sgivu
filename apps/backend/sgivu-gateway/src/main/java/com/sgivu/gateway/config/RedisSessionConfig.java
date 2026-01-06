package com.sgivu.gateway.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RedisSessionConfig {
  private static final Logger log = LoggerFactory.getLogger(RedisSessionConfig.class);

  @PostConstruct
  public void init() {
    log.info("Spring Session Redis Configuration loaded");
  }
}
