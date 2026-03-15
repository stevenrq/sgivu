package com.sgivu.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Configuración de cookies para Spring Session JDBC.
 *
 * <p>Configura el {@link CookieSerializer} usado por Spring Session para gestionar la cookie de
 * sesión cuando la aplicación está detrás de un reverse proxy (p. ej. Nginx). Ajustes principales:
 *
 * <ul>
 *   <li>Nombre de la cookie: {@code AUTH_SESSION}
 *   <li>Ruta (path): {@code /}
 *   <li>{@code SameSite}: {@code Lax}
 *   <li>{@code HttpOnly}: {@code true}
 *   <li>{@code UseSecureCookie}: {@code false} (ajustar a {@code true} en entornos con HTTPS)
 * </ul>
 *
 * Esta configuración evita que los navegadores bloqueen la cookie por políticas de SameSite y
 * permite un comportamiento consistente en entornos de producción y desarrollo.
 */
@Configuration
public class SessionConfig {

  /**
   * Crea y registra un {@link CookieSerializer} configurado para las cookies de sesión.
   *
   * <p>Los valores configurados son adecuados para ejecutarse detrás de un proxy reverso:
   *
   * <ul>
   *   <li>Nombre de cookie: {@code AUTH_SESSION}
   *   <li>Ruta: {@code /}
   *   <li>{@code SameSite}: {@code Lax}
   *   <li>{@code HttpOnly}: {@code true}
   *   <li>{@code UseSecureCookie}: {@code false} (ajustar a {@code true} si sólo se sirve por
   *       HTTPS)
   * </ul>
   *
   * @return instancia configurada de {@link CookieSerializer}
   */
  @Bean
  CookieSerializer cookieSerializer() {
    DefaultCookieSerializer serializer = new DefaultCookieSerializer();
    serializer.setCookieName("AUTH_SESSION");
    serializer.setCookiePath("/");
    serializer.setSameSite("Lax");
    serializer.setUseHttpOnlyCookie(true);
    serializer.setUseSecureCookie(false);
    return serializer;
  }
}
