import { ContractStatus } from './contract-status.enum';
import { ContractType } from './contract-type.enum';
import { PaymentMethod } from './payment-method.enum';

// ── Mapas de etiquetas ───────────────────────────────────────────

export const STATUS_LABELS: Record<ContractStatus, string> = {
  [ContractStatus.PENDING]: 'Pendiente',
  [ContractStatus.ACTIVE]: 'Activo',
  [ContractStatus.COMPLETED]: 'Completado',
  [ContractStatus.CANCELED]: 'Cancelado',
};

export const TYPE_LABELS: Record<ContractType, string> = {
  [ContractType.PURCHASE]: 'Compra',
  [ContractType.SALE]: 'Venta',
};

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  [PaymentMethod.CASH]: 'Efectivo',
  [PaymentMethod.BANK_TRANSFER]: 'Transferencia bancaria',
  [PaymentMethod.BANK_DEPOSIT]: 'Consignación bancaria',
  [PaymentMethod.CASHIERS_CHECK]: 'Cheque de gerencia',
  [PaymentMethod.MIXED]: 'Pago combinado',
  [PaymentMethod.FINANCING]: 'Financiación',
  [PaymentMethod.DIGITAL_WALLET]: 'Billetera digital',
  [PaymentMethod.TRADE_IN]: 'Permuta',
  [PaymentMethod.INSTALLMENT_PAYMENT]: 'Pago a plazos',
};

// ── Funciones de etiqueta ────────────────────────────────────────

export function getStatusLabel(status: ContractStatus): string {
  return STATUS_LABELS[status] ?? status;
}

export function getContractTypeLabel(type: ContractType): string {
  return TYPE_LABELS[type] ?? type;
}

export function getPaymentMethodLabel(method: PaymentMethod): string {
  return PAYMENT_METHOD_LABELS[method] ?? method;
}
