package com.sgivu.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Respuesta pública de una persona. Alimenta flujos de compra/venta y validaciones de identidad en
 * contratos.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PersonResponse extends ClientResponse {
  private Long nationalId;
  private String firstName;
  private String lastName;
}
