package com.sgivu.client.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

/**
 * AuthorizationManager que permite llamadas servicio-servicio mediante la clave interna {@code
 * X-Internal-Service-Key}, evitando exigir JWT en integraciones de backoffice.
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

  @Override
  public AuthorizationDecision check(
      Supplier<Authentication> authentication, RequestAuthorizationContext context) {
    HttpServletRequest request = context.getRequest();
    String providedKey = request.getHeader(INTERNAL_KEY_HEADER);

    boolean isKeyValid = internalServiceKey.equals(providedKey);

    return new AuthorizationDecision(isKeyValid);
  }
}
