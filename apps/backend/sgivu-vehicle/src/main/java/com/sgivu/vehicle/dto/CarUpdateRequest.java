package com.sgivu.vehicle.dto;

import com.sgivu.vehicle.enums.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Schema(description = "Payload para actualizar datos de autos desde capas externas")
public class CarUpdateRequest {

  @Schema(description = "Marca", example = "Toyota", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank
  @Column(nullable = false, length = 20)
  private String brand;

  @Schema(description = "Modelo", example = "Corolla")
  @NotBlank
  @Column(nullable = false, length = 20)
  private String model;

  @NotNull
  @Column(nullable = false)
  private Integer capacity;

  @NotBlank
  @Column(nullable = false, length = 20)
  private String line;

  @NotBlank
  @Column(nullable = false, unique = true, length = 10)
  private String plate;

  @NotBlank
  @Column(name = "motor_number", nullable = false, unique = true, length = 30)
  private String motorNumber;

  @NotBlank
  @Column(name = "serial_number", nullable = false, unique = true, length = 30)
  private String serialNumber;

  @NotBlank
  @Column(name = "chassis_number", nullable = false, unique = true, length = 30)
  private String chassisNumber;

  @NotBlank
  @Column(nullable = false, length = 20)
  private String color;

  @NotBlank
  @Column(name = "city_registered", nullable = false, length = 30)
  private String cityRegistered;

  @Min(1950)
  @Max(2050)
  @NotNull
  @Column(nullable = false)
  private Integer year;

  @Min(0)
  @NotNull
  @Column(nullable = false)
  private Integer mileage;

  @NotBlank
  @Column(name = "transmission", nullable = false, length = 20)
  private String transmission;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private VehicleStatus status;

  @PositiveOrZero
  @Column(name = "purchase_price", nullable = false)
  private Double purchasePrice;

  @PositiveOrZero
  @Column(name = "sale_price", nullable = false)
  private Double salePrice;

  @NotBlank
  @Column(name = "body_type", nullable = false, length = 20)
  private String bodyType;

  @NotBlank
  @Column(name = "fuel_type", nullable = false, length = 20)
  private String fuelType;

  @NotNull
  @Column(name = "number_of_doors", nullable = false)
  private Integer numberOfDoors;
}
