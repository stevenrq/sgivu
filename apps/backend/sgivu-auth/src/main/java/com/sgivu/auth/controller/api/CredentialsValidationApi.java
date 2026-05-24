package com.sgivu.auth.controller.api;

import com.sgivu.auth.dto.CredentialsValidationRequest;
import com.sgivu.auth.dto.CredentialsValidationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(
    name = "Validación de Credenciales",
    description = "Validación en tiempo real de credenciales durante el proceso de login")
public interface CredentialsValidationApi {

  @Operation(
      summary = "Validar credenciales de usuario",
      description =
          "Valida username y password en tiempo real para proporcionar retroalimentación "
              + "inmediata en el formulario de login sin enviar el formulario completo. "
              + "Retorna el estado de validación y, en caso de error, el motivo específico.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Credenciales validadas (ver campo 'valid' para resultado)",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CredentialsValidationResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Request malformado o campos faltantes",
            content = @Content),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor",
            content = @Content)
      })
  @PostMapping(
      value = "/api/validate-credentials",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  CredentialsValidationResponse validateCredentials(
      @Parameter(description = "Credenciales a validar", required = true) @RequestBody
          CredentialsValidationRequest request);
}
