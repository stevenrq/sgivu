package com.sgivu.user.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

/**
 * AuthorizationManager dedicado a peticiones entre microservicios SGIVU.
 *
 * <p>Valida que las llamadas porten la cabecera {@code X-Internal-Service-Key} con la clave
 * compartida configurada. Se usa para exponer endpoints solo al Authorization Server o al API
 * Gateway cuando requiere enriquecer tokens o sincronizar inventario de usuarios sin pasar por el
 * canal público OAuth2.
 */
@Component
public class InternalServiceAuthorizationManager
    implements AuthorizationManager<RequestAuthorizationContext> {

  private static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";

  private final String internalServiceKey;

  public InternalServiceAuthorizationManager(
      @Value("${service.internal.secret-key}") String internalServiceKey) {
    this.internalServiceKey = internalServiceKey;
  }

  /**
   * Verifica que la petición incluya la cabecera interna con la clave compartida.
   *
   * @param authentication autenticación actual (no se usa para la validación).
   * @param context contexto de autorización con el {@link HttpServletRequest}.
   * @return decisión afirmativa solo cuando la clave coincide.
   */
  @Override
  public AuthorizationDecision check(
      Supplier<Authentication> authentication, RequestAuthorizationContext context) {
    HttpServletRequest request = context.getRequest();
    String providedKey = request.getHeader(INTERNAL_KEY_HEADER);

    // El secreto evita depender únicamente del JWT cuando el flujo es estrictamente interno.
    boolean isKeyValid = internalServiceKey.equals(providedKey);

    return new AuthorizationDecision(isKeyValid);
  }
}
