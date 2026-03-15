package com.sgivu.client.dto;

import com.sgivu.client.entity.Address;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO base para clientes expuesto por el servicio de clientes")
public abstract class ClientResponse {
  @Schema(description = "Identificador único del cliente", example = "1")
  private Long id;

  @Schema(description = "Dirección física del cliente")
  private Address address;

  @Schema(description = "Número de teléfono", example = "3001234567")
  private Long phoneNumber;

  @Schema(description = "Correo electrónico de contacto", example = "cliente@ejemplo.com")
  private String email;

  @Schema(description = "Indica si el cliente está habilitado", example = "true")
  private boolean enabled;
}
