package com.sgivu.vehicle.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Representación externa de un auto dentro del catálogo.
 *
 * <p>Amplía {@link VehicleResponse} con atributos utilizados por los flujos de compra y seguros
 * (carrocería, combustible, número de puertas) sin exponer detalles internos.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CarResponse extends VehicleResponse {
  private String bodyType;
  private String fuelType;
  private Integer numberOfDoors;
}
