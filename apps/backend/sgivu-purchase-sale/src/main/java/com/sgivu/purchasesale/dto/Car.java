package com.sgivu.purchasesale.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Representación de automóvil para comunicarse con el microservicio de inventario. Extiende los
 * atributos comunes de {@link Vehicle} con características propias necesarias para tasación y
 * controles regulatorios.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Car extends Vehicle {
  private String bodyType;
  private String fuelType;
  private Integer numberOfDoors;
}
