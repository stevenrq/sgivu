package com.sgivu.gateway.config;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;

/**
 * Configuración de Spring Session usando Redis para el Gateway (BFF).
 *
 * <p>Esta clase proporciona la configuración necesaria para el manejo de sesiones en el gateway y
 * define un {@link WebSessionIdResolver} que utiliza cookies para almacenar el identificador de la
 * sesión. La cookie se nombra "SESSION", se marca como HttpOnly y tiene ruta "/". Se establece
 * <em>SameSite=Lax</em> para permitir flujos de redirección (por ejemplo OAuth2) sin bloquear la
 * cookie. En entornos con HTTPS se recomienda marcar la cookie como <em>Secure</em>.
 *
 * @see org.springframework.web.server.session.CookieWebSessionIdResolver
 */
@Configuration(proxyBeanMethods = false)
public class RedisSessionConfig {
  private static final Logger log = LoggerFactory.getLogger(RedisSessionConfig.class);

  @PostConstruct
  public void init() {
    log.info("Spring Session Redis Configuration loaded");
  }

  @Bean
  WebSessionIdResolver webSessionIdResolver() {
    CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
    resolver.setCookieName("SESSION");
    resolver.addCookieInitializer(
        builder -> {
          builder.path("/");
          builder.httpOnly(true);
          builder.sameSite("Lax");
          builder.maxAge(Duration.ofHours(1));
        });
    log.info("WebSessionIdResolver configured with SameSite=Lax");
    return resolver;
  }
}
