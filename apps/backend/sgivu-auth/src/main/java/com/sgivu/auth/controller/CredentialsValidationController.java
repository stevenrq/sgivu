package com.sgivu.auth.controller;

import com.sgivu.auth.dto.CredentialsValidationRequest;
import com.sgivu.auth.dto.CredentialsValidationResponse;
import com.sgivu.auth.service.CredentialsValidationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Valida credenciales en tiempo real durante el proceso de login. Permite mostrar al usuario
 * retroalimentación inmediata sobre la validez de sus credenciales sin necesidad de enviar el
 * formulario completo.
 */
@RestController
public class CredentialsValidationController {

  private final CredentialsValidationService credentialsValidationService;

  public CredentialsValidationController(
      CredentialsValidationService credentialsValidationService) {
    this.credentialsValidationService = credentialsValidationService;
  }

  /**
   * Valida las credenciales proporcionadas (username y password) en tiempo real.
   *
   * <p>Retorna un JSON con el resultado de la validación: - {valid: true} si las credenciales son
   * válidas - {valid: false, reason: "invalid_credentials"} si la contraseña es incorrecta -
   * {valid: false, reason: "disabled"} si la cuenta está deshabilitada - {valid: false, reason:
   * "locked"} si la cuenta está bloqueada - {valid: false, reason: "expired"} si la cuenta ha
   * expirado - {valid: false, reason: "credentials"} si las credenciales han expirado - {valid:
   * false, reason: "service_unavailable"} si el servicio de usuarios no responde
   *
   * @param request DTO con username y password a validar.
   * @return CredentialsValidationResponse con el resultado de la validación.
   */
  @PostMapping(
      value = "/api/validate-credentials",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public CredentialsValidationResponse validateCredentials(
      @RequestBody CredentialsValidationRequest request) {
    return credentialsValidationService.validateCredentials(request.username(), request.password());
  }
}
