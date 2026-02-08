import { ContractStatus } from './contract-status.enum';
import { ContractType } from './contract-type.enum';
import { PaymentMethod } from './payment-method.enum';
import { VehicleKind } from './vehicle-kind.enum';
import { VehicleCreationPayload } from './purchase-sale.model';

// ── Interfaces ───────────────────────────────────────────────────

export interface ContractFormModel {
  clientId: number | null;
  userId: number | null;
  vehicleId: number | null;
  contractType: ContractType;
  contractStatus: ContractStatus;
  purchasePrice: number | null;
  salePrice: number | null;
  paymentLimitations: string;
  paymentTerms: string;
  paymentMethod: PaymentMethod;
  observations: string;
}

export interface VehicleFormModel {
  vehicleType: VehicleKind;
  brand: string;
  model: string;
  capacity: number | null;
  line: string;
  plate: string;
  motorNumber: string;
  serialNumber: string;
  chassisNumber: string;
  color: string;
  cityRegistered: string;
  year: number | null;
  mileage: number | null;
  transmission: string;
  salePrice: number | null;
  photoUrl: string;
  bodyType: string;
  fuelType: string;
  numberOfDoors: number | null;
  motorcycleType: string;
}

// ── Factory Functions ────────────────────────────────────────────

export function createDefaultContractForm(
  contractType: ContractType = ContractType.PURCHASE,
): ContractFormModel {
  return {
    clientId: null,
    userId: null,
    vehicleId: null,
    contractType,
    contractStatus: ContractStatus.PENDING,
    purchasePrice: null,
    salePrice: null,
    paymentLimitations: '',
    paymentTerms: '',
    paymentMethod: PaymentMethod.BANK_TRANSFER,
    observations: '',
  };
}

export function createDefaultVehicleForm(
  vehicleType: VehicleKind = VehicleKind.CAR,
): VehicleFormModel {
  return {
    vehicleType,
    brand: '',
    model: '',
    capacity: null,
    line: '',
    plate: '',
    motorNumber: '',
    serialNumber: '',
    chassisNumber: '',
    color: '',
    cityRegistered: '',
    year: null,
    mileage: null,
    transmission: '',
    salePrice: null,
    photoUrl: '',
    bodyType: '',
    fuelType: '',
    numberOfDoors: null,
    motorcycleType: '',
  };
}

// ── Payload Builder ──────────────────────────────────────────────

export function buildVehiclePayload(
  form: VehicleFormModel,
): VehicleCreationPayload {
  const trim = (value: string | null | undefined): string =>
    value?.trim() ?? '';
  const isCar = form.vehicleType === VehicleKind.CAR;
  const isMoto = form.vehicleType === VehicleKind.MOTORCYCLE;

  const salePrice =
    form.salePrice !== null && form.salePrice !== undefined
      ? Number(form.salePrice)
      : undefined;
  const numberOfDoors =
    isCar && form.numberOfDoors !== null
      ? Number(form.numberOfDoors)
      : undefined;

  return {
    vehicleType: form.vehicleType,
    brand: trim(form.brand),
    model: trim(form.model),
    capacity: Number(form.capacity ?? 0),
    line: trim(form.line),
    plate: trim(form.plate).toUpperCase(),
    motorNumber: trim(form.motorNumber),
    serialNumber: trim(form.serialNumber),
    chassisNumber: trim(form.chassisNumber),
    color: trim(form.color),
    cityRegistered: trim(form.cityRegistered),
    year: Number(form.year ?? 0),
    mileage: Number(form.mileage ?? 0),
    transmission: trim(form.transmission),
    salePrice,
    photoUrl: trim(form.photoUrl) || undefined,
    bodyType: isCar ? trim(form.bodyType) : undefined,
    fuelType: isCar ? trim(form.fuelType) : undefined,
    numberOfDoors,
    motorcycleType: isMoto ? trim(form.motorcycleType) : undefined,
  };
}
