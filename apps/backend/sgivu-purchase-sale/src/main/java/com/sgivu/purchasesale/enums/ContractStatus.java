package com.sgivu.purchasesale.enums;

/**
 * Enum que define los diferentes estados que puede tener un contrato de compra-venta en el sistema
 * SGIVU.
 *
 * <p>Los estados reflejan el ciclo de vida típico de un contrato, desde su activación hasta su
 * finalización o cancelación.
 */
public enum ContractStatus {
  /** Contrato pendiente de confirmación por las partes o por validaciones internas. */
  PENDING,

  /** Contrato confirmado y en ejecución. */
  ACTIVE,

  /** Contrato completado exitosamente con obligaciones cumplidas. */
  COMPLETED,

  /** Contrato anulado antes de finalizarse. */
  CANCELED
}
