package com.sgivu.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Representación externa de una motocicleta del inventario.
 *
 * <p>Incluye tipo de moto para que contratos y pricing puedan aplicar reglas específicas sin
 * acoplarse al modelo JPA.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Representación externa de una motocicleta del inventario")
public class MotorcycleResponse extends VehicleResponse {
  @Schema(description = "Tipo de motocicleta", example = "Deportiva")
  private String motorcycleType;
}
