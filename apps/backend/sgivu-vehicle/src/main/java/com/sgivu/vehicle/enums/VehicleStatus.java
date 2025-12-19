package com.sgivu.vehicle.enums;

/**
 * Estados de ciclo de vida para vehículos usados.
 *
 * <p>Controla visibilidad en inventario, elegibilidad para contratos/ventas y bloqueos para
 * operaciones de mantenimiento o uso interno.
 */
public enum VehicleStatus {
  AVAILABLE,
  SOLD,
  IN_MAINTENANCE,
  IN_REPAIR,
  IN_USE,
  INACTIVE
}
