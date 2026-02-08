import { ContractStatus } from '../models/contract-status.enum';
import { ContractType } from '../models/contract-type.enum';
import { PaymentMethod } from '../models/payment-method.enum';
import { PurchaseSale } from '../models/purchase-sale.model';

/**
 * Mapas de labels en español para enums del backend.
 * Centralizados aquí para evitar duplicación entre componentes de lista, detalle y filtros.
 */
const STATUS_LABELS: Record<ContractStatus, string> = {
  [ContractStatus.PENDING]: 'Pendiente',
  [ContractStatus.ACTIVE]: 'Activo',
  [ContractStatus.COMPLETED]: 'Completado',
  [ContractStatus.CANCELED]: 'Cancelado',
};

const TYPE_LABELS: Record<ContractType, string> = {
  [ContractType.PURCHASE]: 'Compra',
  [ContractType.SALE]: 'Venta',
};

const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
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

export function getStatusLabel(status: ContractStatus): string {
  return STATUS_LABELS[status] ?? status;
}

export function getContractTypeLabel(type: ContractType): string {
  return TYPE_LABELS[type] ?? type;
}

export function getPaymentMethodLabel(method: PaymentMethod): string {
  return PAYMENT_METHOD_LABELS[method] ?? method;
}

export function getStatusBadgeClass(status: ContractStatus): string {
  switch (status) {
    case ContractStatus.ACTIVE:
      return 'bg-primary-subtle text-primary-emphasis';
    case ContractStatus.COMPLETED:
      return 'bg-success-subtle text-success-emphasis';
    case ContractStatus.CANCELED:
      return 'bg-danger-subtle text-danger-emphasis';
    default:
      return 'bg-warning-subtle text-warning-emphasis';
  }
}

export function getVehicleBadgeClass(contract: PurchaseSale): string {
  return contract.contractType === ContractType.PURCHASE
    ? 'bg-primary-subtle text-primary-emphasis'
    : 'bg-success-subtle text-success-emphasis';
}
