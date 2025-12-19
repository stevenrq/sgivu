package com.sgivu.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sgivu.auth.config.AngularClientProperties;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

class LoginControllerTest {

  private final AngularClientProperties properties;

  LoginControllerTest() {
    properties = new AngularClientProperties();
    properties.setUrl("https://portal.angular");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void login_whenAuthenticated_redirectsToAngular() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("user", "pass", List.of()));

    LoginController controller = new LoginController(properties);
    String view = controller.login(null, null, new ExtendedModelMap());

    assertEquals("redirect:https://portal.angular", view);
  }

  @Test
  void login_whenBadCredentials_setsErrorMessage() {
    Model model = new ExtendedModelMap();
    LoginController controller = new LoginController(properties);

    String view = controller.login("bad_credentials", null, model);

    assertEquals("login", view);
    assertEquals(true, model.getAttribute("loginError"));
    assertEquals(
        "Credenciales inválidas. Verifique su nombre de usuario y contraseña.",
        model.getAttribute("errorMessage"));
  }

  @Test
  void login_whenLogoutParameterPresent_setsLogoutMessage() {
    Model model = new ExtendedModelMap();
    LoginController controller = new LoginController(properties);

    String view = controller.login(null, "true", model);

    assertEquals("login", view);
    assertEquals(true, model.getAttribute("logoutSuccess"));
    assertEquals("Te has desconectado exitosamente.", model.getAttribute("logoutMessage"));
  }
}
