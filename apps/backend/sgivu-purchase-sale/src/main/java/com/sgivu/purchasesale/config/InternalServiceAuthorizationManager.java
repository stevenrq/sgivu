package com.sgivu.purchasesale.config;

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
 * <p>Esta clase implementa {@link AuthorizationManager<RequestAuthorizationContext>} y se encarga
 * de autorizar peticiones HTTP basándose en un encabezado específico que contiene una clave secreta
 * compartida entre servicios internos.
 *
 * <p>La autorización se realiza verificando el valor del encabezado {@code X-Internal-Service-Key}
 * y comparándolo con la clave interna configurada en la propiedad {@code
 * service.internal.secret-key}.
 *
 * <p>Uso típico: esta clase se registra como un componente de Spring y se puede utilizar en la
 * configuración de seguridad para restringir el acceso a ciertos endpoints únicamente a servicios
 * internos confiables.
 *
 * @author Steven
 * @version 1.0
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
   * Evalúa si la solicitud contiene la cabecera con la clave interna esperada.
   *
   * @param authentication proveedor del contexto de autenticación actual (no se utiliza)
   * @param context contexto que expone la petición HTTP
   * @return decisión positiva únicamente cuando la clave coincide
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
