package com.sgivu.auth.controller;

import com.sgivu.auth.config.AngularClientProperties;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Controlador para manejar las solicitudes de login y redirigir usuarios autenticados. */
@Hidden
@Controller
public class LoginController {

  private final AngularClientProperties angularClientProperties;

  public LoginController(AngularClientProperties angularClientProperties) {
    this.angularClientProperties = angularClientProperties;
  }

  @GetMapping("/")
  public String root() {
    return "redirect:/login";
  }

  @GetMapping("/login")
  public String login(
      @RequestParam(required = false) String error,
      @RequestParam(required = false) String logout,
      Model model) {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
      return "redirect:" + angularClientProperties.getUrl();
    }

    if (error != null) {
      String errorMessage = determineErrorMessage(error);
      model.addAttribute("loginError", true);
      model.addAttribute("errorMessage", errorMessage);
    }

    if (logout != null) {
      model.addAttribute("logoutSuccess", true);
      model.addAttribute("logoutMessage", "Te has desconectado exitosamente.");
    }

    return "login";
  }

  private String determineErrorMessage(String error) {
    return switch (error) {
      case "disabled" -> "Su cuenta está deshabilitada. Contacte al administrador para asistencia.";
      case "locked" -> "Su cuenta está temporalmente bloqueada. Por favor, inténtelo más tarde.";
      case "expired" -> "Su cuenta ha expirado. Contacte al administrador.";
      case "credentials" -> "Sus credenciales han expirado. Cambie su contraseña.";
      case "bad_credentials" ->
          "Credenciales inválidas. Verifique su nombre de usuario y contraseña.";
      case "service_unavailable" ->
          "El servicio no está disponible actualmente. Por favor, inténtelo más tarde.";
      default ->
          "Ocurrió un error inesperado durante la autenticación. Por favor, inténtelo de nuevo.";
    };
  }
}
