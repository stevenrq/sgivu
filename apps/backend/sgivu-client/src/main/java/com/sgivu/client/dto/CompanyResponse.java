package com.sgivu.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Respuesta pública de una empresa. Usada para contratos corporativos y control de proveedores en
 * inventario.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CompanyResponse extends ClientResponse {
  private String taxId;
  private String companyName;
}
