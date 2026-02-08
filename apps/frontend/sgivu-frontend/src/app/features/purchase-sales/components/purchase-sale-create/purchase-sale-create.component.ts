import {
  Component,
  DestroyRef,
  OnInit,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule, NgForm, NgModel } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { parseUtcDate } from '../../../../shared/utils/date.utils';
import {
  formatCopCurrency,
  formatCopNumber,
  normalizeMoneyInput,
} from '../../../../shared/utils/currency.utils';
import {
  showAlert,
  showTimedSuccessAlert,
} from '../../../../shared/utils/swal-alert.utils';
import { KpiCardComponent } from '../../../../shared/components/kpi-card/kpi-card.component';
import { FormShellComponent } from '../../../../shared/components/form-shell/form-shell.component';
import { PurchaseSaleService } from '../../services/purchase-sale.service';
import { PurchaseSaleLookupService } from '../../services/purchase-sale-lookup.service';
import { PurchaseSale } from '../../models/purchase-sale.model';
import { ContractType } from '../../models/contract-type.enum';
import { ContractStatus } from '../../models/contract-status.enum';
import { PaymentMethod } from '../../models/payment-method.enum';
import { VehicleKind } from '../../models/vehicle-kind.enum';
import { VehicleStatus } from '../../../vehicles/models/vehicle-status.enum';
import {
  ContractFormModel,
  VehicleFormModel,
  createDefaultContractForm,
  createDefaultVehicleForm,
} from '../../models/purchase-sale-form.model';
import {
  getStatusLabel,
  getContractTypeLabel,
  getPaymentMethodLabel,
} from '../../models/contract-labels';
import { PurchaseVehicleFormComponent } from '../purchase-vehicle-form/purchase-vehicle-form.component';

@Component({
  selector: 'app-purchase-sale-create',
  imports: [
    FormsModule,
    HasPermissionDirective,
    KpiCardComponent,
    FormShellComponent,
    PurchaseVehicleFormComponent,
  ],
  templateUrl: './purchase-sale-create.component.html',
  styleUrl: './purchase-sale-create.component.css',
})
export class PurchaseSaleCreateComponent implements OnInit {
  private readonly purchaseSaleService = inject(PurchaseSaleService);
  private readonly lookupService = inject(PurchaseSaleLookupService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild(PurchaseVehicleFormComponent)
  private vehicleFormComp?: PurchaseVehicleFormComponent;

  readonly contractStatuses = Object.values(ContractStatus);
  readonly paymentMethods = Object.values(PaymentMethod);
  readonly contractTypes = Object.values(ContractType);
  readonly vehicleKinds = Object.values(VehicleKind);
  readonly ContractStatus = ContractStatus;
  readonly ContractType = ContractType;

  contractForm: ContractFormModel = createDefaultContractForm();
  vehicleForm: VehicleFormModel = createDefaultVehicleForm();
  formSubmitted = false;
  purchasePriceInput = '';
  salePriceInput = '';
  formLoading = false;

  readonly clients = this.lookupService.clients;
  readonly users = this.lookupService.users;
  readonly vehicles = this.lookupService.vehicles;
  readonly isLoadingLookups = signal(false);
  readonly hasLookupError = signal(false);

  readonly summaryState = signal({ total: 0, purchases: 0, sales: 0 });

  readonly availableVehicles = computed(() =>
    this.vehicles().filter((v) => v.status === VehicleStatus.AVAILABLE),
  );

  private readonly priceDecimals = 0;
  private readonly salePurchaseCache = new Map<number, number>();

  readonly getStatusLabel = getStatusLabel;
  readonly getContractTypeLabel = getContractTypeLabel;
  readonly getPaymentMethodLabel = getPaymentMethodLabel;

  get isPurchaseType(): boolean {
    return this.contractForm.contractType === ContractType.PURCHASE;
  }

  get isSaleType(): boolean {
    return this.contractForm.contractType === ContractType.SALE;
  }

  ngOnInit(): void {
    this.loadLookups();
    this.refreshSummary();

    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => this.applyQueryParams(params));
  }

