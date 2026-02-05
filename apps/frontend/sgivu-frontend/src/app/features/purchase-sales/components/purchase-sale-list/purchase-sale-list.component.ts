import { CommonModule } from '@angular/common';
import {
  Component,
  computed,
  inject,
  OnDestroy,
  OnInit,
  signal,
  WritableSignal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  ActivatedRoute,
  ParamMap,
  Params,
  Router,
  RouterLink,
} from '@angular/router';
import {
  combineLatest,
  finalize,
  forkJoin,
  map,
  Subscription,
  tap,
} from 'rxjs';
import Swal from 'sweetalert2';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
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
import { PersonService } from '../../../clients/services/person.service';
import { CompanyService } from '../../../clients/services/company.service';
import { UserService } from '../../../users/services/user.service';
import { CarService } from '../../../vehicles/services/car.service';
import { MotorcycleService } from '../../../vehicles/services/motorcycle.service';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { KpiCardComponent } from '../../../../shared/components/kpi-card/kpi-card.component';
import { DataTableComponent } from '../../../../shared/components/data-table/data-table.component';
import { CopCurrencyPipe } from '../../../../shared/pipes/cop-currency.pipe';
import {
  normalizeMoneyInput,
  parseCopCurrency,
} from '../../../../shared/utils/currency.utils';
import { RowNavigateDirective } from '../../../../shared/directives/row-navigate.directive';
import {
  ClientOption,
  mapCarsToVehicles,
  mapCompaniesToClients,
  mapMotorcyclesToVehicles,
  mapPersonsToClients,
  mapUsersToOptions,
  UserOption,
  VehicleOption,
} from '../../models/purchase-sale-reference.model';

interface PurchaseSaleListState {
  items: PurchaseSale[];
  pager?: PaginatedResponse<PurchaseSale>;
  loading: boolean;
  error: string | null;
}

type ContractTypeFilter = ContractType | 'ALL';
type ContractStatusFilter = ContractStatus | 'ALL';
type PriceFilterKey =
  | 'minPurchasePrice'
  | 'maxPurchasePrice'
  | 'minSalePrice'
  | 'maxSalePrice';

interface PurchaseSaleUiFilters {
  contractType: ContractTypeFilter;
  contractStatus: ContractStatusFilter;
  clientId: string;
  userId: string;
  vehicleId: string;
  paymentMethod: string;
  term: string;
  minPurchasePrice: string;
  maxPurchasePrice: string;
  minSalePrice: string;
  maxSalePrice: string;
}

