import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  Router,
  RouterLink,
  ActivatedRoute,
  ParamMap,
  Params,
} from '@angular/router';
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

  // ─── Propiedades del template ──────────────────────────

  readonly pagerUrl = '/vehicles/cars/page';

  get activePagerQueryParams(): Params | null {
    return this.queryParams;
  }

  // ─── Lifecycle ─────────────────────────────────────────

  ngOnInit(): void {
    combineLatest([this.route.paramMap, this.route.queryParamMap])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(([params, query]) => {
        const page = ListPageManager.parsePage(params.get('page'));
        const filterInfo = this.extractFiltersFromQuery(query);

        this.filters = filterInfo.uiState;
        this.priceInputs = filterInfo.priceInputs;
        this.activeFilters = filterInfo.filters;
        this.queryParams = filterInfo.queryParams;

        if (page < 0) {
          this.navigateToPage(0, filterInfo.queryParams ?? undefined);
          return;
        }

        this.loadCars(page, this.activeFilters ?? undefined);
      });
  }

  // ─── Acciones del template ─────────────────────────────

  protected applyFilters(): void {
    this.syncPriceFilters();
    if (
      ListPageManager.areFiltersEmpty(this.filters as Record<string, unknown>)
    ) {
      this.clearFilters();
      return;
    }
    const queryParams = this.buildQueryParams(this.filters);
    this.navigateToPage(0, queryParams ?? undefined);
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

  // ─── Privados ──────────────────────────────────────────

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

  // ─── Filtros ───────────────────────────────────────────

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

  private extractFiltersFromQuery(queryMap: ParamMap): {
    filters: CarSearchFilters | null;
    uiState: CarSearchFilters;
    priceInputs: Record<'minSalePrice' | 'maxSalePrice', string>;
    queryParams: Params | null;
  } {
    const uiState = this.createFilterState();
    const priceInputs: Record<'minSalePrice' | 'maxSalePrice', string> = {
      minSalePrice: '',
      maxSalePrice: '',
    };
    const filters: CarSearchFilters = {};

    const assignText = (
      paramKey: string,
      setter: (value: string) => void,
    ): void => {
      const value = queryMap.get(paramKey);
      if (value) {
        setter(value);
      }
    };

    assignText('carPlate', (v) => {
      filters.plate = v;
      uiState.plate = v;
    });
    assignText('carBrand', (v) => {
      filters.brand = v;
      uiState.brand = v;
    });
    assignText('carLine', (v) => {
      filters.line = v;
      uiState.line = v;
    });
    assignText('carModel', (v) => {
      filters.model = v;
      uiState.model = v;
    });
    assignText('carFuelType', (v) => {
      filters.fuelType = v;
      uiState.fuelType = v;
    });
    assignText('carBodyType', (v) => {
      filters.bodyType = v;
      uiState.bodyType = v;
    });
    assignText('carTransmission', (v) => {
      filters.transmission = v;
      uiState.transmission = v;
    });
    assignText('carCity', (v) => {
      filters.cityRegistered = v;
      uiState.cityRegistered = v;
    });

    const statusValue = queryMap.get('carStatus');
    if (statusValue) {
      filters.status = statusValue as VehicleStatus;
      uiState.status = statusValue as VehicleStatus;
    }

    const minYear = this.toNumber(queryMap.get('carMinYear'));
    if (minYear !== null) {
      filters.minYear = minYear;
      uiState.minYear = minYear;
    }
    const maxYear = this.toNumber(queryMap.get('carMaxYear'));
    if (maxYear !== null) {
      filters.maxYear = maxYear;
      uiState.maxYear = maxYear;
    }

    const minCapacity = this.toNumber(queryMap.get('carMinCapacity'));
    if (minCapacity !== null) {
      filters.minCapacity = minCapacity;
      uiState.minCapacity = minCapacity;
    }
    const maxCapacity = this.toNumber(queryMap.get('carMaxCapacity'));
    if (maxCapacity !== null) {
      filters.maxCapacity = maxCapacity;
      uiState.maxCapacity = maxCapacity;
    }

    const minMileage = this.toNumber(queryMap.get('carMinMileage'));
    if (minMileage !== null) {
      filters.minMileage = minMileage;
      uiState.minMileage = minMileage;
    }
    const maxMileage = this.toNumber(queryMap.get('carMaxMileage'));
    if (maxMileage !== null) {
      filters.maxMileage = maxMileage;
      uiState.maxMileage = maxMileage;
    }

    const minSalePrice = this.toNumber(queryMap.get('carMinSalePrice'));
    if (minSalePrice == null) {
      priceInputs.minSalePrice = '';
    } else {
      filters.minSalePrice = minSalePrice;
      uiState.minSalePrice = minSalePrice;
      priceInputs.minSalePrice = String(minSalePrice);
    }

    const maxSalePrice = this.toNumber(queryMap.get('carMaxSalePrice'));
    if (maxSalePrice == null) {
      priceInputs.maxSalePrice = '';
    } else {
      filters.maxSalePrice = maxSalePrice;
      uiState.maxSalePrice = maxSalePrice;
      priceInputs.maxSalePrice = String(maxSalePrice);
    }

    const hasFilters = !ListPageManager.areFiltersEmpty(
      filters as Record<string, unknown>,
    );

    return {
      filters: hasFilters ? filters : null,
      uiState,
      priceInputs,
      queryParams: hasFilters ? this.buildQueryParams(filters) : null,
    };
  }

  private buildQueryParams(filters: CarSearchFilters): Params | null {
    const params: Params = {};

    const assignString = (key: string, value?: string | null) => {
      if (value && value.trim().length > 0) {
        params[key] = value;
      }
    };
    const assignNumber = (key: string, value?: number | null) => {
      if (value !== undefined && value !== null) {
        params[key] = String(value);
      }
    };

    assignString('carPlate', filters.plate);
    assignString('carBrand', filters.brand);
    assignString('carLine', filters.line);
    assignString('carModel', filters.model);
    assignString('carFuelType', filters.fuelType);
    assignString('carBodyType', filters.bodyType);
    assignString('carTransmission', filters.transmission);
    assignString('carCity', filters.cityRegistered);

    if (filters.status) {
      params['carStatus'] = filters.status;
    }

    assignNumber('carMinYear', filters.minYear);
    assignNumber('carMaxYear', filters.maxYear);
    assignNumber('carMinCapacity', filters.minCapacity);
    assignNumber('carMaxCapacity', filters.maxCapacity);
    assignNumber('carMinMileage', filters.minMileage);
    assignNumber('carMaxMileage', filters.maxMileage);
    assignNumber('carMinSalePrice', filters.minSalePrice);
    assignNumber('carMaxSalePrice', filters.maxSalePrice);

    return Object.keys(params).length ? params : null;
  }
}
