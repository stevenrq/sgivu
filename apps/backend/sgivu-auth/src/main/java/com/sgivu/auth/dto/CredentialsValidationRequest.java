package com.sgivu.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud de validación de credenciales de usuario")
public record CredentialsValidationRequest(
    @Schema(
            description = "Nombre de usuario a validar",
            example = "jperez",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("username")
        String username,
    @Schema(
            description = "Contraseña a validar",
            example = "SecureP@ss123",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("password")
        String password) {}
