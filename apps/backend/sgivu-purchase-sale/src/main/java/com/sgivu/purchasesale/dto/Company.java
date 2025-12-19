package com.sgivu.purchasesale.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Cliente corporativo proveniente del microservicio de clientes. Complementa la jerarquía con NIT y
 * razón social para contratos con empresas.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Company extends Client {
  private String taxId;
  private String companyName;
}
