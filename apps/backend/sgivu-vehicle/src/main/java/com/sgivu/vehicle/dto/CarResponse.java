package com.sgivu.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Representación externa de un auto dentro del catálogo")
public class CarResponse extends VehicleResponse {
  @Schema(description = "Tipo de carrocería", example = "Sedán")
  private String bodyType;

  @Schema(description = "Tipo de combustible", example = "Gasolina")
  private String fuelType;

  @Schema(description = "Número de puertas", example = "4")
  private Integer numberOfDoors;
}
