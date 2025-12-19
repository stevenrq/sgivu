package com.sgivu.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Request para validar credenciales de usuario. */
public record CredentialsValidationRequest(
    @JsonProperty("username") String username, @JsonProperty("password") String password) {}
