import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { VehicleKind } from '../../models/vehicle-kind.enum';
import {
  VehicleFormControls,
  applyVehicleTypeValidators,
  buildVehiclePayload,
} from '../../models/purchase-sale-form.model';
import { VehicleCreationPayload } from '../../models/purchase-sale.model';
import {
  formatCopCurrency,
  normalizeMoneyInput,
  parseCopCurrency,
} from '../../../../shared/utils/currency.utils';

@Component({
  selector: 'app-purchase-vehicle-form',
  imports: [ReactiveFormsModule],
  templateUrl: './purchase-vehicle-form.component.html',
  styleUrl: './purchase-vehicle-form.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PurchaseVehicleFormComponent {
  readonly vehicleFormGroup = input.required<FormGroup<VehicleFormControls>>();
  readonly vehicleKinds = Object.values(VehicleKind);

  vehicleSalePriceInput = '';
  vehicleMileageInput = '';

  private readonly mileageFormatter = new Intl.NumberFormat('es-CO', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  });

  get isCarSelected(): boolean {
    return (
      this.vehicleFormGroup().controls.vehicleType.value === VehicleKind.CAR
    );
  }

  get isMotorcycleSelected(): boolean {
    return (
      this.vehicleFormGroup().controls.vehicleType.value ===
      VehicleKind.MOTORCYCLE
    );
  }

  onVehicleTypeChange(): void {
    const kind = this.vehicleFormGroup().controls.vehicleType.value;
    applyVehicleTypeValidators(this.vehicleFormGroup(), kind);
    this.vehicleSalePriceInput = '';
  }

  onPriceInput(value: string): void {
    const { numericValue, displayValue } = normalizeMoneyInput(value, 0);
    this.vehicleSalePriceInput = displayValue;
    this.vehicleFormGroup().controls.salePrice.setValue(numericValue);
  }

  onMileageInput(value: string): void {
    const numericValue = parseCopCurrency(value);
    if (numericValue === null) {
      this.vehicleFormGroup().controls.mileage.setValue(null);
      this.vehicleMileageInput = '';
      return;
    }
    const sanitized = Math.max(0, Math.floor(numericValue));
    this.vehicleFormGroup().controls.mileage.setValue(sanitized);
    this.vehicleMileageInput = this.formatMileage(sanitized);
  }

  formatCurrency(value: number | null | undefined): string {
    return formatCopCurrency(value, {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    });
  }

  formatMileage(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '';
    }
    return this.mileageFormatter.format(value);
  }

  buildPayload(): VehicleCreationPayload {
    return buildVehiclePayload(this.vehicleFormGroup());
  }

  resetDisplayInputs(): void {
    this.vehicleSalePriceInput = '';
    this.vehicleMileageInput = '';
  }
}
