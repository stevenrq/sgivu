package com.sgivu.purchasesale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Schema(
    description =
        "Resumen compacto de un vehículo usado con campos clave para identificación y estado")
@Value
@Builder
public class VehicleSummary {
  Long id;
  String type;
  String brand;
  String line;
  String model;
  String plate;
  String status;
}
