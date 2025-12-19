package com.sgivu.purchasesale.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * DTO para motocicletas que viaja hacia/desde el microservicio de inventario. Complementa los
 * datos básicos de {@link Vehicle} con el tipo de motocicleta para cálculos de valorización.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Motorcycle extends Vehicle {
  private String motorcycleType;
}
