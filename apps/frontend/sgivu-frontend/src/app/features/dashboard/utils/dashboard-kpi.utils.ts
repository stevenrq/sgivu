import { ContractType } from '../../purchase-sales/models/contract-type.enum';
import { PurchaseSale } from '../../purchase-sales/models/purchase-sale.model';
import { formatCopCurrency } from '../../../shared/utils/currency.utils';

export interface SalesMetrics {
  salesHistoryCount: number;
  monthlyRevenue: number;
  monthlySales: number;
}

export function computeSalesMetrics(contracts: PurchaseSale[]): SalesMetrics {
  const salesHistoryCount = contracts.filter(
    (contract) => contract.contractType === ContractType.SALE,
  ).length;

  const now = new Date();
  const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
  const endOfMonth = new Date(
    now.getFullYear(),
    now.getMonth() + 1,
    0,
    23,
    59,
    59,
    999,
  );

  const salesThisMonth = contracts.filter((contract) => {
    if (contract.contractType !== ContractType.SALE) {
      return false;
    }
    const timestamp = contract.updatedAt ?? contract.createdAt;
    if (!timestamp) {
      return false;
    }
    const contractDate = new Date(timestamp);
    return contractDate >= startOfMonth && contractDate <= endOfMonth;
  });

  const monthlyRevenue = salesThisMonth.reduce(
    (acc, contract) => acc + (contract.salePrice ?? 0),
    0,
  );

  return {
    salesHistoryCount,
    monthlyRevenue,
    monthlySales: salesThisMonth.length,
  };
}

export function formatDashboardCurrency(value: number): string {
  return formatCopCurrency(value, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  });
}
