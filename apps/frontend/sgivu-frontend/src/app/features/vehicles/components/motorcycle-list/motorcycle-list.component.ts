import {
  Component,
  DestroyRef,
  OnInit,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute, Params } from '@angular/router';
import { combineLatest, map } from 'rxjs';
import Swal from 'sweetalert2';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { PagerComponent } from '../../../../shared/components/pager/pager.component';
import { KpiCardComponent } from '../../../../shared/components/kpi-card/kpi-card.component';
import { DataTableComponent } from '../../../../shared/components/data-table/data-table.component';
import { RowNavigateDirective } from '../../../../shared/directives/row-navigate.directive';
import { CopCurrencyPipe } from '../../../../shared/pipes/cop-currency.pipe';
import {
  MotorcycleService,
  MotorcycleSearchFilters,
} from '../../services/motorcycle.service';
import { Motorcycle } from '../../models/motorcycle.model';
import { VehicleStatus } from '../../models/vehicle-status.enum';
import { VehicleUiHelperService } from '../../../../shared/services/vehicle-ui-helper.service';
import {
  ListPageManager,
  FallbackCountsResult,
} from '../../../../shared/utils/list-page-manager';
import { normalizeMoneyInput } from '../../../../shared/utils/currency.utils';
import {
  FilterFieldMapping,
  extractFiltersFromQuery,
  buildQueryParams,
} from '../../../../shared/utils/filter-query.utils';

const MOTORCYCLE_FILTER_MAPPINGS: FilterFieldMapping[] = [
  { queryKey: 'motorcyclePlate', filterKey: 'plate', type: 'string' },
  { queryKey: 'motorcycleBrand', filterKey: 'brand', type: 'string' },
  { queryKey: 'motorcycleLine', filterKey: 'line', type: 'string' },
  { queryKey: 'motorcycleModel', filterKey: 'model', type: 'string' },
  { queryKey: 'motorcycleType', filterKey: 'motorcycleType', type: 'string' },
  {
    queryKey: 'motorcycleTransmission',
    filterKey: 'transmission',
    type: 'string',
  },
  { queryKey: 'motorcycleCity', filterKey: 'cityRegistered', type: 'string' },
  { queryKey: 'motorcycleStatus', filterKey: 'status', type: 'enum' },
  { queryKey: 'motorcycleMinYear', filterKey: 'minYear', type: 'number' },
  { queryKey: 'motorcycleMaxYear', filterKey: 'maxYear', type: 'number' },
  {
    queryKey: 'motorcycleMinCapacity',
    filterKey: 'minCapacity',
    type: 'number',
  },
  {
    queryKey: 'motorcycleMaxCapacity',
    filterKey: 'maxCapacity',
    type: 'number',
  },
  { queryKey: 'motorcycleMinMileage', filterKey: 'minMileage', type: 'number' },
  { queryKey: 'motorcycleMaxMileage', filterKey: 'maxMileage', type: 'number' },
  {
    queryKey: 'motorcycleMinSalePrice',
    filterKey: 'minSalePrice',
    type: 'price',
  },
  {
    queryKey: 'motorcycleMaxSalePrice',
    filterKey: 'maxSalePrice',
    type: 'price',
  },
];

