package com.sgivu.auth.controller;

import com.sgivu.auth.config.AngularClientProperties;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Endpoint de logout SSO alternativo para el caso en que el RP-Initiated Logout (OIDC) no puede
 * completarse (por ejemplo, cuando la sesión del gateway ha expirado y no hay {@code id_token_hint}
 * disponible).
 *
 * <p>El gateway redirige a este endpoint como fallback cuando no dispone de un {@code OidcUser} en
 * la sesión para construir la URL de {@code /connect/logout} con {@code id_token_hint}. Este
 * endpoint invalida la sesión HTTP del auth server (respaldada en JDBC) y redirige al cliente
 * Angular, evitando que la sesión persistente del auth server cause una re-autenticación
 * automática.
 *
 * @see com.sgivu.auth.security.SecurityConfig
 */
@Hidden
@Controller
public class SsoLogoutController {

  private static final Logger log = LoggerFactory.getLogger(SsoLogoutController.class);

  private final AngularClientProperties angularClientProperties;

  public SsoLogoutController(AngularClientProperties angularClientProperties) {
    this.angularClientProperties = angularClientProperties;
  }

  /**
   * Invalida la sesión HTTP del auth server y redirige al {@code redirect_uri} proporcionado.
   *
   * @param redirectUri URI de redirección después de cerrar sesión. Se valida para prevenir ataques
   *     de redirección abierta; solo se aceptan URIs que empiecen con la URL registrada del cliente
   *     Angular.
   * @param request la petición HTTP entrante
   * @param response la respuesta HTTP saliente
   * @throws IOException si ocurre un error al enviar la redirección
   */
  @GetMapping("/sso-logout")
  public void ssoLogout(
      @RequestParam(name = "redirect_uri", required = false) String redirectUri,
      HttpServletRequest request,
      HttpServletResponse response)
      throws IOException {

    HttpSession session = request.getSession(false);
    if (session != null) {
      log.info("Invalidating auth server session (SSO logout fallback).");
      session.invalidate();
    }
    SecurityContextHolder.clearContext();

    String targetUrl = angularClientProperties.getUrl().concat("/login");
    if (redirectUri != null && isValidRedirectUri(redirectUri)) {
      targetUrl = redirectUri;
    }

    log.debug("SSO logout completed. Redirecting to: {}", targetUrl);
    response.sendRedirect(targetUrl);
  }

  private boolean isValidRedirectUri(String redirectUri) {
    String allowedBase = angularClientProperties.getUrl();
    return redirectUri.equals(allowedBase) || redirectUri.startsWith(allowedBase + "/");
  }
}
