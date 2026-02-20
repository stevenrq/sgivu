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
import { PurchaseSale } from '../../models/purchase-sale.model';
import { PurchaseSaleService } from '../../services/purchase-sale.service';
import { ContractStatus } from '../../models/contract-status.enum';
import {
  getStatusLabel,
  getContractTypeLabel,
  getPaymentMethodLabel,
} from '../../models/contract-labels';
import { CopCurrencyPipe } from '../../../../shared/pipes/cop-currency.pipe';
import { UtcToGmtMinus5Pipe } from '../../../../shared/pipes/utc-to-gmt-minus5.pipe';

@Component({
  selector: 'app-purchase-sale-detail',
  imports: [CommonModule, RouterLink, CopCurrencyPipe, UtcToGmtMinus5Pipe],
  templateUrl: './purchase-sale-detail.component.html',
  styleUrls: [
    '../../../../shared/styles/entity-detail-page.css',
    './purchase-sale-detail.component.css',
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PurchaseSaleDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly purchaseSaleService = inject(PurchaseSaleService);
  private readonly destroyRef = inject(DestroyRef);

  protected contract: PurchaseSale | null = null;
  protected loading = true;
  protected error: string | null = null;

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const idParam = params.get('id');
        if (!idParam) {
          void this.router.navigate(['/purchase-sales']);
          return;
        }
        const id = Number(idParam);
        if (Number.isNaN(id)) {
          void this.router.navigate(['/purchase-sales']);
          return;
        }
        this.loadContract(id);
      });
  }

  get statusBadgeClass(): string {
    if (!this.contract) {
      return 'bg-secondary';
    }
    switch (this.contract.contractStatus) {
      case ContractStatus.ACTIVE:
        return 'bg-primary';
      case ContractStatus.COMPLETED:
        return 'bg-success';
      case ContractStatus.CANCELED:
        return 'bg-danger';
      default:
        return 'bg-warning text-dark';
    }
  }

  get typeLabel(): string {
    if (!this.contract) {
      return '';
    }
    return getContractTypeLabel(this.contract.contractType);
  }

  get statusLabel(): string {
    if (!this.contract) {
      return '';
    }
    return getStatusLabel(this.contract.contractStatus);
  }

  get paymentMethodLabel(): string {
    if (!this.contract) {
      return '';
    }
    return getPaymentMethodLabel(this.contract.paymentMethod);
  }

  protected get clientDetailLink(): (string | number)[] {
    if (!this.contract) {
      return ['/clients'];
    }
    const summary = this.contract.clientSummary;
    const type = summary?.type?.toLowerCase();
    if (type === 'company') {
      return ['/clients/companies', this.contract.clientId, 'detail'];
    }
    if (type === 'person') {
      return ['/clients/persons', this.contract.clientId, 'detail'];
    }
    return ['/clients'];
  }

  protected get vehicleDetailLink(): (string | number)[] {
    if (!this.contract?.vehicleId) {
      return ['/vehicles'];
    }
    const summary = this.contract.vehicleSummary;
    const type = summary?.type?.toLowerCase();
    if (type === 'motorcycle') {
      return ['/vehicles/motorcycles', this.contract.vehicleId, 'details'];
    }
    return ['/vehicles/cars', this.contract.vehicleId, 'details'];
  }

  private loadContract(id: number): void {
    this.loading = true;
    this.error = null;
    this.purchaseSaleService
      .getById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (contract) => {
          this.contract = contract;
          this.loading = false;
        },
        error: () => {
          this.error = 'No se pudo cargar la información del contrato.';
          this.loading = false;
        },
      });
  }

  protected get clientName(): string {
    if (!this.contract) {
      return '';
    }
    const summary = this.contract.clientSummary;
    if (summary?.name) {
      return summary.name;
    }
    return `Cliente #${this.contract.clientId}`;
  }

  protected get clientIdentifier(): string {
    if (!this.contract) {
      return '';
    }
    const summary = this.contract.clientSummary;
    if (summary?.identifier) {
      return summary.identifier;
    }
    return 'Identificación no disponible';
  }

  protected get userName(): string {
    if (!this.contract) {
      return '';
    }
    const summary = this.contract.userSummary;
    if (summary?.fullName) {
      return summary.username
        ? `${summary.fullName} (@${summary.username})`
        : summary.fullName;
    }
    return `Usuario #${this.contract.userId}`;
  }

  protected get vehicleLabel(): string {
    if (!this.contract) {
      return '';
    }
    const summary = this.contract.vehicleSummary;
    if (summary) {
      const pieces = [
        summary.brand,
        summary.model,
        summary.plate ? `(${summary.plate})` : null,
      ].filter(Boolean);
      if (pieces.length) {
        return pieces.join(' ');
      }
    }
    if (this.contract.vehicleId) {
      return `Vehículo #${this.contract.vehicleId}`;
    }
    return 'Vehículo no disponible';
  }
}