  submitContract(contractFormRef: NgForm): void {
    this.formSubmitted = true;
    if (!contractFormRef.valid) {
      this.showValidationError();
      return;
    }

    const purchasePrice = this.contractForm.purchasePrice;
    const salePrice = this.isSaleType
      ? this.contractForm.salePrice
      : this.vehicleForm.salePrice;
    if (!this.isPriceInputValid(purchasePrice, salePrice)) {
      this.showPriceError();
      return;
    }

    const payload: PurchaseSale = {
      clientId: Number(this.contractForm.clientId),
      userId: Number(this.contractForm.userId),
      purchasePrice: purchasePrice ?? 0,
      salePrice: salePrice ?? 0,
      contractStatus: this.contractForm.contractStatus,
      contractType: this.contractForm.contractType,
      paymentLimitations: this.contractForm.paymentLimitations.trim(),
      paymentTerms: this.contractForm.paymentTerms.trim(),
      paymentMethod: this.contractForm.paymentMethod,
      observations: this.contractForm.observations?.trim() ?? null,
    };

    if (this.isSaleType) {
      payload.vehicleId = Number(this.contractForm.vehicleId);
    } else {
      payload.vehicleData = this.vehicleFormComp?.buildPayload();
    }

    this.formLoading = true;
    this.purchaseSaleService
      .create(payload)
      .pipe(finalize(() => (this.formLoading = false)))
      .subscribe({
        next: () => {
          this.resetContractForm(contractFormRef, true);
          this.salePurchaseCache.clear();
          this.refreshSummary();
          void showTimedSuccessAlert('Contrato registrado con éxito.').then(
            () => {
              void this.router.navigate(['/purchase-sales/page', 0]);
            },
          );
        },
        error: (error) => this.handleError(error, 'registrar el contrato'),
      });
  }

  onContractTypeChange(type: ContractType): void {
    this.contractForm.contractType = type;
    this.contractForm.salePrice = null;
    this.contractForm.vehicleId = null;
    this.contractForm.purchasePrice = null;
    this.purchasePriceInput = '';
    this.salePriceInput = '';
  }

