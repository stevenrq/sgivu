package com.sgivu.gateway.controller.api;

import com.sgivu.gateway.dto.AuthSessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Tag(name = "Auth (BFF)", description = "Estado de sesión expuesto por el BFF para la SPA")
@RequestMapping("/auth")
public interface AuthSessionApi {

  @Operation(
      summary = "Obtener estado de sesión autenticada",
      description =
          "Devuelve información de la sesión del usuario autenticado (claims) expuesta por el BFF"
              + " para la SPA.")
  @ApiResponse(
      responseCode = "200",
      description = "Sesión autenticada",
      content = @Content(schema = @Schema(implementation = AuthSessionResponse.class)))
  @ApiResponse(responseCode = "401", description = "No autenticado")
  @GetMapping("/session")
  Mono<ResponseEntity<AuthSessionResponse>> session(
      Authentication authentication, ServerWebExchange exchange);
}
