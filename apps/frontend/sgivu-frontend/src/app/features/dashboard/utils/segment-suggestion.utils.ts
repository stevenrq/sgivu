import { PurchaseSale } from '../../purchase-sales/models/purchase-sale.model';
import { VehicleKind } from '../../purchase-sales/models/vehicle-kind.enum';
import { normalizeVehicleType } from './vehicle-kind.utils';

export interface SegmentOption {
  vehicleType: VehicleKind;
  brand: string;
  model: string;
  line?: string | null;
  occurrences: number;
}

export function buildSegmentSuggestions(
  contracts: PurchaseSale[],
): SegmentOption[] {
  const counter = new Map<string, SegmentOption>();

  contracts.forEach((contract) => {
    const summary = contract.vehicleSummary;
    const vehicleType = normalizeVehicleType(
      summary?.type ?? contract.vehicleData?.vehicleType,
    );
    const brand = summary?.brand ?? contract.vehicleData?.brand;
    const model = summary?.model ?? contract.vehicleData?.model;
    const line = contract.vehicleData?.line;

    if (!vehicleType || !brand || !model) {
      return;
    }

    const key = `${vehicleType}|${brand.toUpperCase()}|${model.toUpperCase()}|${
      line?.toUpperCase() ?? ''
    }`;
    const existing = counter.get(key);
    if (existing) {
      existing.occurrences += 1;
      return;
    }
    counter.set(key, {
      vehicleType,
      brand,
      model,
      line: line ?? null,
      occurrences: 1,
    });
  });

  return Array.from(counter.values())
    .sort((a, b) => b.occurrences - a.occurrences)
    .slice(0, 6);
}
