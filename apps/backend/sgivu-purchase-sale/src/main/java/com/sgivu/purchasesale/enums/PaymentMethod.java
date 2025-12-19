package com.sgivu.purchasesale.enums;

/**
 * Enum que define los diferentes métodos de pago disponibles en el sistema SGIVU para las
 * operaciones de compra y venta de vehículos usados.
 *
 * <p>Cada valor representa una forma de pago válida dentro del contexto colombiano, incluyendo
 * opciones tradicionales (efectivo, transferencia) y digitales (Nequi, Daviplata).
 */
public enum PaymentMethod {

  /** Pago realizado en efectivo. Limitado legalmente por montos máximos en Colombia. */
  CASH,

  /** Transferencia bancaria directa entre cuentas (ACH, PSE, app bancaria). */
  BANK_TRANSFER,

  /** Consignación bancaria efectuada en ventanilla o cajero. */
  BANK_DEPOSIT,

  /** Pago mediante cheque de gerencia emitido por una entidad bancaria. */
  CASHIERS_CHECK,

  /** Combinación de varios métodos de pago (efectivo + transferencia, etc.). */
  MIXED,

  /** Pago realizado a través de financiación o crédito con una entidad financiera. */
  FINANCING,

  /** Pago mediante billeteras digitales como Nequi, Daviplata, Powwi o Movii. */
  DIGITAL_WALLET,

  /** Entrega de otro vehículo o activo como parte del pago (permuta). */
  TRADE_IN,

  /** Pago diferido en cuotas o plazos acordados entre las partes. */
  INSTALLMENT_PAYMENT
}
