package com.sgivu.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Criterios de búsqueda para empresas (todas las propiedades opcionales)")
public class CompanySearchCriteria {
  @Schema(description = "Razón social para búsqueda parcial", example = "ACME")
  private final String companyName;

  @Schema(description = "Identificador fiscal (RUC/NIT)", example = "900123456-7")
  private final String taxId;

  @Schema(description = "Correo electrónico para filtrar", example = "contacto@acme.com")
  private final String email;

  @Schema(description = "Número de teléfono", example = "6013001234")
  private final Long phoneNumber;

  @Schema(description = "Estado habilitado (true/false)", example = "true")
  private final Boolean enabled;

  @Schema(description = "Ciudad del domicilio fiscal", example = "Medellín")
  private final String city;
}
