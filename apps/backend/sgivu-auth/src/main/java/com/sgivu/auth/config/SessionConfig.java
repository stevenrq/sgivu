package com.sgivu.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Configuración de cookies para Spring Session JDBC.
 *
 * <p>Esta configuración es necesaria para que las cookies de sesión funcionen correctamente detrás
 * de un reverse proxy (Nginx) en producción. Sin esta configuración, las cookies pueden ser
 * bloqueadas por políticas de SameSite del navegador.
 */
@Configuration
public class SessionConfig {

  private static final Logger log = LoggerFactory.getLogger(SessionConfig.class);

  /**
   * Configura el serializador de cookies para Spring Session.
   *
   * <p>Configuraciones aplicadas:
   *
   * <ul>
   *   <li><b>cookieName:</b> SESSION (nombre estándar)
   *   <li><b>cookiePath:</b> / (disponible en todo el dominio)
   *   <li><b>sameSite:</b> Lax (permite cookies en navegación de nivel superior)
   *   <li><b>httpOnly:</b> true (previene acceso vía JavaScript)
   *   <li><b>useSecureCookie:</b> false (HTTP en producción actual; cambiar a true con HTTPS)
   * </ul>
   */
  @Bean
  CookieSerializer cookieSerializer() {
    DefaultCookieSerializer serializer = new DefaultCookieSerializer();
    // Usar nombre diferente al Gateway para evitar conflictos de cookies
    serializer.setCookieName("AUTH_SESSION");
    serializer.setCookiePath("/");
    serializer.setSameSite("Lax");
    serializer.setUseHttpOnlyCookie(true);
    // En HTTP (sin TLS), useSecureCookie debe ser false.
    // Cambiar a true cuando se habilite HTTPS.
    serializer.setUseSecureCookie(false);
    log.info("Spring Session CookieSerializer configured with SameSite=Lax");
    return serializer;
  }
}