@Component({
  selector: 'app-motorcycle-list',
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    HasPermissionDirective,
    PagerComponent,
    KpiCardComponent,
    DataTableComponent,
    CopCurrencyPipe,
    RowNavigateDirective,
  ],
  templateUrl: './motorcycle-list.component.html',
  styleUrl: './motorcycle-list.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MotorcycleListComponent implements OnInit {
  private readonly motorcycleService = inject(MotorcycleService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly vehicleUiHelper = inject(VehicleUiHelperService);
  private readonly destroyRef = inject(DestroyRef);

  readonly listManager = new ListPageManager<Motorcycle>(this.destroyRef);
  readonly vehicleStatuses = Object.values(VehicleStatus);
  readonly VehicleStatus = VehicleStatus;

  filters: MotorcycleSearchFilters = this.createFilterState();
  priceInputs: Record<'minSalePrice' | 'maxSalePrice', string> = {
    minSalePrice: '',
    maxSalePrice: '',
  };

  private activeFilters: MotorcycleSearchFilters | null = null;
  private queryParams: Params | null = null;
  private readonly priceDecimals = 0;

  readonly pagerUrl = '/vehicles/motorcycles/page';

  get activePagerQueryParams(): Params | null {
    return this.queryParams;
  }

  ngOnInit(): void {
    combineLatest([this.route.paramMap, this.route.queryParamMap])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(([params, query]) => {
        const page = ListPageManager.parsePage(params.get('page'));
        const filterInfo = extractFiltersFromQuery<MotorcycleSearchFilters>(
          query,
          MOTORCYCLE_FILTER_MAPPINGS,
          () => this.createFilterState(),
        );

        this.filters = filterInfo.uiState;
        this.priceInputs = filterInfo.priceInputs as Record<
          'minSalePrice' | 'maxSalePrice',
          string
        >;
        this.activeFilters = filterInfo.filters;
        this.queryParams = filterInfo.queryParams;

        if (page < 0) {
          this.navigateToPage(0, filterInfo.queryParams ?? undefined);
          return;
        }

        this.loadMotorcycles(page, this.activeFilters ?? undefined);
      });
  }

  protected applyFilters(): void {
    this.syncPriceFilters();
    if (
      ListPageManager.areFiltersEmpty(this.filters as Record<string, unknown>)
    ) {
      this.clearFilters();
      return;
    }
    const qp = buildQueryParams(this.filters, MOTORCYCLE_FILTER_MAPPINGS);
    this.navigateToPage(0, qp ?? undefined);
  }

  protected clearFilters(): void {
    this.filters = this.createFilterState();
    this.priceInputs = { minSalePrice: '', maxSalePrice: '' };
    this.activeFilters = null;
    this.queryParams = null;
    this.navigateToPage(0);
  }

  protected changeStatus(motorcycle: Motorcycle, status: VehicleStatus): void {
    const previous = motorcycle.status;
    motorcycle.status = status;
    this.motorcycleService
      .changeStatus(motorcycle.id, status)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          void Swal.fire({
            icon: 'success',
            title: 'Estado actualizado',
            text: 'El estado del vehículo fue actualizado correctamente.',
            timer: 2000,
            showConfirmButton: false,
          });
        },
        error: () => {
          motorcycle.status = previous;
          void Swal.fire({
            icon: 'error',
            title: 'Error al actualizar el estado',
            text: 'No se pudo actualizar el estado del vehículo.',
          });
        },
      });
  }

  protected toggleAvailability(motorcycle: Motorcycle): void {
    this.vehicleUiHelper.updateMotorcycleStatus(
      motorcycle.id,
      motorcycle.status === VehicleStatus.INACTIVE
        ? VehicleStatus.AVAILABLE
        : VehicleStatus.INACTIVE,
      () =>
        this.loadMotorcycles(
          this.listManager.currentPage(),
          this.activeFilters ?? undefined,
        ),
      motorcycle.plate,
    );
  }

  protected onPriceInput(
    field: 'minSalePrice' | 'maxSalePrice',
    rawValue: string,
  ): void {
    const { numericValue, displayValue } = normalizeMoneyInput(
      rawValue,
      this.priceDecimals,
    );
    this.priceInputs[field] = displayValue;
    this.filters[field] = numericValue;
  }

  protected statusLabel(status: VehicleStatus): string {
    const labels: Record<VehicleStatus, string> = {
      [VehicleStatus.AVAILABLE]: 'Disponible',
      [VehicleStatus.SOLD]: 'Vendido',
      [VehicleStatus.IN_MAINTENANCE]: 'En mantenimiento',
      [VehicleStatus.IN_REPAIR]: 'En reparación',
      [VehicleStatus.IN_USE]: 'En uso',
      [VehicleStatus.INACTIVE]: 'Inactivo',
    };
    return labels[status] ?? status;
  }

  protected toNumber(value: unknown): number | null {
    if (value === null || value === undefined || value === '') {
      return null;
    }
    const parsed = Number(value);
    return Number.isNaN(parsed) ? null : parsed;
  }

  private static computeVehicleCounts(items: Motorcycle[]): {
    active: number;
    inactive: number;
  } {
    const active = items.filter(
      (item) => item.status === VehicleStatus.AVAILABLE,
    ).length;
    return { active, inactive: items.length - active };
  }

  private loadMotorcycles(
    page: number,
    filters?: MotorcycleSearchFilters,
  ): void {
    const activeFilters =
      filters &&
      !ListPageManager.areFiltersEmpty(filters as Record<string, unknown>)
        ? filters
        : undefined;

    this.listManager.loadPage(
      {
        fetchPager: (p) =>
          activeFilters
            ? this.motorcycleService.searchPaginated(p, activeFilters)
            : this.motorcycleService.getAllPaginated(p),
        fetchCounts: () => this.motorcycleService.getCounts(),
        errorMessage: 'Error al cargar las motocicletas.',
        countKeys: {
          active: ['availableMotorcycles', 'available', 'availableVehicles'],
          inactive: [
            'unavailableMotorcycles',
            'unavailable',
            'unavailableVehicles',
          ],
        },
        computeCountsFn: MotorcycleListComponent.computeVehicleCounts,
        fallbackCounts: activeFilters
          ? undefined
          : () =>
              this.motorcycleService.getAll().pipe(
                map(
                  (motorcycles): FallbackCountsResult<Motorcycle> => ({
                    ...MotorcycleListComponent.computeVehicleCounts(
                      motorcycles,
                    ),
                    total: motorcycles.length,
                    items: motorcycles,
                  }),
                ),
              ),
      },
      page,
    );
  }

  private navigateToPage(page: number, queryParams?: Params): void {
    const commands = ['/vehicles/motorcycles/page', page];
    if (queryParams) {
      void this.router.navigate(commands, { queryParams });
    } else {
      void this.router.navigate(commands);
    }
  }

  private syncPriceFilters(): void {
    this.onPriceInput('minSalePrice', this.priceInputs.minSalePrice);
    this.onPriceInput('maxSalePrice', this.priceInputs.maxSalePrice);
  }

  private createFilterState(): MotorcycleSearchFilters {
    return {
      plate: '',
      brand: '',
      line: '',
      model: '',
      motorcycleType: '',
      transmission: '',
      cityRegistered: '',
      status: '',
      minYear: null,
      maxYear: null,
      minCapacity: null,
      maxCapacity: null,
      minMileage: null,
      maxMileage: null,
      minSalePrice: null,
      maxSalePrice: null,
    };
  }
}
