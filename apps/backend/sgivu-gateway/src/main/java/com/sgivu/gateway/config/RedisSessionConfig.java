package com.sgivu.gateway.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;

/**
 * Configuración de Spring Session con Redis para el Gateway BFF. Define cómo se manejan las cookies
 * de sesión para la autenticación.
 */
@Configuration(proxyBeanMethods = false)
public class RedisSessionConfig {
  private static final Logger log = LoggerFactory.getLogger(RedisSessionConfig.class);

  @PostConstruct
  public void init() {
    log.info("Spring Session Redis Configuration loaded");
  }

  /**
   * Configura el resolver de ID de sesión basado en cookies. Usa SameSite=Lax para permitir el
   * envío de cookies en redirecciones OAuth2.
   */
  @Bean
  WebSessionIdResolver webSessionIdResolver() {
    CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
    resolver.setCookieName("SESSION");
    resolver.addCookieInitializer(
        builder -> {
          builder.path("/");
          builder.httpOnly(true);
          builder.sameSite("Lax"); // Lax permite cookies en redirecciones top-level
          // No usar Secure porque estamos en HTTP (sin HTTPS)
        });
    log.info("WebSessionIdResolver configured with SameSite=Lax");
    return resolver;
  }
}
