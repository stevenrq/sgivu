package com.sgivu.vehicle.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

/**
 * AuthorizationManager personalizado que solo autoriza solicitudes provenientes de servicios
 * internos del ecosistema.
 *
 * <p>Se usa para permitir llamadas server-to-server (ej. microservicio de contratos o predicción)
 * sin requerir JWT de usuario, validando un secreto compartido vía encabezado {@code
 * X-Internal-Service-Key}. Reduce latencia en flujos internos manteniendo aislamiento entre
 * clientes externos e internos.
 *
 * @apiNote La clave se lee de {@code service.internal.secret-key}; mantenerla alineada en todos los
 *     servicios SGIVU.
 */
@Component
public class InternalServiceAuthorizationManager
    implements AuthorizationManager<RequestAuthorizationContext> {

  private static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";

  private final String internalServiceKey;

  /**
   * Inyecta la clave compartida entre servicios SGIVU para autorizar llamadas internas.
   *
   * @param internalServiceKey secreto definido en configuración
   */
  public InternalServiceAuthorizationManager(
      @Value("${service.internal.secret-key}") String internalServiceKey) {
    this.internalServiceKey = internalServiceKey;
  }

  /**
   * Valida que la solicitud incluya el encabezado esperado con la clave interna configurada.
   *
   * @param authentication proveedor de autenticación (no utilizado en esta validación)
   * @param context contexto de la solicitud HTTP
   * @return decisión de autorización basada en la coincidencia de la clave
   */
  @Override
  public AuthorizationDecision check(
      Supplier<Authentication> authentication, RequestAuthorizationContext context) {
    HttpServletRequest request = context.getRequest();
    String providedKey = request.getHeader(INTERNAL_KEY_HEADER);

    boolean isKeyValid = internalServiceKey.equals(providedKey);

    return new AuthorizationDecision(isKeyValid);
  }
}
