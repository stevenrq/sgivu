package com.sgivu.client.dto;

import com.sgivu.client.entity.Address;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO base para exponer clientes a otros microservicios y frontales. Incluye datos de contacto
 * necesarios para contratos y logística.
 */
@Data
@NoArgsConstructor
public abstract class ClientResponse {
  private Long id;
  private Address address;
  private Long phoneNumber;
  private String email;
  private boolean enabled;
}
