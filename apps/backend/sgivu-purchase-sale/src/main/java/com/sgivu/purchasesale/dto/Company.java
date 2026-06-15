package com.sgivu.purchasesale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Schema(description = "Cliente corporativo con NIT y razón social para contratos con empresas")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Company extends Client {
  private String taxId;
  private String companyName;
}
