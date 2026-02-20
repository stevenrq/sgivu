package com.sgivu.purchasesale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(
    description =
        "Modelo base de vehículo usado con atributos comunes para integración con inventario")
@Data
@NoArgsConstructor
public class Vehicle {
  private Long id;
  private String brand;
  private String model;
  private Integer capacity;
  private String line;
  private String plate;
  private String motorNumber;
  private String serialNumber;
  private String chassisNumber;
  private String color;
  private String cityRegistered;
  private Integer year;
  private Integer mileage;
  private String transmission;
  private Double purchasePrice;
  private Double salePrice;
  private String status;
  private String photoUrl;
}
