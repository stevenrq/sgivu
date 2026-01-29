package com.sgivu.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de validación de credenciales")
public record CredentialsValidationResponse(
    @Schema(
            description = "Indica si las credenciales son válidas",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("valid")
        boolean valid,
    @Schema(
            description =
                "Motivo del fallo de validación. Valores posibles: invalid_credentials, disabled,"
                    + " locked, expired, credentials, service_unavailable. Null si valid=true.",
            example = "invalid_credentials",
            nullable = true)
        @JsonProperty("reason")
        String reason) {}
