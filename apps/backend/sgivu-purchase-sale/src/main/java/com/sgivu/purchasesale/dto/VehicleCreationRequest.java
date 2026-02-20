package com.sgivu.purchasesale.dto;

import com.sgivu.purchasesale.enums.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Schema(
    description =
        "Datos mínimos para registrar un vehículo en el microservicio de inventario cuando el"
            + " contrato es de compra")
@Getter
@Setter
@ToString
public class VehicleCreationRequest {

  @NotNull(message = "El tipo de vehículo es obligatorio para registrar la compra.")
  private VehicleType vehicleType;

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
  private String photoUrl;

  // Campos específicos para automóviles
  private String bodyType;
  private String fuelType;
  private Integer numberOfDoors;

  // Campos específicos para motocicletas
  private String motorcycleType;
}
