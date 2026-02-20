package com.sgivu.gateway.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;

/**
 * Configuración de Spring Session usando Redis para el Gateway (BFF).
 *
 * <p>Define un {@link WebSessionIdResolver} basado en cookies para almacenar el identificador de
 * sesión. La cookie se nombra {@code SESSION}, se marca como {@code HttpOnly} y tiene ruta {@code
 * /}. Se establece {@code SameSite=Lax} para permitir flujos de redirección OAuth2 sin bloquear la
 * cookie. En entornos con HTTPS se recomienda agregar {@code Secure=true}.
 *
 * <h3>Estrategia de expiración (session cookie, sin {@code maxAge})</h3>
 *
 * <p>La cookie <strong>no define {@code maxAge}</strong>, lo que la convierte en una <em>session
 * cookie</em> que el navegador elimina al cerrarse. El tiempo de vida real de la sesión lo controla
 * Redis a través de {@code spring.session.timeout} (configurado en {@code sgivu-gateway.yml}), que
 * funciona con <strong>expiración deslizante</strong>: el TTL se resetea en cada request.
 *
 * <p><strong>¿Por qué no usar {@code maxAge}?</strong> {@link CookieWebSessionIdResolver} emite la
 * cookie solo al crear la sesión (login). Si se establece {@code maxAge=1h}, el navegador la
 * eliminará exactamente 1 hora después del login, incluso si el usuario estuvo activo y la sesión
 * Redis sigue viva (TTL deslizante). Esto provoca cierre de sesión prematuro cuando el usuario está
 * activo.
 *
 * @see org.springframework.web.server.session.CookieWebSessionIdResolver
 */
@Configuration(proxyBeanMethods = false)
public class RedisSessionConfig {
  private static final Logger log = LoggerFactory.getLogger(RedisSessionConfig.class);

  @PostConstruct
  public void init() {
    log.info(
        "Spring Session Redis configuration loaded — cookie strategy: session cookie (no maxAge)");
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
          // Sin maxAge: session cookie. La expiración la controla Redis (sliding timeout).
          // Definir maxAge aquí causaría expiración absoluta desde el login, ignorando actividad.
        });
    log.info("WebSessionIdResolver configured — SESSION cookie: HttpOnly, SameSite=Lax, path=/");
    return resolver;
  }
}
