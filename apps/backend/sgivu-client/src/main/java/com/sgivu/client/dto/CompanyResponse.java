package com.sgivu.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Representación pública de una empresa cliente")
public class CompanyResponse extends ClientResponse {
  @Schema(description = "Identificador fiscal (RUC/NIT)", example = "900123456-7")
  private String taxId;

  @Schema(description = "Razón social de la empresa", example = "ACME SA")
  private String companyName;
}
