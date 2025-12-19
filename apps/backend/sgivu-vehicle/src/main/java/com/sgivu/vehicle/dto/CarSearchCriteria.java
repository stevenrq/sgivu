package com.sgivu.vehicle.dto;

import com.sgivu.vehicle.enums.VehicleStatus;
import lombok.Builder;
import lombok.Getter;

/**
 * Filtros dinámicos para consultas de autos.
 *
 * <p>Permite al front y a otros microservicios filtrar inventario por atributos clave usados en
 * cotizaciones y predicción de demanda (rango de precios, año, combustible, etc.).
 *
 * @apiNote Se usa junto a {@link com.sgivu.vehicle.specification.CarSpecifications} para construir
 *     consultas JPA sin concatenar SQL manual.
 */
@Getter
@Builder
public class CarSearchCriteria {
  private final String plate;
  private final String brand;
  private final String line;
  private final String model;
  private final String fuelType;
  private final String bodyType;
  private final String transmission;
  private final String cityRegistered;
  private final VehicleStatus status;
  private final Integer minYear;
  private final Integer maxYear;
  private final Integer minCapacity;
  private final Integer maxCapacity;
  private final Integer minMileage;
  private final Integer maxMileage;
  private final Double minSalePrice;
  private final Double maxSalePrice;
}
