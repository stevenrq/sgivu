package com.sgivu.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.*;

@NoArgsConstructor
@Getter
@Setter
@Schema(description = "Envoltura estándar para respuestas de la API")
public class ApiResponse<T> {

  @Schema(description = "Datos de la respuesta exitosa")
  private T data;

  @Schema(
      description = "Mapa de errores de validación (campo -> mensaje)",
      example = "{\"email\": \"Formato de email inválido\"}")
  private Map<String, String> errors;

  public ApiResponse(T data) {
    this.data = data;
    this.errors = null;
  }

  public ApiResponse(Map<String, String> errors) {
    this.errors = errors;
    this.data = null;
  }
}
