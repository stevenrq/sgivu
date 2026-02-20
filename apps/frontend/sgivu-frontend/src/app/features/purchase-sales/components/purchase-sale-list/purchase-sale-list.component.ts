import { CommonModule } from '@angular/common';
import {
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Params, Router, RouterLink } from '@angular/router';
import { combineLatest, finalize, tap } from 'rxjs';
import Swal from 'sweetalert2';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { showHttpError } from '../../../../shared/utils/error-handler.utils';
import { PagerComponent } from '../../../../shared/components/pager/pager.component';
import { UtcToGmtMinus5Pipe } from '../../../../shared/pipes/utc-to-gmt-minus5.pipe';
import {
  PurchaseSaleSearchFilters,
  PurchaseSaleService,
} from '../../services/purchase-sale.service';
import { PurchaseSale } from '../../models/purchase-sale.model';
import { ContractType } from '../../models/contract-type.enum';
import { ContractStatus } from '../../models/contract-status.enum';
import { PaymentMethod } from '../../models/payment-method.enum';
import { PaginatedResponse } from '../../../../shared/models/paginated-response';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { KpiCardComponent } from '../../../../shared/components/kpi-card/kpi-card.component';
import { DataTableComponent } from '../../../../shared/components/data-table/data-table.component';
import { CopCurrencyPipe } from '../../../../shared/pipes/cop-currency.pipe';
import { RowNavigateDirective } from '../../../../shared/directives/row-navigate.directive';
import { VehicleOption } from '../../models/purchase-sale-reference.model';
import { PurchaseSaleLookupService } from '../../services/purchase-sale-lookup.service';
import {
  PurchaseSaleReportService,
  ExportFormat,
} from '../../services/purchase-sale-report.service';
import {
  getStatusLabel,
  getContractTypeLabel,
  getPaymentMethodLabel,
  getStatusBadgeClass,
  getVehicleBadgeClass,
} from '../../models/contract-labels';
import {
  PurchaseSaleUiFilters,
  PriceFilterKey,
  ContractStatusFilter,
  ContractTypeFilter,
  getDefaultUiFilters,
  normalizePriceInput,
  buildQueryParamsFromFilters,
  extractFiltersFromQuery,
  paramMapToObject,
} from '../../utils/purchase-sale-filter.utils';
import {
  QuickSuggestion,
  buildQuickSuggestions,
  hintQuickSearchFilters,
} from '../../utils/quick-search.utils';

interface PurchaseSaleListState {
  items: PurchaseSale[];
  pager?: PaginatedResponse<PurchaseSale>;
  loading: boolean;
  error: string | null;
}

