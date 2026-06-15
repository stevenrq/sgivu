package com.sgivu.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Representación pública de una persona cliente")
public class PersonResponse extends ClientResponse {
  @Schema(description = "Documento nacional (número)", example = "1234567890")
  private Long nationalId;

  @Schema(description = "Nombre del contacto", example = "Juan")
  private String firstName;

  @Schema(description = "Apellido del contacto", example = "Pérez")
  private String lastName;
}
