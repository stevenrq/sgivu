package com.sgivu.purchasesale.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Modelo base de cliente (persona o empresa) devuelto por el microservicio de clientes. Permite
 * compartir metadatos comunes como contacto y habilitación sin duplicar lógica en este
 * microservicio.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class Client {
  private Long id;
  private Address address;
  private Long phoneNumber;
  private String email;
  private boolean isEnabled;
}
