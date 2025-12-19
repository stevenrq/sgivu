package com.sgivu.vehicle.dto;

import com.sgivu.vehicle.enums.VehicleStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO base expuesto por la API para representar vehículos.
 *
 * <p>Se utiliza como contrato con front y otros microservicios (cotizaciones, contratos) evitando
 * exponer entidades JPA y controlando exactamente los datos visibles del inventario.
 */
@Data
@NoArgsConstructor
public class VehicleResponse {
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
  private VehicleStatus status;
  private Double purchasePrice;
  private Double salePrice;
}