type ExportFormat = 'pdf' | 'excel' | 'csv';
type ReportExtension = 'pdf' | 'xlsx' | 'csv';
type QuickSuggestionType = 'client' | 'user' | 'vehicle' | 'status' | 'type';
interface QuickSuggestion {
  label: string;
  context: string;
  type: QuickSuggestionType;
  value: string;
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
})
export class PurchaseSaleListComponent implements OnInit, OnDestroy {
  readonly contractStatuses = Object.values(ContractStatus);
  readonly contractTypes = Object.values(ContractType);
  readonly ContractStatus = ContractStatus;
  readonly ContractType = ContractType;
  readonly paymentMethods = Object.values(PaymentMethod);
  readonly clients: WritableSignal<ClientOption[]> = signal<ClientOption[]>([]);
  readonly users: WritableSignal<UserOption[]> = signal<UserOption[]>([]);
  readonly vehicles: WritableSignal<VehicleOption[]> = signal<VehicleOption[]>(
    [],
  );
  readonly clientMap = computed(
    () =>
      new Map<number, ClientOption>(
        this.clients().map((client) => [client.id, client]),
      ),
  );
  readonly userMap = computed(
    () =>
      new Map<number, UserOption>(this.users().map((user) => [user.id, user])),
  );
  readonly vehicleMap = computed(
    () =>
      new Map<number, VehicleOption>(
        this.vehicles().map((vehicle) => [vehicle.id, vehicle]),
      ),
  );
  readonly summaryState = signal({
    total: 0,
    purchases: 0,
    sales: 0,
  });
  filters: PurchaseSaleUiFilters = this.getDefaultUiFilters();
  reportStartDate: string | null = null;
  reportEndDate: string | null = null;
  exportLoading: Record<ExportFormat, boolean> = {
    pdf: false,
    excel: false,
    csv: false,
  };
  listState: PurchaseSaleListState = {
    items: [],
    loading: false,
    error: null,
  };
  quickSuggestions: QuickSuggestion[] = [];
  pagerQueryParams: Params | null = null;
  readonly pagerUrl = '/purchase-sales/page';
  private readonly purchaseSaleService = inject(PurchaseSaleService);
  private readonly personService = inject(PersonService);
  private readonly companyService = inject(CompanyService);
  private readonly userService = inject(UserService);
  private readonly carService = inject(CarService);
  private readonly motorcycleService = inject(MotorcycleService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly linkedClientIds = new Set<number>();
  private readonly linkedUserIds = new Set<number>();
  private readonly linkedVehicleIds = new Set<number>();
  private currentPage = 0;
  private readonly subscriptions: Subscription[] = [];
  private activeSearchFilters: PurchaseSaleSearchFilters | null = null;
  private readonly priceDecimals = 0;
  private readonly statusLabels: Record<ContractStatus, string> = {
    [ContractStatus.PENDING]: 'Pendiente',
    [ContractStatus.ACTIVE]: 'Activo',
    [ContractStatus.COMPLETED]: 'Completado',
    [ContractStatus.CANCELED]: 'Cancelado',
  };
  private readonly typeLabels: Record<ContractType, string> = {
    [ContractType.PURCHASE]: 'Compra',
    [ContractType.SALE]: 'Venta',
  };
  private readonly paymentMethodLabels: Record<PaymentMethod, string> = {
    [PaymentMethod.CASH]: 'Efectivo',
    [PaymentMethod.BANK_TRANSFER]: 'Transferencia bancaria',
    [PaymentMethod.BANK_DEPOSIT]: 'Consignación bancaria',
    [PaymentMethod.CASHIERS_CHECK]: 'Cheque de gerencia',
    [PaymentMethod.MIXED]: 'Pago combinado',
    [PaymentMethod.FINANCING]: 'Financiación',
    [PaymentMethod.DIGITAL_WALLET]: 'Billetera digital',
    [PaymentMethod.TRADE_IN]: 'Permuta',
    [PaymentMethod.INSTALLMENT_PAYMENT]: 'Pago a plazos',
  };

  get pager(): PaginatedResponse<PurchaseSale> | undefined {
    return this.listState.pager;
  }

  get isListLoading(): boolean {
    return this.listState.loading;
  }

  get listError(): string | null {
    return this.listState.error;
  }

  get contracts(): PurchaseSale[] {
    return this.listState.items;
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

  ngOnInit(): void {
    this.loadLookups();
    const routeSub = combineLatest([
      this.route.paramMap,
      this.route.queryParamMap,
    ]).subscribe(([params, query]) => {
      const pageParam = params.get('page');
      const requiredPage = this.parsePage(pageParam);
      if (Number.isNaN(requiredPage) || requiredPage < 0) {
        this.navigateToPage(0, this.paramMapToObject(query) ?? undefined);
        return;
      }

      const {
        uiFilters,
        requestFilters,
        queryParams: pagerParams,
      } = this.extractFiltersFromQuery(query);
      this.filters = uiFilters;
      this.pagerQueryParams = pagerParams;
      this.activeSearchFilters = requestFilters;
      this.loadContracts(requiredPage, requestFilters ?? undefined);
    });

    this.subscriptions.push(routeSub);
    this.refreshSummary();
  }

  ngOnDestroy(): void {
    for (const sub of this.subscriptions) {
      sub.unsubscribe();
    }
  }

  navigateToCreate(): void {
    void this.router.navigate(['/purchase-sales/register']);
  }

  getVehicleBadgeClass(contract: PurchaseSale): string {
    return contract.contractType === ContractType.PURCHASE
      ? 'bg-primary-subtle text-primary-emphasis'
      : 'bg-success-subtle text-success-emphasis';
  }

  getStatusBadgeClass(status: ContractStatus): string {
    switch (status) {
      case ContractStatus.ACTIVE:
        return 'bg-primary-subtle text-primary-emphasis';
      case ContractStatus.COMPLETED:
        return 'bg-success-subtle text-success-emphasis';
      case ContractStatus.CANCELED:
        return 'bg-danger-subtle text-danger-emphasis';
      default:
        return 'bg-warning-subtle text-warning-emphasis';
    }
  }

  getStatusLabel(status: ContractStatus): string {
    return this.statusLabels[status] ?? status;
  }

  getContractTypeLabel(type: ContractType): string {
    return this.typeLabels[type] ?? type;
  }

  getPaymentMethodLabel(method: PaymentMethod): string {
    return this.paymentMethodLabels[method] ?? method;
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

  getClientLabel(contract: PurchaseSale): string {
    const summary = contract.clientSummary;
    if (summary) {
      const pieces = [summary.name ?? `Cliente ##${summary.id}`];
      if (summary.identifier) {
        pieces.push(summary.identifier);
      }
      return pieces.join(' - ');
    }

    const fallback = this.clientMap().get(contract.clientId);
    return fallback ? fallback.label : `Cliente #${contract.clientId}`;
  }

  getUserLabel(contract: PurchaseSale): string {
    const summary = contract.userSummary;
    if (summary) {
      return [summary.fullName ?? `Usuario #${summary.id}`]
        .filter(Boolean)
        .join(' ');
    }

    const fallback = this.userMap().get(contract.userId);
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

    const fallback = this.vehicleMap().get(contract.vehicleId);
    return fallback ? fallback.label : 'Vehículo';
  }

  resetFilters(): void {
    this.clearFilters();
  }

  resetReportDates(): void {
    this.reportStartDate = null;
    this.reportEndDate = null;
  }

  applyFilters(): void {
    this.quickSuggestions = [];
    this.hintQuickSearchFilters();
    const queryParams = this.buildQueryParamsFromFilters();
    void this.router.navigate(['/purchase-sales/page', 0], {
      queryParams,
    });
  }

  clearFilters(): void {
    this.filters = this.getDefaultUiFilters();
    this.quickSuggestions = [];
    void this.router.navigate(['/purchase-sales/page', 0]);
  }

  onPriceFilterChange(field: PriceFilterKey, rawValue: string): void {
    const { displayValue } = normalizeMoneyInput(rawValue, this.priceDecimals);
    this.filters[field] = displayValue;
  }

  downloadReport(format: ExportFormat): void {
    if (this.reportStartDate && this.reportEndDate) {
      if (this.reportStartDate > this.reportEndDate) {
        void Swal.fire({
          icon: 'warning',
          title: 'Rango inválido',
          text: 'La fecha inicial no puede ser posterior a la fecha final.',
        });
        return;
      }
    }

    this.exportLoading[format] = true;
    const start = this.reportStartDate ?? undefined;
    const end = this.reportEndDate ?? undefined;
    const request$ = this.getReportObservable(format, start, end);

    request$
      .pipe(finalize(() => (this.exportLoading[format] = false)))
      .subscribe({
        next: (blob) => {
          const extension = this.getExtension(format);
          const fileName = this.buildReportFileName(extension);
          const url = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = fileName;
          document.body.appendChild(link);
          link.click();
          link.remove();
          URL.revokeObjectURL(url);
          this.showSuccessMessage(
            `Reporte ${extension.toUpperCase()} generado correctamente.`,
          );
        },
        error: (error) => this.handleError(error, 'generar el reporte'),
      });
  }

  onQuickSearchChange(term: string): void {
    this.filters.term = term;
    this.updateQuickSuggestions(term);
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

      const payload: PurchaseSale = {
        ...contract,
        contractStatus: status,
      };

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
    if (!contract.vehicleId) {
      return undefined;
    }
    return this.vehicleMap().get(contract.vehicleId);
  }

  private getVehicleLabelById(vehicleId: number): string | null {
    const option = this.vehicleMap().get(vehicleId);
    return option ? option.label : null;
  }

  private getReportObservable(
    format: ExportFormat,
    start?: string,
    end?: string,
  ) {
    switch (format) {
      case 'pdf':
        return this.purchaseSaleService.downloadPdf(start, end);
      case 'excel':
        return this.purchaseSaleService.downloadExcel(start, end);
      case 'csv':
      default:
        return this.purchaseSaleService.downloadCsv(start, end);
    }
  }

  private getExtension(format: ExportFormat): ReportExtension {
    if (format === 'pdf') {
      return 'pdf';
    }
    if (format === 'excel') {
      return 'xlsx';
    }
    return 'csv';
  }

  private buildReportFileName(extension: ReportExtension): string {
    const today = new Date().toISOString().split('T')[0];
    const rangeLabel =
      this.reportStartDate || this.reportEndDate
        ? `${this.reportStartDate ?? 'inicio'}-a-${this.reportEndDate ?? 'fin'}`
        : 'completo';
    return `reporte-compras-ventas-${rangeLabel}-${today}.${extension}`;
  }

  private reloadCurrentPage(): void {
    this.loadContracts(this.currentPage, this.activeSearchFilters ?? undefined);
  }

  private loadContracts(
    page: number,
    filters?: PurchaseSaleSearchFilters,
  ): void {
    this.listState.loading = true;
    this.listState.error = null;

    const request$ = filters
      ? this.purchaseSaleService.searchPaginated({
          ...filters,
          page,
          size: 10,
        })
      : this.purchaseSaleService.getAllPaginated(page);

    request$.pipe(finalize(() => (this.listState.loading = false))).subscribe({
      next: (pager) => {
        this.listState.items = pager.content;
        this.listState.pager = pager;
        this.currentPage = pager.number;
      },
      error: (error) => {
        this.listState.error =
          'No se pudieron cargar los contratos de compra/venta.';
        this.handleError(error, 'cargar los contratos', false);
      },
    });
  }

  private loadLookups(): void {
    const clients$ = forkJoin([
      this.personService.getAll(),
      this.companyService.getAll(),
    ]).pipe(
      map(([persons, companies]) => [
        ...mapPersonsToClients(persons),
        ...mapCompaniesToClients(companies),
      ]),
    );

    const users$ = this.userService.getAll().pipe(map(mapUsersToOptions));

    const vehicles$ = forkJoin([
      this.carService.getAll(),
      this.motorcycleService.getAll(),
    ]).pipe(
      map(([cars, motorcycles]) => [
        ...mapCarsToVehicles(cars),
        ...mapMotorcyclesToVehicles(motorcycles),
      ]),
    );

    const lookupSub = forkJoin([clients$, users$, vehicles$]).subscribe({
      next: ([clientOptions, userOptions, vehicleOptions]) => {
        const sortedClients = clientOptions
          .slice()
          .sort((a, b) => a.label.localeCompare(b.label));
        const sortedUsers = userOptions
          .slice()
          .sort((a, b) => a.label.localeCompare(b.label));
        const sortedVehicles = vehicleOptions
          .slice()
          .sort((a, b) => a.label.localeCompare(b.label));

        this.clients.set(sortedClients);
        this.users.set(sortedUsers);
        this.vehicles.set(sortedVehicles);
      },
      error: (error) => {
        this.handleError(error, 'cargar la información auxiliar');
      },
    });

    this.subscriptions.push(lookupSub);
  }

  private refreshSummary(): void {
    const summarySub = this.purchaseSaleService.getAll().subscribe({
      next: (contracts) => {
        const purchases = contracts.filter(
          (contract) => contract.contractType === ContractType.PURCHASE,
        ).length;
        const sales = contracts.filter(
          (contract) => contract.contractType === ContractType.SALE,
        ).length;

        this.summaryState.set({
          total: contracts.length,
          purchases,
          sales,
        });
        this.updateLinkedEntities(contracts);
      },
      error: () => {
        this.summaryState.set({
          total: 0,
          purchases: 0,
          sales: 0,
        });
        this.updateLinkedEntities([]);
      },
    });

    this.subscriptions.push(summarySub);
  }

  private updateLinkedEntities(contracts: PurchaseSale[]): void {
    this.linkedClientIds.clear();
    this.linkedUserIds.clear();
    this.linkedVehicleIds.clear();

    contracts.forEach((contract) => {
      this.trackLinkedId(this.linkedClientIds, contract.clientId);
      this.trackLinkedId(this.linkedUserIds, contract.userId);
      this.trackLinkedId(this.linkedVehicleIds, contract.vehicleId);
    });
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
    void this.router.navigate(['/purchase-sales/page', page], {
      queryParams,
    });
  }

  private parsePage(value: string | null): number {
    return value ? Number(value) : 0;
  }

  private getDefaultUiFilters(): PurchaseSaleUiFilters {
    return {
      contractType: 'ALL',
      contractStatus: 'ALL',
      clientId: '',
      userId: '',
      vehicleId: '',
      paymentMethod: '',
      term: '',
      minPurchasePrice: '',
      maxPurchasePrice: '',
      minSalePrice: '',
      maxSalePrice: '',
    };
  }

  private hintQuickSearchFilters(): void {
    const rawTerm = (this.filters.term ?? '').trim();
    if (!rawTerm) {
      return;
    }

    const normalized = rawTerm.toLowerCase();

    const vehicleMatch = this.vehicles().find(
      (vehicle) =>
        this.linkedVehicleIds.has(vehicle.id) &&
        this.includesTerm(vehicle.label, normalized),
    );
    if (vehicleMatch && !this.filters.vehicleId) {
      this.filters.vehicleId = vehicleMatch.id.toString();
    }

    const clientMatch = this.clients().find(
      (client) =>
        this.linkedClientIds.has(client.id) &&
        this.includesTerm(client.label, normalized),
    );
    if (clientMatch && !this.filters.clientId) {
      this.filters.clientId = clientMatch.id.toString();
    }

    const userMatch = this.users().find(
      (user) =>
        this.linkedUserIds.has(user.id) &&
        this.includesTerm(user.label, normalized),
    );
    if (userMatch && !this.filters.userId) {
      this.filters.userId = userMatch.id.toString();
    }

    this.filters.term = rawTerm;
  }

  private updateQuickSuggestions(term: string): void {
    const normalized = term.trim().toLowerCase();
    if (normalized.length < 2) {
      this.quickSuggestions = [];
      return;
    }

    const matches: QuickSuggestion[] = [];

    this.clients()
      .filter(
        (client) =>
          this.linkedClientIds.has(client.id) &&
          this.includesTerm(client.label, normalized),
      )
      .slice(0, 3)
      .forEach((client) =>
        matches.push({
          label: client.label,
          context: 'Cliente con contratos',
          type: 'client',
          value: client.id.toString(),
        }),
      );

    this.users()
      .filter(
        (user) =>
          this.linkedUserIds.has(user.id) &&
          this.includesTerm(user.label, normalized),
      )
      .slice(0, 3)
      .forEach((user) =>
        matches.push({
          label: user.label,
          context: 'Usuario con contratos',
          type: 'user',
          value: user.id.toString(),
        }),
      );

    this.vehicles()
      .filter(
        (vehicle) =>
          this.linkedVehicleIds.has(vehicle.id) &&
          this.includesTerm(vehicle.label, normalized),
      )
      .slice(0, 3)
      .forEach((vehicle) =>
        matches.push({
          label: vehicle.label,
          context: 'Vehículo utilizado en contratos',
          type: 'vehicle',
          value: vehicle.id.toString(),
        }),
      );

    this.contractStatuses
      .filter((status) => this.matchesStatus(status, normalized))
      .forEach((status) =>
        matches.push({
          label: this.getStatusLabel(status),
          context: 'Estado de contrato',
          type: 'status',
          value: status,
        }),
      );

    this.contractTypes
      .filter((type) => this.matchesType(type, normalized))
      .forEach((type) =>
        matches.push({
          label: this.getContractTypeLabel(type),
          context: 'Tipo de contrato',
          type: 'type',
          value: type,
        }),
      );

    this.quickSuggestions = matches.slice(0, 9);
  }

  private buildQueryParamsFromFilters(): Params | undefined {
    const params: Params = {};

    if (this.filters.contractType !== 'ALL') {
      params['contractType'] = this.filters.contractType;
    }

    if (this.filters.contractStatus !== 'ALL') {
      params['contractStatus'] = this.filters.contractStatus;
    }

    if (this.filters.paymentMethod) {
      params['paymentMethod'] = this.filters.paymentMethod;
    }

    [
      ['clientId', this.filters.clientId],
      ['userId', this.filters.userId],
      ['vehicleId', this.filters.vehicleId],
      ['term', this.filters.term],
    ].forEach(([key, value]) => {
      if (value) {
        params[key] = value;
      }
    });

    const parsedPriceFilters: Partial<Record<PriceFilterKey, number | null>> = {
      minPurchasePrice: this.parsePriceFilter(this.filters.minPurchasePrice),
      maxPurchasePrice: this.parsePriceFilter(this.filters.maxPurchasePrice),
      minSalePrice: this.parsePriceFilter(this.filters.minSalePrice),
      maxSalePrice: this.parsePriceFilter(this.filters.maxSalePrice),
    };

    (
      Object.entries(parsedPriceFilters) as [PriceFilterKey, number | null][]
    ).forEach(([key, value]) => {
      if (value !== null) {
        params[key] = value;
      }
    });

    return Object.keys(params).length ? params : undefined;
  }

  private includesTerm(value: string, normalizedTerm: string): boolean {
    return value.toLowerCase().includes(normalizedTerm);
  }

  private matchesStatus(
    status: ContractStatus,
    normalizedTerm: string,
  ): boolean {
    const label = this.getStatusLabel(status).toLowerCase();
    return (
      label.includes(normalizedTerm) ||
      status.toLowerCase().includes(normalizedTerm)
    );
  }

  private matchesType(type: ContractType, normalizedTerm: string): boolean {
    const label = this.getContractTypeLabel(type).toLowerCase();
    return (
      label.includes(normalizedTerm) ||
      type.toLowerCase().includes(normalizedTerm)
    );
  }

  private extractFiltersFromQuery(query: ParamMap): {
    uiFilters: PurchaseSaleUiFilters;
    requestFilters: PurchaseSaleSearchFilters | null;
    queryParams: Params | null;
  } {
    const uiFilters = this.getDefaultUiFilters();
    const requestFilters: PurchaseSaleSearchFilters = {};

    const applyEnum = <T extends string>(
      key: string,
      isValid: (v: string | null) => v is T,
    ) => {
      const val = query.get(key);
      if (isValid(val)) {
        (uiFilters as any)[key] = val;
        (requestFilters as any)[key] = val;
      }
    };

    const applyNumber = (key: string, requestKey?: string) => {
      const val = query.get(key);
      if (!val) return;
      (uiFilters as any)[key] = val;
      const parsed = this.parseNumberParam(val);
      if (parsed !== undefined) {
        (requestFilters as any)[requestKey ?? key] = parsed;
      }
    };

    const applyPrice = (key: string, requestKey?: string) => {
      const val = query.get(key);
      if (!val) return;
      const { numericValue, displayValue } = normalizeMoneyInput(
        val,
        this.priceDecimals,
      );
      (uiFilters as any)[key] = displayValue;
      if (numericValue !== null) {
        (requestFilters as any)[requestKey ?? key] = numericValue;
      }
    };

    const applyString = (key: string) => {
      const val = query.get(key);
      if (!val) return;
      (uiFilters as any)[key] = val;
      (requestFilters as any)[key] = val;
    };

    applyEnum('contractType', this.isValidContractType);
    applyEnum('contractStatus', this.isValidContractStatus);
    applyEnum('paymentMethod', this.isValidPaymentMethod);

    applyNumber('clientId', 'clientId');
    applyNumber('userId', 'userId');
    applyNumber('vehicleId', 'vehicleId');

    applyPrice('minPurchasePrice', 'minPurchasePrice');
    applyPrice('maxPurchasePrice', 'maxPurchasePrice');
    applyPrice('minSalePrice', 'minSalePrice');
    applyPrice('maxSalePrice', 'maxSalePrice');

    applyString('term');

    const queryParams = this.paramMapToObject(query);
    const hasFilters = !this.arePurchaseSaleFiltersEmpty(requestFilters);

    return {
      uiFilters,
      requestFilters: hasFilters ? requestFilters : null,
      queryParams,
    };
  }

  private paramMapToObject(map: ParamMap): Params | null {
    const params: Params = {};
    map.keys.forEach((key) => {
      const value = map.get(key);
      if (value) {
        params[key] = value;
      }
    });
    return Object.keys(params).length ? params : null;
  }

  private parsePriceFilter(value: string): number | null {
    const parsed = parseCopCurrency(value);
    if (parsed === null) {
      return null;
    }
    const factor = Math.pow(10, this.priceDecimals);
    const normalized =
      this.priceDecimals > 0
        ? Math.round(parsed * factor) / factor
        : Math.round(parsed);
    const sanitized = Math.max(0, normalized);
    return Number.isFinite(sanitized) ? sanitized : null;
  }

  private parseNumberParam(value: string | null): number | undefined {
    if (!value) {
      return undefined;
    }
    const parsed = Number(value);
    return Number.isNaN(parsed) ? undefined : parsed;
  }

  private arePurchaseSaleFiltersEmpty(
    filters: PurchaseSaleSearchFilters,
  ): boolean {
    return Object.values(filters).every(
      (value) => value === undefined || value === null,
    );
  }

  private isValidContractType(value: string | null): value is ContractType {
    return !!value && this.contractTypes.includes(value as ContractType);
  }

  private isValidContractStatus(value: string | null): value is ContractStatus {
    return !!value && this.contractStatuses.includes(value as ContractStatus);
  }

  private isValidPaymentMethod(value: string | null): value is PaymentMethod {
    return (
      !!value && Object.values(PaymentMethod).includes(value as PaymentMethod)
    );
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
    const details =
      (error as { error?: { details?: string } })?.error?.details ?? null;
    const message = this.decorateVehicleMessage(
      details ??
        `Se presentó un inconveniente al ${action}. Intenta nuevamente.`,
    );

    if (displayAlert) {
      void Swal.fire({
        icon: 'error',
        title: 'Oops...',
        text: message,
      });
    }
  }

  private decorateVehicleMessage(message: string | null): string {
    if (!message) {
      return '';
    }

    return message.replaceAll(/veh[ií]culo con id (\d+)/gi, (_, id: string) => {
      const numericId = Number(id);
      const label = this.getVehicleLabelById(numericId);
      return label ?? `vehículo con id ${id}`;
    });
  }
}
