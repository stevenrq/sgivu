package com.sgivu.purchasesale.security;

import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Interceptor que copia el JWT del contexto de seguridad actual en la cabecera Authorization de las
 * llamadas que realiza el {@link org.springframework.web.client.RestClient} a otros microservicios.
 */
@Component
public class JwtAuthorizationInterceptor implements ClientHttpRequestInterceptor {

  /**
   * Copia el token JWT autenticado en la cabecera Authorization de la petición saliente si aún no
   * existe.
   *
   * @param request petición HTTP en curso
   * @param body cuerpo serializado
   * @param execution cadena de ejecución del interceptor
   * @return respuesta del servicio remoto
   * @throws IOException error de I/O durante la invocación
   */
  @Override
  @NonNull
  public ClientHttpResponse intercept(
      @NonNull HttpRequest request,
      @NonNull byte[] body,
      @NonNull ClientHttpRequestExecution execution)
      throws IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String tokenValue = extractTokenValue(authentication);

    if (tokenValue != null && !tokenValue.isBlank()) {
      HttpHeaders headers = request.getHeaders();
      if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
        headers.setBearerAuth(tokenValue);
      }
    }

    return execution.execute(request, body);
  }

  /**
   * Obtiene el valor del token JWT desde el contexto de Spring Security (si existe).
   *
   * @param authentication objeto de autenticación actual
   * @return token en formato string o {@code null} si no aplica
   */
  @Nullable
  private String extractTokenValue(@Nullable Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
      return jwtAuthenticationToken.getToken().getTokenValue();
    }
    return null;
  }
}