@Component({
  selector: 'app-purchase-sale-list',
  imports: [
    CommonModule,
    FormsModule,
    HasPermissionDirective,
    PagerComponent,
    UtcToGmtMinus5Pipe,
    PageHeaderComponent,
    KpiCardComponent,
    DataTableComponent,
    CopCurrencyPipe,
    RouterLink,
    RowNavigateDirective,
  ],
  templateUrl: './purchase-sale-list.component.html',
  styleUrl: './purchase-sale-list.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PurchaseSaleListComponent implements OnInit {
  readonly contractStatuses = Object.values(ContractStatus);
  readonly contractTypes = Object.values(ContractType);
  readonly ContractStatus = ContractStatus;
  readonly ContractType = ContractType;
  readonly paymentMethods = Object.values(PaymentMethod);
  readonly lookupService = inject(PurchaseSaleLookupService);
  readonly reportService = inject(PurchaseSaleReportService);

  readonly summaryState = signal({ total: 0, purchases: 0, sales: 0 });
  filters: PurchaseSaleUiFilters = getDefaultUiFilters();
  reportStartDate: string | null = null;
  reportEndDate: string | null = null;
  readonly listState = signal<PurchaseSaleListState>({
    items: [],
    loading: false,
    error: null,
  });
  quickSuggestions: QuickSuggestion[] = [];
  pagerQueryParams: Params | null = null;
  readonly pagerUrl = '/purchase-sales/page';

  private readonly purchaseSaleService = inject(PurchaseSaleService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly linkedClientIds = new Set<number>();
  private readonly linkedUserIds = new Set<number>();
  private readonly linkedVehicleIds = new Set<number>();
  private currentPage = 0;
  private activeSearchFilters: PurchaseSaleSearchFilters | null = null;

  ngOnInit(): void {
    this.lookupService.loadAll(this.destroyRef, (err) =>
      this.handleError(err, 'cargar la información auxiliar'),
    );

    combineLatest([this.route.paramMap, this.route.queryParamMap])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(([params, query]) => {
        const pageParam = params.get('page');
        const requiredPage = pageParam ? Number(pageParam) : 0;
        if (Number.isNaN(requiredPage) || requiredPage < 0) {
          this.navigateToPage(0, paramMapToObject(query) ?? undefined);
          return;
        }

        const {
          uiFilters,
          requestFilters,
          queryParams: pagerParams,
        } = extractFiltersFromQuery(query);
        this.filters = uiFilters;
        this.pagerQueryParams = pagerParams;
        this.activeSearchFilters = requestFilters;
        this.loadContracts(requiredPage, requestFilters ?? undefined);
      });

    this.refreshSummary();
  }

  get pager(): PaginatedResponse<PurchaseSale> | undefined {
    return this.listState().pager;
  }

  get isListLoading(): boolean {
    return this.listState().loading;
  }

  get listError(): string | null {
    return this.listState().error;
  }

  get contracts(): PurchaseSale[] {
    return this.listState().items;
  }

  get totalContracts(): number {
    return this.summaryState().total;
  }

  get totalPurchases(): number {
    return this.summaryState().purchases;
  }

  get totalSales(): number {
    return this.summaryState().sales;
  }

  readonly getStatusLabel = getStatusLabel;
  readonly getContractTypeLabel = getContractTypeLabel;
  readonly getPaymentMethodLabel = getPaymentMethodLabel;
  readonly getStatusBadgeClass = getStatusBadgeClass;
  readonly getVehicleBadgeClass = getVehicleBadgeClass;

  getClientLabel(contract: PurchaseSale): string {
    const summary = contract.clientSummary;
    if (summary) {
      const pieces = [summary.name ?? `Cliente ##${summary.id}`];
      if (summary.identifier) {
        pieces.push(summary.identifier);
      }
      return pieces.join(' - ');
    }
    const fallback = this.lookupService.clientMap().get(contract.clientId);
    return fallback ? fallback.label : `Cliente #${contract.clientId}`;
  }

  getUserLabel(contract: PurchaseSale): string {
    const summary = contract.userSummary;
    if (summary) {
      return [summary.fullName ?? `Usuario #${summary.id}`]
        .filter(Boolean)
        .join(' ');
    }
    const fallback = this.lookupService.userMap().get(contract.userId);
    return fallback ? fallback.label : `Usuario #${contract.userId}`;
  }

  getVehicleLabel(contract: PurchaseSale): string {
    const summary = contract.vehicleSummary;
    if (summary) {
      const brand = summary.brand ?? 'Vehículo';
      const model = summary.model ?? 'N/D';
      const plate = summary.plate ?? 'N/D';
      return `${brand} ${model} (${plate})`;
    }
    if (!contract.vehicleId) {
      return 'Vehículo no disponible';
    }
    const fallback = this.lookupService.vehicleMap().get(contract.vehicleId);
    return fallback ? fallback.label : 'Vehículo';
  }

  getPurchaseDate(contract: PurchaseSale): string | null {
    if (contract.contractType !== ContractType.PURCHASE) {
      return null;
    }
    const vehicle = this.getVehicleOption(contract);
    return vehicle?.createdAt ?? contract.createdAt ?? null;
  }

  getSaleDate(contract: PurchaseSale): string | null {
    if (contract.contractType !== ContractType.SALE) {
      return null;
    }
    const vehicle = this.getVehicleOption(contract);
    return contract.createdAt ?? vehicle?.updatedAt ?? null;
  }

  navigateToCreate(): void {
    void this.router.navigate(['/purchase-sales/register']);
  }

  resetReportDates(): void {
    this.reportStartDate = null;
    this.reportEndDate = null;
  }

  downloadReport(format: ExportFormat): void {
    this.reportService.download(
      format,
      this.destroyRef,
      this.reportStartDate,
      this.reportEndDate,
    );
  }

  applyFilters(): void {
    this.quickSuggestions = [];
    hintQuickSearchFilters(this.filters, this.buildSearchContext());
    const queryParams = buildQueryParamsFromFilters(this.filters);
    void this.router.navigate(['/purchase-sales/page', 0], { queryParams });
  }

  clearFilters(): void {
    this.filters = getDefaultUiFilters();
    this.quickSuggestions = [];
    void this.router.navigate(['/purchase-sales/page', 0]);
  }

  resetFilters(): void {
    this.clearFilters();
  }

  onPriceFilterChange(field: PriceFilterKey, rawValue: string): void {
    this.filters[field] = normalizePriceInput(rawValue);
  }

  onQuickSearchChange(term: string): void {
    this.filters.term = term;
    this.quickSuggestions = buildQuickSuggestions(
      term,
      this.buildSearchContext(),
    );
  }

  selectQuickSuggestion(suggestion: QuickSuggestion): void {
    if (suggestion.type === 'client') {
      this.filters.clientId = suggestion.value;
      this.filters.term = '';
    } else if (suggestion.type === 'user') {
      this.filters.userId = suggestion.value;
      this.filters.term = '';
    } else if (suggestion.type === 'vehicle') {
      this.filters.vehicleId = suggestion.value;
      this.filters.term = '';
    } else if (suggestion.type === 'status') {
      this.filters.contractStatus = suggestion.value as ContractStatusFilter;
      this.filters.term = '';
    } else if (suggestion.type === 'type') {
      this.filters.contractType = suggestion.value as ContractTypeFilter;
      this.filters.term = '';
    }
    this.quickSuggestions = [];
    this.applyFilters();
  }

  deleteContract(contract: PurchaseSale): void {
    if (!contract.id) {
      return;
    }

    if (contract.contractStatus !== ContractStatus.CANCELED) {
      void Swal.fire({
        icon: 'warning',
        title: 'Acción no permitida',
        text: 'Solo se pueden eliminar operaciones canceladas.',
        confirmButtonColor: '#0d6efd',
      });
      return;
    }

    void Swal.fire({
      title: '¿Confirmas esta acción?',
      text: `Vas a eliminar la operación #${contract.id}.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, continuar',
      cancelButtonText: 'No, cancelar',
      confirmButtonColor: '#fd0d0d',
      cancelButtonColor: '#6c757d',
    }).then((result) => {
      if (!result.isConfirmed) {
        return;
      }

      this.purchaseSaleService.deleteById(contract.id!).subscribe({
        next: () => {
          const vehicleMsg = this.decorateVehicleMessage(
            contract.vehicleSummary?.plate ?? null,
          );
          this.showSuccessMessage(
            `Operación #${contract.id} eliminada correctamente.${vehicleMsg}`,
          );
          this.reloadCurrentPage();
        },
        error: (error) => this.handleError(error, 'eliminar la operación'),
      });
    });
  }

  updateStatus(contract: PurchaseSale, status: ContractStatus): void {
    if (!contract.id) {
      return;
    }

    const actionLabels: Record<ContractStatus, string> = {
      [ContractStatus.PENDING]: 'marcar como pendiente',
      [ContractStatus.ACTIVE]: 'marcar como activa',
      [ContractStatus.COMPLETED]: 'marcar como completada',
      [ContractStatus.CANCELED]: 'cancelar',
    };

    void Swal.fire({
      title: '¿Confirmas esta acción?',
      text: `Vas a ${actionLabels[status]} la operación #${contract.id}.`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, continuar',
      cancelButtonText: 'No, cancelar',
      confirmButtonColor: '#0d6efd',
      cancelButtonColor: '#6c757d',
    }).then((result) => {
      if (!result.isConfirmed) {
        return;
      }

      const payload: PurchaseSale = { ...contract, contractStatus: status };

      this.purchaseSaleService
        .update(contract.id!, payload)
        .pipe(
          tap(() => this.showSuccessMessage('Contrato actualizado con éxito.')),
        )
        .subscribe({
          next: () => {
            this.reloadCurrentPage();
            this.refreshSummary();
          },
          error: (error) => this.handleError(error, 'actualizar el contrato'),
        });
    });
  }

  private getVehicleOption(contract: PurchaseSale): VehicleOption | undefined {
    return contract.vehicleId
      ? this.lookupService.vehicleMap().get(contract.vehicleId)
      : undefined;
  }

  private buildSearchContext() {
    return {
      clients: this.lookupService.clients(),
      users: this.lookupService.users(),
      vehicles: this.lookupService.vehicles(),
      linkedClientIds: this.linkedClientIds,
      linkedUserIds: this.linkedUserIds,
      linkedVehicleIds: this.linkedVehicleIds,
      contractStatuses: this.contractStatuses,
      contractTypes: this.contractTypes,
    };
  }

  private reloadCurrentPage(): void {
    this.loadContracts(this.currentPage, this.activeSearchFilters ?? undefined);
  }

  private loadContracts(
    page: number,
    filters?: PurchaseSaleSearchFilters,
  ): void {
    this.listState.update((s) => ({ ...s, loading: true, error: null }));

    const request$ = filters
      ? this.purchaseSaleService.searchPaginated({
          ...filters,
          page,
          size: 10,
        })
      : this.purchaseSaleService.getAllPaginated(page);

    request$
      .pipe(
        finalize(() =>
          this.listState.update((s) => ({ ...s, loading: false })),
        ),
      )
      .subscribe({
        next: (pager) => {
          this.listState.update((s) => ({
            ...s,
            items: pager.content,
            pager,
          }));
          this.currentPage = pager.number;
        },
        error: (error) => {
          this.listState.update((s) => ({
            ...s,
            error: 'No se pudieron cargar los contratos de compra/venta.',
          }));
          this.handleError(error, 'cargar los contratos', false);
        },
      });
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
          this.summaryState.set({
            total: contracts.length,
            purchases,
            sales,
          });
          this.updateLinkedEntities(contracts);
        },
        error: () => {
          this.summaryState.set({ total: 0, purchases: 0, sales: 0 });
          this.updateLinkedEntities([]);
        },
      });
  }

  private updateLinkedEntities(contracts: PurchaseSale[]): void {
    this.linkedClientIds.clear();
    this.linkedUserIds.clear();
    this.linkedVehicleIds.clear();

    for (const c of contracts) {
      this.trackLinkedId(this.linkedClientIds, c.clientId);
      this.trackLinkedId(this.linkedUserIds, c.userId);
      this.trackLinkedId(this.linkedVehicleIds, c.vehicleId);
    }
  }

  private trackLinkedId(
    target: Set<number>,
    value: number | null | undefined,
  ): void {
    if (typeof value === 'number' && Number.isFinite(value)) {
      target.add(value);
    }
  }

  private navigateToPage(page: number, queryParams?: Params): void {
    void this.router.navigate(['/purchase-sales/page', page], { queryParams });
  }

  private showSuccessMessage(message: string): void {
    void Swal.fire({
      icon: 'success',
      title: 'Operación exitosa',
      text: message,
      timer: 2200,
      showConfirmButton: false,
    });
  }

  private handleError(
    error: unknown,
    action: string,
    displayAlert = true,
  ): void {
    if (displayAlert) {
      showHttpError(error, action, (msg) => this.decorateVehicleMessage(msg));
    }
  }

  private decorateVehicleMessage(message: string | null): string {
    if (!message) {
      return '';
    }
    return message.replaceAll(/veh[ií]culo con id (\d+)/gi, (_, id: string) => {
      const numericId = Number(id);
      const option = this.lookupService.vehicleMap().get(numericId);
      return option ? option.label : `vehículo con id ${id}`;
    });
  }
}
