package com.sgivu.vehicle.dto;

import com.sgivu.vehicle.enums.VehicleStatus;
import lombok.Builder;
import lombok.Getter;

/**
 * Filtros para búsquedas de inventario de motocicletas.
 *
 * <p>Incluye atributos necesarios para segmentar por tipo y rango de precios en los flujos de
 * ventas y campañas de oferta.
 *
 * @apiNote Se consume por {@link com.sgivu.vehicle.specification.MotorcycleSpecifications} para
 *     generar predicados JPA seguros.
 */
@Getter
@Builder
public class MotorcycleSearchCriteria {
  private final String plate;
  private final String brand;
  private final String line;
  private final String model;
  private final String motorcycleType;
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
