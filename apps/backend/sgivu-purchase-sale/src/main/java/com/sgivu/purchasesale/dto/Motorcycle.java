package com.sgivu.purchasesale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * DTO para motocicletas que viaja hacia/desde el microservicio de inventario. Complementa los datos
 * básicos de {@link Vehicle} con el tipo de motocicleta para cálculos de valorización.
 */
@Schema(description = "DTO de motocicleta con atributos adicionales para valoración")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Motorcycle extends Vehicle {
  private String motorcycleType;
}
