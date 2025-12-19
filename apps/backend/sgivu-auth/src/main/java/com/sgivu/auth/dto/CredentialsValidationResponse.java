package com.sgivu.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Response de validación de credenciales. */
public record CredentialsValidationResponse(
    @JsonProperty("valid") boolean valid, @JsonProperty("reason") String reason) {}
