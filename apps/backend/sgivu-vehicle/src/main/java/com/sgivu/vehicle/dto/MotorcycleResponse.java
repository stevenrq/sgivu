package com.sgivu.vehicle.dto;

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
public class MotorcycleResponse extends VehicleResponse {
  private String motorcycleType;
}
