import { CommonModule } from '@angular/common';
import {
  Component,
  DestroyRef,
  OnInit,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { Observable, combineLatest, finalize } from 'rxjs';
import Swal from 'sweetalert2';
import { Car } from '../../models/car.model';
import { Motorcycle } from '../../models/motorcycle.model';
import { VehicleStatus } from '../../models/vehicle-status.enum';
import { CarService } from '../../services/car.service';
import { MotorcycleService } from '../../services/motorcycle.service';
import { VehicleImageService } from '../../services/vehicle-image.service';
import { VehicleImageResponse } from '../../models/vehicle-image-response';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { CopCurrencyPipe } from '../../../../shared/pipes/cop-currency.pipe';
import { VehicleUiHelperService } from '../../../../shared/services/vehicle-ui-helper.service';
import { UtcToGmtMinus5Pipe } from '../../../../shared/pipes/utc-to-gmt-minus5.pipe';
import { PurchaseSaleService } from '../../../purchase-sales/services/purchase-sale.service';
import { PurchaseSale } from '../../../purchase-sales/models/purchase-sale.model';
import { ContractType } from '../../../purchase-sales/models/contract-type.enum';
import { ContractStatus } from '../../../purchase-sales/models/contract-status.enum';
import {
  getStatusLabel as getContractStatusLabel,
  getContractTypeLabel,
} from '../../../purchase-sales/models/contract-labels';
import {
  getVehicleStatusLabel,
  getVehicleStatusBadgeClass,
} from '../../../../shared/utils/vehicle-status-labels.utils';

type VehicleDetailType = 'car' | 'motorcycle';

@Component({
  selector: 'app-vehicle-detail',
  templateUrl: './vehicle-detail.component.html',
  styleUrls: [
    '../../../../shared/styles/entity-detail-page.css',
    './vehicle-detail.component.css',
  ],
  imports: [
    CommonModule,
    RouterLink,
    HasPermissionDirective,
    CopCurrencyPipe,
    UtcToGmtMinus5Pipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VehicleDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly carService = inject(CarService);
  private readonly motorcycleService = inject(MotorcycleService);
  private readonly vehicleImageService = inject(VehicleImageService);
  private readonly vehicleUiHelper = inject(VehicleUiHelperService);
  private readonly purchaseSaleService = inject(PurchaseSaleService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly VehicleStatus = VehicleStatus;
  protected vehicle: Car | Motorcycle | null = null;

  protected vehicleImages: VehicleImageResponse[] = [];
  protected isLoading = true;
  protected errorMessage: string | null = null;
  protected vehicleType: VehicleDetailType = 'car';
  protected currentCar: Car | null = null;
  protected currentMotorcycle: Motorcycle | null = null;
  protected purchaseHistory: PurchaseSale[] = [];
  protected historyLoading = false;
  protected historyError: string | null = null;

  private currentVehicleId: number | null = null;

  ngOnInit(): void {
    combineLatest([this.route.paramMap, this.route.data])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(([params, data]) => {
        this.vehicleType = this.normalizeType(data['vehicleType']);
        const idParam = params.get('id');
        if (!idParam) {
          return;
        }
        const id = Number(idParam);
        if (Number.isNaN(id)) {
          void Swal.fire({
            icon: 'error',
            title: 'Identificador inválido',
            text: 'El identificador proporcionado no es válido.',
          });
          void this.router.navigate(['/vehicles']);
          return;
        }
        this.loadVehicle(id);
      });
  }

  get isCar(): boolean {
    return this.vehicleType === 'car';
  }

  get isMotorcycle(): boolean {
    return this.vehicleType === 'motorcycle';
  }

  get typeLabel(): string {
    return this.isCar ? 'Automóvil' : 'Motocicleta';
  }

  get heroImageUrl(): string {
    const preferred =
      this.vehicleImages.find((image) => image.primary)?.url ??
      this.vehicleImages[0]?.url;
    if (preferred) {
      return preferred;
    }
    const label = encodeURIComponent(this.isCar ? 'AUTO' : 'MOTO');
    return `https://placehold.co/160x120/EFEFEF/333333?text=${label}`;
  }

  get inventoryLink(): (string | number)[] {
    return this.isCar
      ? ['/vehicles/cars/page', 0]
      : ['/vehicles/motorcycles/page', 0];
  }

  get editLink(): (string | number)[] {
    if (!this.vehicle) {
      return this.inventoryLink;
    }
    return this.isCar
      ? ['/vehicles/cars', this.vehicle.id]
      : ['/vehicles/motorcycles', this.vehicle.id];
  }

  get updatePermission(): string {
    return this.isCar ? 'car:update' : 'motorcycle:update';
  }

  statusLabel(status: VehicleStatus): string {
    return getVehicleStatusLabel(status);
  }

  statusBadgeClass(status: VehicleStatus): string {
    return getVehicleStatusBadgeClass(status);
  }

  toggleAvailability(): void {
    if (!this.vehicle) {
      return;
    }
    const nextStatus =
      this.vehicle.status === VehicleStatus.INACTIVE
        ? VehicleStatus.AVAILABLE
        : VehicleStatus.INACTIVE;
    const callback = () => this.loadVehicle(this.vehicle!.id);
    if (this.isCar) {
      this.vehicleUiHelper.updateCarStatus(
        this.vehicle.id,
        nextStatus,
        callback,
        this.vehicle.plate,
      );
    } else {
      this.vehicleUiHelper.updateMotorcycleStatus(
        this.vehicle.id,
        nextStatus,
        callback,
        this.vehicle.plate,
      );
    }
  }

  reload(): void {
    if (this.currentVehicleId === null) {
      return;
    }
    this.loadVehicle(this.currentVehicleId);
  }

  private loadVehicle(id: number): void {
    this.currentVehicleId = id;
    this.isLoading = true;
    this.errorMessage = null;
    const request$: Observable<Car | Motorcycle> =
      this.vehicleType === 'car'
        ? this.carService.getById(id)
        : this.motorcycleService.getById(id);
    request$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isLoading = false;
        }),
      )
      .subscribe({
        next: (vehicle) => {
          this.vehicle = vehicle;
          if (this.isCar) {
            this.currentCar = vehicle as Car;
            this.currentMotorcycle = null;
          } else {
            this.currentMotorcycle = vehicle as Motorcycle;
            this.currentCar = null;
          }
          this.loadImages(vehicle.id);
          this.loadHistory(vehicle.id);
        },
        error: () => {
          this.vehicle = null;
          this.currentCar = null;
          this.currentMotorcycle = null;
          this.vehicleImages = [];
          this.errorMessage =
            'No se pudo cargar la información del vehículo. Intenta nuevamente.';
        },
      });
  }

  protected contractTypeLabel(type: ContractType): string {
    return getContractTypeLabel(type);
  }

  protected contractStatusLabel(status: ContractStatus): string {
    return getContractStatusLabel(status);
  }

  protected contractStatusClass(status: ContractStatus): string {
    switch (status) {
      case ContractStatus.ACTIVE:
        return 'bg-info text-dark';
      case ContractStatus.COMPLETED:
        return 'bg-success';
      case ContractStatus.CANCELED:
        return 'bg-danger';
      case ContractStatus.PENDING:
      default:
        return 'bg-warning text-dark';
    }
  }

  protected historyAmount(contract: PurchaseSale): number | undefined {
    if (contract.contractType === ContractType.PURCHASE) {
      return contract.purchasePrice;
    }
    if (contract.contractType === ContractType.SALE) {
      return contract.salePrice;
    }
    return undefined;
  }

  protected historyAmountLabel(contract: PurchaseSale): string {
    return contract.contractType === ContractType.PURCHASE
      ? 'Valor de compra'
      : 'Valor de venta';
  }

  protected getHistoryClientLabel(contract: PurchaseSale): string {
    const summary = contract.clientSummary;
    if (summary) {
      const pieces = [
        summary.name ?? `Cliente #${summary.id ?? contract.clientId ?? ''}`,
      ];
      if (summary.identifier) {
        pieces.push(summary.identifier);
      }
      return pieces.join(' - ');
    }
    if (contract.clientId) {
      return `Cliente #${contract.clientId}`;
    }
    return 'Cliente no disponible';
  }

  protected getHistoryUserLabel(contract: PurchaseSale): string {
    const summary = contract.userSummary;
    if (summary) {
      const username = summary.username ? `@${summary.username}` : null;
      return [summary.fullName ?? `Usuario #${summary.id ?? ''}`, username]
        .filter(Boolean)
        .join(' ');
    }
    if (contract.userId) {
      return `Usuario #${contract.userId}`;
    }
    return 'Usuario no disponible';
  }

  protected getHistoryTimestamp(contract: PurchaseSale): string | null {
    if (contract.contractType === ContractType.PURCHASE) {
      return (
        this.vehicle?.createdAt ??
        contract.createdAt ??
        contract.updatedAt ??
        null
      );
    }
    if (contract.contractType === ContractType.SALE) {
      return (
        contract.createdAt ??
        this.vehicle?.updatedAt ??
        contract.updatedAt ??
        null
      );
    }
    return contract.updatedAt ?? contract.createdAt ?? null;
  }

  private loadImages(id: number): void {
    this.vehicleImageService
      .getImages(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (images) => {
          this.vehicleImages = images;
        },
        error: () => {
          this.vehicleImages = [];
        },
      });
  }

  private loadHistory(vehicleId: number): void {
    this.historyLoading = true;
    this.historyError = null;
    this.purchaseSaleService
      .getByVehicleId(vehicleId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.historyLoading = false;
        }),
      )
      .subscribe({
        next: (contracts) => {
          const sortValue = (contract: PurchaseSale): number => {
            const timestamp = this.getHistoryTimestamp(contract);
            return timestamp ? new Date(timestamp).getTime() : 0;
          };
          this.purchaseHistory = [...contracts].sort(
            (a, b) => sortValue(b) - sortValue(a),
          );
        },
        error: () => {
          this.purchaseHistory = [];
          this.historyError =
            'No se pudo cargar el historial de transacciones del vehículo.';
        },
      });
  }

  private normalizeType(value: string | undefined): VehicleDetailType {
    return value === 'motorcycle' ? 'motorcycle' : 'car';
  }
}
