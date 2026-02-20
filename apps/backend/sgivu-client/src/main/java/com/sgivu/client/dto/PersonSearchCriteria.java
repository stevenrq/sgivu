package com.sgivu.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Criterios de búsqueda para personas (todas las propiedades opcionales)")
public class PersonSearchCriteria {
  @Schema(description = "Nombre o apellido para búsqueda parcial", example = "Juan")
  private final String name;

  @Schema(description = "Correo electrónico para filtrar", example = "juan.perez@ejemplo.com")
  private final String email;

  @Schema(description = "Documento nacional (número)", example = "1234567890")
  private final Long nationalId;

  @Schema(description = "Número de teléfono", example = "3001234567")
  private final Long phoneNumber;

  @Schema(description = "Estado habilitado (true/false)", example = "true")
  private final Boolean enabled;

  @Schema(description = "Ciudad del domicilio", example = "Bogotá")
  private final String city;
}
