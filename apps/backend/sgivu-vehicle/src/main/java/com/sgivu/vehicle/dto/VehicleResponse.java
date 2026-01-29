package com.sgivu.vehicle.dto;

import com.sgivu.vehicle.enums.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO base expuesto por la API para representar vehículos")
public class VehicleResponse {
  @Schema(
      description = "Identificador interno",
      example = "1",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  @Schema(
      description = "Marca del vehículo",
      example = "Toyota",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String brand;

  @Schema(description = "Modelo del vehículo", example = "Corolla")
  private String model;

  @Schema(description = "Capacidad en cc o unidades", example = "1600")
  private Integer capacity;

  @Schema(description = "Línea del vehículo", example = "XLE")
  private String line;

  @Schema(description = "Placa", example = "ABC123")
  private String plate;

  @Schema(description = "Número de motor", example = "MN123456")
  private String motorNumber;

  @Schema(description = "Número de serie", example = "SN123456")
  private String serialNumber;

  @Schema(description = "Número de chasis", example = "CH123456")
  private String chassisNumber;

  @Schema(description = "Color", example = "Rojo")
  private String color;

  @Schema(description = "Ciudad registrada", example = "Madrid")
  private String cityRegistered;

  @Schema(description = "Año del vehículo", example = "2018")
  private Integer year;

  @Schema(description = "Kilometraje", example = "120000")
  private Integer mileage;

  @Schema(description = "Transmisión", example = "Automática")
  private String transmission;

  @Schema(description = "Estado del vehículo", example = "AVAILABLE")
  private VehicleStatus status;

  @Schema(description = "Precio de compra", example = "5000.0")
  private Double purchasePrice;

  @Schema(description = "Precio de venta", example = "7000.0")
  private Double salePrice;
}
