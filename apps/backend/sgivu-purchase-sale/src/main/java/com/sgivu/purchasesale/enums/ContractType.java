package com.sgivu.purchasesale.enums;

/**
 * Naturaleza del contrato dentro del inventario de vehículos usados. Determina si el microservicio
 * debe registrar un nuevo vehículo (compra) o consumir stock existente (venta).
 */
public enum ContractType {
  PURCHASE,
  SALE
}
