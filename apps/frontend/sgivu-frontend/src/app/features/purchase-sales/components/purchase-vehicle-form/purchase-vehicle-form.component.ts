import { Component, Input } from '@angular/core';
import { FormsModule, NgModel } from '@angular/forms';
import { VehicleKind } from '../../models/vehicle-kind.enum';
import {
  VehicleFormModel,
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
  imports: [FormsModule],
  templateUrl: './purchase-vehicle-form.component.html',
  styleUrl: './purchase-vehicle-form.component.css',
})
export class PurchaseVehicleFormComponent {
  @Input({ required: true }) vehicleForm!: VehicleFormModel;
  @Input() formSubmitted = false;
  @Input() vehicleKinds: VehicleKind[] = [];

  vehicleSalePriceInput = '';
  vehicleMileageInput = '';

  private readonly mileageFormatter = new Intl.NumberFormat('es-CO', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  });

  get isCarSelected(): boolean {
    return this.vehicleForm.vehicleType === VehicleKind.CAR;
  }

  get isMotorcycleSelected(): boolean {
    return this.vehicleForm.vehicleType === VehicleKind.MOTORCYCLE;
  }

  onVehicleTypeChange(kind: VehicleKind): void {
    this.vehicleForm.vehicleType = kind;
    if (kind === VehicleKind.CAR) {
      this.vehicleForm.motorcycleType = '';
      this.vehicleSalePriceInput = '';
      return;
    }

    this.vehicleForm.bodyType = '';
    this.vehicleForm.fuelType = '';
    this.vehicleForm.numberOfDoors = null;
    this.vehicleSalePriceInput = '';
  }

  onPriceInput(value: string): void {
    const { numericValue, displayValue } = normalizeMoneyInput(value, 0);
    this.vehicleSalePriceInput = displayValue;
    this.vehicleForm.salePrice = numericValue;
  }

  onMileageInput(value: string): void {
    const numericValue = parseCopCurrency(value);
    if (numericValue === null) {
      this.vehicleForm.mileage = null;
      this.vehicleMileageInput = '';
      return;
    }
    const sanitized = Math.max(0, Math.floor(numericValue));
    this.vehicleForm.mileage = sanitized;
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

  showControlErrors(control: NgModel | null): boolean {
    if (!control) {
      return false;
    }
    return (
      !!control.invalid &&
      (control.touched || control.dirty || this.formSubmitted)
    );
  }

  buildPayload(): VehicleCreationPayload {
    return buildVehiclePayload(this.vehicleForm);
  }

  resetDisplayInputs(): void {
    this.vehicleSalePriceInput = '';
    this.vehicleMileageInput = '';
  }
}
