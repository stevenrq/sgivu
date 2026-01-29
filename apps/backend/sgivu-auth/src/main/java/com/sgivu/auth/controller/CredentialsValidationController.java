package com.sgivu.auth.controller;

import com.sgivu.auth.controller.api.CredentialsValidationApi;
import com.sgivu.auth.dto.CredentialsValidationRequest;
import com.sgivu.auth.dto.CredentialsValidationResponse;
import com.sgivu.auth.service.CredentialsValidationService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CredentialsValidationController implements CredentialsValidationApi {

  private final CredentialsValidationService credentialsValidationService;

  public CredentialsValidationController(
      CredentialsValidationService credentialsValidationService) {
    this.credentialsValidationService = credentialsValidationService;
  }

  @Override
  public CredentialsValidationResponse validateCredentials(CredentialsValidationRequest request) {
    return credentialsValidationService.validateCredentials(request.username(), request.password());
  }
}
