import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  FormsModule,
} from '@angular/forms';
import { ChartConfiguration, ChartOptions } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { forkJoin, of, catchError, retry, timer } from 'rxjs';
import { KpiCardComponent } from '../../../../shared/components/kpi-card/kpi-card.component';
import { CarService } from '../../../vehicles/services/car.service';
import { MotorcycleService } from '../../../vehicles/services/motorcycle.service';
import { PurchaseSaleService } from '../../../purchase-sales/services/purchase-sale.service';
import { PurchaseSale } from '../../../purchase-sales/models/purchase-sale.model';
import { VehicleCount } from '../../../vehicles/interfaces/vehicle-count.interface';
import { DemandPredictionService } from '../../../../shared/services/demand-prediction.service';
import {
  DemandPredictionRequest,
  DemandMetrics,
  ModelMetadata,
  DemandPredictionResponse,
} from '../../../../shared/models/demand-prediction.model';
import { VehicleKind } from '../../../purchase-sales/models/vehicle-kind.enum';
import { VehicleOption } from '../../../purchase-sales/models/purchase-sale-reference.model';
import { DashboardStateService } from '../../services/dashboard-state.service';
import { PurchaseSaleLookupService } from '../../../purchase-sales/services/purchase-sale-lookup.service';
import {
  DEMAND_CHART_OPTIONS,
  INVENTORY_CHART_OPTIONS,
  buildForecastChartData,
  computeDemandScaleRange,
  buildInventoryChartData,
} from '../../utils/dashboard-chart.utils';
import {
  computeSalesMetrics,
  formatDashboardCurrency,
} from '../../utils/dashboard-kpi.utils';
import {
  SegmentOption,
  buildSegmentSuggestions,
} from '../../utils/segment-suggestion.utils';
import {
  normalizeVehicleType,
  describeSegment,
} from '../../utils/vehicle-kind.utils';

