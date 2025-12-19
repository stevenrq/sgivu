package com.sgivu.purchasesale.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Resumen compacto de un vehículo usado. Se construye a partir de respuestas del microservicio de
 * inventario y se utiliza para enriquecer contratos sin exponer la estructura completa del recurso
 * remoto.
 */
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
