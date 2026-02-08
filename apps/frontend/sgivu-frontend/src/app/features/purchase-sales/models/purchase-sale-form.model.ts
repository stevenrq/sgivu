import { ContractStatus } from './contract-status.enum';
import { ContractType } from './contract-type.enum';
import { PaymentMethod } from './payment-method.enum';
import { VehicleKind } from './vehicle-kind.enum';
import { VehicleCreationPayload } from './purchase-sale.model';

/**
 * Modelo del formulario reactivo de contrato.
 * Separado de `PurchaseSale` porque los formularios necesitan `null` para campos no seleccionados,
 * mientras que el modelo de API usa tipos estrictos.
 */
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

/**
 * Modelo del formulario reactivo de vehículo dentro de un contrato de compra.
 * Incluye campos de carro y moto en una sola interfaz; `buildVehiclePayload()` filtra
 * los campos según `vehicleType` antes de enviar al backend.
 */
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

/** Crea un `ContractFormModel` con valores por defecto seguros para inicializar el formulario reactivo.
 *
 * @param contractType Tipo de contrato (compra o venta) para inicializar el formulario con el tipo correcto.
 * @returns Modelo de formulario con valores por defecto.
 */
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

/** Crea un `VehicleFormModel` con valores por defecto seguros para inicializar el formulario reactivo.
 *
 * @param vehicleType Tipo de vehículo (carro o moto) para inicializar el formulario con el tipo correcto.
 * @returns Modelo de formulario con valores por defecto.
 */
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

/**
 * Transforma el modelo de formulario al payload de API, sanitizando valores y
 * filtrando campos exclusivos de carro o moto según `vehicleType`.
 * Los campos string se trimmean y la placa se convierte a mayúsculas.
 *
 * @param form Modelo del formulario reactivo.
 * @returns Payload formateado para enviar al backend.
 */
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