@Component({
  selector: 'app-dashboard',
  imports: [
    CommonModule,
    BaseChartDirective,
    KpiCardComponent,
    ReactiveFormsModule,
    FormsModule,
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements OnInit {
  private readonly carService = inject(CarService);
  private readonly motorcycleService = inject(MotorcycleService);
  private readonly purchaseSaleService = inject(PurchaseSaleService);
  private readonly demandPredictionService = inject(DemandPredictionService);
  private readonly dashboardStateService = inject(DashboardStateService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  readonly lookupService = inject(PurchaseSaleLookupService);

  private readonly latestModel = signal<ModelMetadata | null>(null);
  private readonly allContracts = signal<PurchaseSale[]>([]);

  readonly totalInventory = signal<number | null>(null);
  readonly monthlySales = signal<number | null>(null);
  private readonly monthlyRevenue = signal<number | null>(null);
  readonly vehiclesToSell = signal<number | null>(null);
  readonly salesHistoryCount = signal(0);

  readonly isLoading = signal(false);
  readonly loadError = signal<string | null>(null);

  readonly predictionForm: FormGroup = this.buildPredictionForm();
  quickVehicleTerm = '';
  readonly quickVehicleLoading = signal(false);
  private readonly defaultHorizon = 6;
  readonly activeSegmentLabel = signal<string | null>(null);
  readonly VehicleKind = VehicleKind;

  readonly demandData = signal<ChartConfiguration<'line'>['data'] | null>(null);
  readonly demandOptions = signal<ChartOptions<'line'>>(DEMAND_CHART_OPTIONS);
  readonly inventoryData = signal<
    ChartConfiguration<'doughnut'>['data'] | null
  >(null);
  readonly inventoryOptions = INVENTORY_CHART_OPTIONS;

  readonly predictionLoading = signal(false);
  readonly predictionError = signal<string | null>(null);
  readonly retrainLoading = signal(false);
  readonly retrainError = signal<string | null>(null);
  readonly retrainMessage = signal<string | null>(null);
  readonly modelVersion = signal<string | null>(null);
  readonly forecastMetrics = signal<DemandMetrics | null>(null);
  readonly segmentSuggestions = signal<SegmentOption[]>([]);
  readonly contractedVehicles = signal<
    (VehicleOption & { contractsCount?: number })[]
  >([]);
  readonly contractedVehiclesLoading = signal(false);

  readonly monthlyRevenueDisplay = computed(() => {
    const revenue = this.monthlyRevenue();
    return revenue === null ? null : formatDashboardCurrency(revenue);
  });

  readonly latestModelValue = computed(() => this.latestModel());

  ngOnInit(): void {
    this.restoreSavedPrediction();
    this.loadDashboardData();
  }

  private loadDashboardData(): void {
    this.isLoading.set(true);
    this.loadError.set(null);

    forkJoin({
      vehicleCounts: forkJoin({
        cars: this.carService.getCounts(),
        motorcycles: this.motorcycleService.getCounts(),
      }).pipe(
        retry({
          count: 3,
          delay: (_error, retryCount) => {
            const delayMs = Math.pow(2, retryCount) * 1000;
            console.warn(
              `Retrying loadVehicleCounts, attempt ${retryCount + 1}...`,
            );
            return timer(delayMs);
          },
        }),
      ),
      contracts: this.purchaseSaleService.getAll().pipe(
        retry({
          count: 3,
          delay: (_error, retryCount) => {
            const delayMs = Math.pow(2, retryCount) * 1000;
            console.warn(
              `Retrying getAll contracts, attempt ${retryCount + 1}...`,
            );
            return timer(delayMs);
          },
        }),
      ),
      latestModel: this.demandPredictionService
        .getLatestModel()
        .pipe(catchError(() => of(null))),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ vehicleCounts, contracts, latestModel }) => {
          this.applyVehicleCounts(vehicleCounts);
          this.applySalesMetrics(contracts);
          this.allContracts.set(contracts);
          this.loadVehicleOptions();
          this.latestModel.set(latestModel);
          this.segmentSuggestions.set(buildSegmentSuggestions(contracts));
          this.isLoading.set(false);
        },
        error: () => {
          this.loadError.set(
            'No se pudieron recuperar las métricas del inventario en este momento.',
          );
          this.isLoading.set(false);
        },
      });
  }

  private restoreSavedPrediction(): void {
    const savedState = this.dashboardStateService.getLastPrediction();
    if (!savedState?.payload || !savedState?.response) {
      if (savedState) {
        this.dashboardStateService.clear();
      }
      return;
    }

    const vehicleType =
      normalizeVehicleType(savedState.payload.vehicleType) ?? VehicleKind.CAR;

    this.predictionForm.patchValue({
      vehicleType,
      brand: savedState.payload.brand,
      model: savedState.payload.model,
      line: savedState.payload.line ?? '',
      horizonMonths: savedState.payload.horizonMonths ?? this.defaultHorizon,
    });

    this.quickVehicleTerm = savedState.quickVehicleTerm ?? '';
    this.activeSegmentLabel.set(savedState.activeSegmentLabel);

    const chartResult = buildForecastChartData(
      savedState.response.predictions ?? [],
      savedState.response.history ?? [],
    );
    this.demandData.set(chartResult);
    this.updateDemandScale(chartResult);

    this.modelVersion.set(
      savedState.response.modelVersion ??
        savedState.latestModel?.version ??
        null,
    );
    this.forecastMetrics.set(
      savedState.response.metrics ?? savedState.latestModel?.metrics ?? null,
    );
    const currentLatest = savedState.latestModel ?? this.latestModel();
    if (savedState.response.trainedAt && currentLatest) {
      this.latestModel.set({
        ...currentLatest,
        trainedAt: savedState.response.trainedAt,
      });
    } else if (savedState.response.trainedAt) {
      this.latestModel.set({ trainedAt: savedState.response.trainedAt });
    } else if (currentLatest) {
      this.latestModel.set(currentLatest);
    }
  }

  private applyVehicleCounts(counts: {
    cars: VehicleCount;
    motorcycles: VehicleCount;
  }): void {
    const result = buildInventoryChartData(counts);
    this.totalInventory.set(result.totalInventory);
    this.vehiclesToSell.set(result.vehiclesToSell);
    this.inventoryData.set(result.chartData);
  }

  private applySalesMetrics(contracts: PurchaseSale[]): void {
    const metrics = computeSalesMetrics(contracts);
    this.salesHistoryCount.set(metrics.salesHistoryCount);
    this.monthlyRevenue.set(metrics.monthlyRevenue);
    this.monthlySales.set(metrics.monthlySales);
  }

  private buildPredictionForm(): FormGroup {
    return this.fb.group({
      vehicleType: [VehicleKind.CAR, [Validators.required]],
      brand: ['', [Validators.required]],
      model: ['', [Validators.required]],
      line: [''],
      horizonMonths: [
        6,
        [Validators.required, Validators.min(1), Validators.max(24)],
      ],
    });
  }

  private loadVehicleOptions(): void {
    this.contractedVehiclesLoading.set(true);
    this.lookupService.loadVehiclesOnly(this.destroyRef, () => {
      this.contractedVehiclesLoading.set(false);
    });
    // La señal se actualiza reactivamente; marcar carga como completada tras un tick
    const checkInterval = setInterval(() => {
      if (this.lookupService.vehicles().length > 0) {
        this.contractedVehiclesLoading.set(false);
        clearInterval(checkInterval);
      }
    }, 100);
    // Seguridad: limpiar después de 10s
    setTimeout(() => {
      this.contractedVehiclesLoading.set(false);
      clearInterval(checkInterval);
    }, 10000);
  }

  applySegment(segment: SegmentOption, autoRun = false): void {
    this.predictionForm.patchValue({
      vehicleType: segment.vehicleType,
      brand: segment.brand,
      model: segment.model,
      line: segment.line ?? '',
    });
    if (autoRun) {
      this.onSubmitPrediction();
    }
  }

  onSubmitPrediction(): void {
    const formValue = this.predictionForm.value;
    if (!formValue.brand || !formValue.model) {
      this.predictionError.set(
        'Selecciona un vehículo desde la búsqueda rápida para generar la predicción.',
      );
      this.demandData.set(null);
      this.activeSegmentLabel.set(null);
      return;
    }

    if (this.predictionForm.invalid) {
      this.predictionForm.markAllAsTouched();
      return;
    }

    this.predictionLoading.set(true);
    this.predictionError.set(null);
    this.retrainError.set(null);
    this.retrainMessage.set(null);

    const payload: DemandPredictionRequest = {
      vehicleType: (formValue.vehicleType as VehicleKind) ?? VehicleKind.CAR,
      brand: formValue.brand,
      model: formValue.model,
      line: formValue.line || null,
      horizonMonths: Number(formValue.horizonMonths ?? 6),
      confidence: 0.95,
    };

    this.demandPredictionService
      .predict(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          const hasPredictions = response.predictions.length > 0;
          if (hasPredictions) {
            const chartResult = buildForecastChartData(
              response.predictions,
              response.history ?? [],
            );
            this.demandData.set(chartResult);
            this.updateDemandScale(chartResult);
          } else {
            this.demandData.set(null);
          }
          this.modelVersion.set(
            response.modelVersion ?? this.latestModel()?.version ?? null,
          );
          this.forecastMetrics.set(
            response.metrics ?? this.latestModel()?.metrics ?? null,
          );
          if (response.trainedAt) {
            const current = this.latestModel();
            this.latestModel.set(
              current
                ? { ...current, trainedAt: response.trainedAt }
                : { trainedAt: response.trainedAt },
            );
          }
          this.activeSegmentLabel.set(describeSegment(payload));
          if (!hasPredictions) {
            this.predictionError.set(
              'El modelo no devolvió predicciones para este segmento.',
            );
          }
          this.predictionLoading.set(false);
          this.persistPredictionState(payload, response);
        },
        error: (error) => {
          this.predictionError.set(
            error?.error?.detail ??
              'No se pudo obtener la predicción de demanda. Intenta nuevamente.',
          );
          this.predictionLoading.set(false);
          this.demandData.set(null);
          this.activeSegmentLabel.set(null);
        },
      });
  }

  private persistPredictionState(
    payload: DemandPredictionRequest,
    response: DemandPredictionResponse,
  ): void {
    if (!this.demandData()) {
      return;
    }

    const vehicleType =
      normalizeVehicleType(payload.vehicleType) ?? VehicleKind.CAR;
    const horizonMonths = payload.horizonMonths ?? this.defaultHorizon;
    const confidence = payload.confidence ?? 0.95;

    this.dashboardStateService.setLastPrediction({
      payload: {
        vehicleType,
        brand: payload.brand,
        model: payload.model,
        line: payload.line ?? null,
        horizonMonths,
        confidence,
      },
      response: {
        ...response,
        history: response.history ?? [],
      },
      activeSegmentLabel: this.activeSegmentLabel(),
      quickVehicleTerm: this.quickVehicleTerm,
      latestModel: this.latestModel(),
    });
  }

  private updateDemandScale(
    chartResult: ChartConfiguration<'line'>['data'] | null,
  ): void {
    const extended = chartResult as
      | (ChartConfiguration<'line'>['data'] & {
          _scaleValues?: (number | null)[];
        })
      | null;
    if (extended?._scaleValues) {
      this.demandOptions.set(
        computeDemandScaleRange(this.demandOptions(), extended._scaleValues),
      );
    }
  }

  filterVehicleOptions(term: string | null): void {
    const normalized = (term ?? '').trim().toUpperCase();
    if (!normalized) {
      this.contractedVehicles.set([]);
      return;
    }
    const contracts = this.allContracts();
    this.contractedVehicles.set(
      this.lookupService
        .vehicles()
        .filter((vehicle) => {
          const label = vehicle.label.toUpperCase();
          const line = (vehicle.line ?? '').toUpperCase();
          return label.includes(normalized) || line.includes(normalized);
        })
        .map((vehicle) => ({
          ...vehicle,
          contractsCount: contracts.filter(
            (c) =>
              c.vehicleId === vehicle.id || c.vehicleSummary?.id === vehicle.id,
          ).length,
        }))
        .slice(0, 8),
    );
  }

  selectQuickVehicle(
    vehicle: VehicleOption & { contractsCount?: number },
  ): void {
    const [brand, ...rest] = vehicle.label.split(' ');
    const model = rest
      .join(' ')
      .replace(/\([^)]+\)/, '')
      .trim();
    this.predictionForm.patchValue({
      vehicleType: vehicle.type,
      brand: brand ?? vehicle.label,
      model,
      line: vehicle.line ?? '',
    });
    this.contractedVehicles.set([]);
    this.quickVehicleTerm = vehicle.label;
    this.onSubmitPrediction();
  }

  onRetrain(): void {
    if (this.retrainLoading()) {
      return;
    }
    this.retrainLoading.set(true);
    this.retrainError.set(null);
    this.retrainMessage.set(null);

    this.demandPredictionService
      .retrain()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (metadata) => {
          this.latestModel.set({
            version: metadata.version,
            trainedAt: metadata.trained_at,
            metrics: metadata.metrics,
          });
          this.modelVersion.set(metadata.version);
          this.forecastMetrics.set(metadata.metrics);
          this.retrainMessage.set('Modelo reentrenado correctamente.');
          this.retrainLoading.set(false);
        },
        error: (error) => {
          this.retrainError.set(
            error?.error?.detail ??
              'No se pudo reentrenar el modelo. Intenta nuevamente.',
          );
          this.retrainLoading.set(false);
        },
      });
  }
}
