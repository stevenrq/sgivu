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
import { CarService, CarSearchFilters } from '../../services/car.service';
import { Car } from '../../models/car.model';
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

const CAR_FILTER_MAPPINGS: FilterFieldMapping[] = [
  { queryKey: 'carPlate', filterKey: 'plate', type: 'string' },
  { queryKey: 'carBrand', filterKey: 'brand', type: 'string' },
  { queryKey: 'carLine', filterKey: 'line', type: 'string' },
  { queryKey: 'carModel', filterKey: 'model', type: 'string' },
  { queryKey: 'carFuelType', filterKey: 'fuelType', type: 'string' },
  { queryKey: 'carBodyType', filterKey: 'bodyType', type: 'string' },
  { queryKey: 'carTransmission', filterKey: 'transmission', type: 'string' },
  { queryKey: 'carCity', filterKey: 'cityRegistered', type: 'string' },
  { queryKey: 'carStatus', filterKey: 'status', type: 'enum' },
  { queryKey: 'carMinYear', filterKey: 'minYear', type: 'number' },
  { queryKey: 'carMaxYear', filterKey: 'maxYear', type: 'number' },
  { queryKey: 'carMinCapacity', filterKey: 'minCapacity', type: 'number' },
  { queryKey: 'carMaxCapacity', filterKey: 'maxCapacity', type: 'number' },
  { queryKey: 'carMinMileage', filterKey: 'minMileage', type: 'number' },
  { queryKey: 'carMaxMileage', filterKey: 'maxMileage', type: 'number' },
  { queryKey: 'carMinSalePrice', filterKey: 'minSalePrice', type: 'price' },
  { queryKey: 'carMaxSalePrice', filterKey: 'maxSalePrice', type: 'price' },
];

@Component({
  selector: 'app-car-list',
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
  templateUrl: './car-list.component.html',
  styleUrl: './car-list.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CarListComponent implements OnInit {
  private readonly carService = inject(CarService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly vehicleUiHelper = inject(VehicleUiHelperService);
  private readonly destroyRef = inject(DestroyRef);

  readonly listManager = new ListPageManager<Car>(this.destroyRef);
  readonly vehicleStatuses = Object.values(VehicleStatus);
  readonly VehicleStatus = VehicleStatus;

  filters: CarSearchFilters = this.createFilterState();
  priceInputs: Record<'minSalePrice' | 'maxSalePrice', string> = {
    minSalePrice: '',
    maxSalePrice: '',
  };

  private activeFilters: CarSearchFilters | null = null;
  private queryParams: Params | null = null;
  private readonly priceDecimals = 0;

  readonly pagerUrl = '/vehicles/cars/page';

  get activePagerQueryParams(): Params | null {
    return this.queryParams;
  }

  ngOnInit(): void {
    combineLatest([this.route.paramMap, this.route.queryParamMap])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(([params, query]) => {
        const page = ListPageManager.parsePage(params.get('page'));
        const filterInfo = extractFiltersFromQuery<CarSearchFilters>(
          query,
          CAR_FILTER_MAPPINGS,
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

        this.loadCars(page, this.activeFilters ?? undefined);
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
    const qp = buildQueryParams(this.filters, CAR_FILTER_MAPPINGS);
    this.navigateToPage(0, qp ?? undefined);
  }

  protected clearFilters(): void {
    this.filters = this.createFilterState();
    this.priceInputs = { minSalePrice: '', maxSalePrice: '' };
    this.activeFilters = null;
    this.queryParams = null;
    this.navigateToPage(0);
  }

  protected changeStatus(car: Car, status: VehicleStatus): void {
    const previous = car.status;
    car.status = status;
    this.carService
      .changeStatus(car.id, status)
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
          car.status = previous;
          void Swal.fire({
            icon: 'error',
            title: 'Error al actualizar el estado',
            text: 'No se pudo actualizar el estado del vehículo.',
          });
        },
      });
  }

  protected toggleAvailability(car: Car): void {
    this.vehicleUiHelper.updateCarStatus(
      car.id,
      car.status === VehicleStatus.INACTIVE
        ? VehicleStatus.AVAILABLE
        : VehicleStatus.INACTIVE,
      () =>
        this.loadCars(
          this.listManager.currentPage(),
          this.activeFilters ?? undefined,
        ),
      car.plate,
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

  private static computeVehicleCounts(items: Car[]): {
    active: number;
    inactive: number;
  } {
    const active = items.filter(
      (item) => item.status === VehicleStatus.AVAILABLE,
    ).length;
    return { active, inactive: items.length - active };
  }

  private loadCars(page: number, filters?: CarSearchFilters): void {
    const activeFilters =
      filters &&
      !ListPageManager.areFiltersEmpty(filters as Record<string, unknown>)
        ? filters
        : undefined;

    this.listManager.loadPage(
      {
        fetchPager: (p) =>
          activeFilters
            ? this.carService.searchPaginated(p, activeFilters)
            : this.carService.getAllPaginated(p),
        fetchCounts: () => this.carService.getCounts(),
        errorMessage: 'Error al cargar los automóviles.',
        countKeys: {
          active: ['availableCars', 'available', 'availableVehicles'],
          inactive: ['unavailableCars', 'unavailable', 'unavailableVehicles'],
        },
        computeCountsFn: CarListComponent.computeVehicleCounts,
        fallbackCounts: activeFilters
          ? undefined
          : () =>
              this.carService.getAll().pipe(
                map(
                  (cars): FallbackCountsResult<Car> => ({
                    ...CarListComponent.computeVehicleCounts(cars),
                    total: cars.length,
                    items: cars,
                  }),
                ),
              ),
      },
      page,
    );
  }

  private navigateToPage(page: number, queryParams?: Params): void {
    const commands = ['/vehicles/cars/page', page];
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

  private createFilterState(): CarSearchFilters {
    return {
      plate: '',
      brand: '',
      line: '',
      model: '',
      fuelType: '',
      bodyType: '',
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