  onVehicleSelectionChange(vehicleId: number | null): void {
    if (!this.isSaleType) return;

    if (!vehicleId) {
      this.contractForm.purchasePrice = null;
      this.purchasePriceInput = '';
      return;
    }

    if (this.salePurchaseCache.has(vehicleId)) {
      this.applyPrefilledPurchasePrice(this.salePurchaseCache.get(vehicleId));
      return;
    }

    this.purchaseSaleService
      .getByVehicleId(vehicleId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (contracts) => {
          const sale = this.findRegisteredSale(contracts);
          if (sale) {
            this.showVehicleSoldWarning(vehicleId, sale.contractStatus);
            return;
          }
          const purchase = this.findEligiblePurchase(contracts);
          if (!purchase) {
            this.showVehicleSaleRestriction(vehicleId);
            return;
          }
          this.salePurchaseCache.set(vehicleId, purchase.purchasePrice);
          this.applyPrefilledPurchasePrice(purchase.purchasePrice);
        },
        error: (error) => {
          this.handleError(error, 'validar el vehículo seleccionado');
          this.contractForm.vehicleId = null;
        },
      });
  }

  formatCurrency(value: number | null | undefined): string {
    return formatCopCurrency(value, {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    });
  }

  onPriceInput(value: string, field: 'purchasePrice' | 'salePrice'): void {
    const { numericValue, displayValue } = normalizeMoneyInput(
      value,
      this.priceDecimals,
    );

    if (field === 'purchasePrice') {
      this.purchasePriceInput = displayValue;
      this.contractForm.purchasePrice = numericValue;
    } else {
      this.salePriceInput = displayValue;
      this.contractForm.salePrice = numericValue;
    }
  }

  showControlErrors(control: NgModel | null): boolean {
    if (!control) return false;
    return (
      !!control.invalid &&
      (control.touched || control.dirty || this.formSubmitted)
    );
  }

  resetContractForm(form?: NgForm, keepSelections = false): void {
    const selectedContractType = keepSelections
      ? this.contractForm.contractType
      : ContractType.PURCHASE;
    const selectedVehicleType = keepSelections
      ? this.vehicleForm.vehicleType
      : VehicleKind.CAR;

    form?.resetForm();
    this.contractForm = createDefaultContractForm(selectedContractType);
    this.vehicleForm = createDefaultVehicleForm(selectedVehicleType);
    this.purchasePriceInput = '';
    this.salePriceInput = '';
    this.vehicleFormComp?.resetDisplayInputs();
    this.formSubmitted = false;
  }

  private loadLookups(): void {
    this.isLoadingLookups.set(true);
    this.hasLookupError.set(false);

    this.lookupService.loadAll(
      this.destroyRef,
      (err) => {
        this.hasLookupError.set(true);
        this.handleError(err, 'cargar la información auxiliar');
      },
      () => this.isLoadingLookups.set(false),
    );
  }

  private refreshSummary(): void {
    this.purchaseSaleService
      .getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (contracts) => {
          const purchases = contracts.filter(
            (c) => c.contractType === ContractType.PURCHASE,
          ).length;
          const sales = contracts.filter(
            (c) => c.contractType === ContractType.SALE,
          ).length;
          this.summaryState.set({ total: contracts.length, purchases, sales });
        },
        error: () =>
          this.summaryState.set({ total: 0, purchases: 0, sales: 0 }),
      });
  }

  private applyPrefilledPurchasePrice(value = 0): void {
    this.contractForm.purchasePrice = value;
    this.purchasePriceInput = formatCopNumber(value, {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    });
  }

  private findEligiblePurchase(contracts: PurchaseSale[]): PurchaseSale | null {
    const eligibleStatuses = new Set([
      ContractStatus.ACTIVE,
      ContractStatus.COMPLETED,
    ]);

    return (
      contracts
        .filter((c) => c.contractType === ContractType.PURCHASE)
        .filter((c) => eligibleStatuses.has(c.contractStatus))
        .sort(
          (a, b) =>
            (parseUtcDate(b.updatedAt ?? '')?.getTime() ?? 0) -
            (parseUtcDate(a.updatedAt ?? '')?.getTime() ?? 0),
        )[0] ?? null
    );
  }

  private findRegisteredSale(contracts: PurchaseSale[]): PurchaseSale | null {
    const blockingStatuses = new Set([
      ContractStatus.PENDING,
      ContractStatus.ACTIVE,
      ContractStatus.COMPLETED,
    ]);
    return (
      contracts.find(
        (c) =>
          c.contractType === ContractType.SALE &&
          blockingStatuses.has(c.contractStatus),
      ) ?? null
    );
  }

  private showVehicleSaleRestriction(vehicleId: number): void {
    const label = this.getVehicleLabelById(vehicleId);
    void showAlert({
      icon: 'warning',
      title: 'Inventario no disponible',
      text: `${label} no cuenta con una compra activa o completada registrada.`,
    });
    this.contractForm.vehicleId = null;
    this.contractForm.purchasePrice = null;
    this.purchasePriceInput = '';
  }

  private showVehicleSoldWarning(
    vehicleId: number,
    status: ContractStatus,
  ): void {
    const label = this.getVehicleLabelById(vehicleId);
    const statusLabel = getStatusLabel(status);
    void showAlert({
      icon: 'warning',
      title: 'Venta ya registrada',
      text: `${label} ya tiene un contrato de venta en estado ${statusLabel}. No puedes registrar una nueva venta para este vehículo.`,
    });
    this.contractForm.vehicleId = null;
    this.contractForm.purchasePrice = null;
    this.purchasePriceInput = '';
  }

  private showValidationError(): void {
    void showAlert({
      icon: 'warning',
      title: 'Formulario incompleto',
      text: 'Por favor, completa todos los campos obligatorios antes de continuar.',
    });
  }

  private showPriceError(): void {
    void showAlert({
      icon: 'warning',
      title: 'Precios inválidos',
      text: 'El precio de compra y el precio de venta deben ser valores en COP mayores a cero.',
    });
  }

  private handleError(
    error: unknown,
    action: string,
    displayAlert = true,
  ): void {
    const details =
      (error as { error?: { details?: string } })?.error?.details ?? null;
    const message = this.decorateVehicleMessage(
      details ??
        `Se presentó un inconveniente al ${action}. Intenta nuevamente.`,
    );

    if (displayAlert) {
      void showAlert({ icon: 'error', title: 'Oops...', text: message });
    }
  }

  private applyQueryParams(queryParams: ParamMap): void {
    const typeParam = queryParams.get('contractType');
    if (
      typeParam &&
      (Object.values(ContractType) as string[]).includes(typeParam)
    ) {
      this.onContractTypeChange(typeParam as ContractType);
    }

    const vehicleKindParam = queryParams.get('vehicleKind');
    if (
      vehicleKindParam &&
      (Object.values(VehicleKind) as string[]).includes(vehicleKindParam)
    ) {
      this.vehicleForm.vehicleType = vehicleKindParam as VehicleKind;
    }
  }

  private isPriceInputValid(
    purchasePrice: number | null | undefined,
    salePrice: number | null | undefined,
  ): boolean {
    if (!purchasePrice || purchasePrice <= 0) return false;
    if (this.isSaleType) return !!salePrice && salePrice > 0;
    if (salePrice !== null && salePrice !== undefined && salePrice < 0)
      return false;
    return true;
  }

  private getVehicleLabelById(vehicleId: number): string {
    const option = this.lookupService.vehicleMap().get(vehicleId);
    return option ? option.label : 'Este vehículo';
  }

  private decorateVehicleMessage(message: string | null): string {
    if (!message) return '';
    return message.replaceAll(/veh[ií]culo con id (\d+)/gi, (_, id: string) => {
      const label = this.getVehicleLabelById(Number(id));
      return label ?? `vehículo con id ${id}`;
    });
  }
}
