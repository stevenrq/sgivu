package com.sgivu.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(
    description =
        "Criterios de filtrado para búsqueda de usuarios (todos opcionales, se combinan con AND)")
public class UserFilterCriteria {

  @Schema(description = "Filtrar por nombre o apellido (búsqueda parcial)", example = "Juan")
  private final String name;

  @Schema(description = "Filtrar por nombre de usuario (exacto o parcial)", example = "jperez")
  private final String username;

  @Schema(description = "Filtrar por correo electrónico", example = "juan@ejemplo.com")
  private final String email;

  @Schema(description = "Filtrar por rol asignado", example = "ADMIN")
  private final String role;

  @Schema(
      description = "Filtrar por estado (true=habilitado, false=deshabilitado)",
      example = "true")
  private final Boolean enabled;
}
