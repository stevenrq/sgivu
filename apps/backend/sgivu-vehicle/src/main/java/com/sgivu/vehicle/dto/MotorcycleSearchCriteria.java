package com.sgivu.vehicle.dto;

import com.sgivu.vehicle.enums.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Filtros dinámicos para consultas de motocicletas")
public class MotorcycleSearchCriteria {
  @Schema(description = "Placa", example = "ABC123")
  private final String plate;

  @Schema(description = "Marca", example = "Honda")
  private final String brand;

  @Schema(description = "Línea", example = "CB")
  private final String line;

  @Schema(description = "Modelo", example = "CB500")
  private final String model;

  @Schema(description = "Tipo de moto", example = "Deportiva")
  private final String motorcycleType;

  @Schema(description = "Transmisión", example = "Manual")
  private final String transmission;

  @Schema(description = "Ciudad registrada", example = "Madrid")
  private final String cityRegistered;

  @Schema(description = "Estado del vehículo", example = "AVAILABLE")
  private final VehicleStatus status;

  @Schema(description = "Año mínimo", example = "2010")
  private final Integer minYear;

  @Schema(description = "Año máximo", example = "2022")
  private final Integer maxYear;

  @Schema(description = "Cilindraje mínimo", example = "125")
  private final Integer minCapacity;

  @Schema(description = "Cilindraje máximo", example = "1000")
  private final Integer maxCapacity;

  @Schema(description = "Kilometraje mínimo", example = "0")
  private final Integer minMileage;

  @Schema(description = "Kilometraje máximo", example = "200000")
  private final Integer maxMileage;

  @Schema(description = "Precio de venta mínimo", example = "500.0")
  private final Double minSalePrice;

  @Schema(description = "Precio de venta máximo", example = "20000.0")
  private final Double maxSalePrice;
}
