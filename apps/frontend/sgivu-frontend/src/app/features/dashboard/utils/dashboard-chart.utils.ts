import { ChartConfiguration, ChartOptions } from 'chart.js';
import { DemandPredictionPoint } from '../../../shared/models/demand-prediction.model';
import { VehicleCount } from '../../vehicles/interfaces/vehicle-count.interface';
import {
  formatMonthKey,
  formatMonthLabel,
  parseMonth,
  parseMonthKey,
} from './dashboard-date.utils';

/** Opciones base del gráfico de líneas de demanda. Separadas del componente para reutilización y testing. */
export const DEMAND_CHART_OPTIONS: ChartOptions<'line'> = {
  responsive: true,
  maintainAspectRatio: false,
  interaction: {
    intersect: false,
    mode: 'index',
  },
  scales: {
    y: {
      beginAtZero: false,
      grid: { color: 'rgba(0,0,0,0.1)', lineWidth: 0.5 },
      ticks: {
        color: '#6c757d',
        callback: (value) =>
          Number(value).toLocaleString('es-CO', {
            minimumFractionDigits: 0,
            maximumFractionDigits: 3,
          }),
        stepSize: 1,
      },
      title: {
        display: true,
        text: 'Unidades',
        color: '#495057',
      },
    },
    x: {
      grid: { display: false },
      ticks: {
        color: '#6c757d',
        maxRotation: 45,
        minRotation: 45,
      },
      title: {
        display: true,
        text: 'Mes',
        color: '#495057',
      },
    },
  },
  plugins: {
    legend: {
      labels: {
        color: '#6c757d',
        filter: (legendItem) => legendItem.text !== 'IC 95% base',
      },
    },
    tooltip: {
      callbacks: {
        label: (context) => {
          const label = context.dataset.label || 'Valor';
          const value =
            typeof context.parsed.y === 'number'
              ? context.parsed.y.toLocaleString('es-CO', {
                  minimumFractionDigits: 0,
                  maximumFractionDigits: 3,
                })
              : context.parsed.y;
          return `${label}: ${value} unidades`;
        },
      },
    },
  },
};

export const INVENTORY_CHART_OPTIONS: ChartOptions<'doughnut'> = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'bottom',
      labels: { color: '#6c757d' },
    },
  },
};

/**
 * Construye los datasets del gráfico de pronóstico uniendo historia y predicciones
 * en un eje temporal continuo. Los meses se normalizan a una key común (`YYYY-MM`)
 * para alinear ambas series aunque tengan formatos de fecha distintos.
 *
 * @param predictions Puntos de predicción de demanda futura.
 * @param history Puntos de demanda histórica.
 * @returns Datos formateados para el gráfico de líneas de demanda.
 */
export function buildForecastChartData(
  predictions: DemandPredictionPoint[],
  history: { month: string; salesCount: number }[],
): ChartConfiguration<'line'>['data'] {
  if (predictions.length === 0) {
    return { labels: [], datasets: [] };
  }

  const predictionMap = new Map(
    predictions.map((point) => [
      formatMonthKey(parseMonth(point.month)),
      {
        demand: point.demand,
        lowerCi: point.lowerCi,
        upperCi: point.upperCi,
      },
    ]),
  );

  const historyMap = new Map(
    history.map((point) => [
      formatMonthKey(parseMonth(point.month)),
      point.salesCount,
    ]),
  );

  const unionKeys = Array.from(
    new Set([
      ...Array.from(historyMap.keys()),
      ...Array.from(predictionMap.keys()),
    ]),
  ).sort((a, b) => parseMonthKey(a).getTime() - parseMonthKey(b).getTime());

  const labels = unionKeys.map((key) => formatMonthLabel(parseMonthKey(key)));

  const demandValues = unionKeys.map(
    (key) => predictionMap.get(key)?.demand ?? null,
  );
  const historicalValues = unionKeys.map((key) => historyMap.get(key) ?? null);
  const lowerBand = unionKeys.map(
    (key) => predictionMap.get(key)?.lowerCi ?? null,
  );
  const upperBand = unionKeys.map(
    (key) => predictionMap.get(key)?.upperCi ?? null,
  );

  return {
    labels,
    datasets: [
      {
        label: 'Demanda Predicha',
        data: demandValues,
        borderColor: '#0d6efd',
        backgroundColor: 'rgba(13,110,253,0.2)',
        fill: false,
        tension: 0,
        borderWidth: 2,
        pointBackgroundColor: '#0d6efd',
        spanGaps: true,
        order: 1,
      },
      {
        label: 'Ventas Históricas',
        data: historicalValues,
        borderColor: '#6c757d',
        backgroundColor: '#6c757d',
        fill: false,
        tension: 0,
        borderWidth: 2,
        pointRadius: 5,
        pointHoverRadius: 6,
        pointBackgroundColor: '#6c757d',
        spanGaps: true,
        order: 0,
      },
    ],
    _scaleValues: [
      ...historicalValues,
      ...lowerBand,
      ...upperBand,
      ...demandValues,
    ],
  } as ChartConfiguration<'line'>['data'] & {
    _scaleValues: (number | null)[];
  };
}

/**
 * Calcula el rango del eje Y a partir de todos los valores (historia + predicción + intervalos de confianza)
 * para que la escala se ajuste automáticamente al rango real de los datos.
 *
 * @param baseOptions Opciones base del gráfico.
 * @param values Valores numéricos a considerar para calcular el rango.
 * @returns Nuevas opciones del gráfico con el rango ajustado.
 */
export function computeDemandScaleRange(
  baseOptions: ChartOptions<'line'>,
  values: (number | null)[],
): ChartOptions<'line'> {
  const numericValues = values.filter(
    (value): value is number => typeof value === 'number',
  );
  if (numericValues.length === 0) {
    return baseOptions;
  }

  const minValue = Math.min(...numericValues);
  const maxValue = Math.max(...numericValues);
  const suggestedMin = Math.min(0, Math.floor(minValue));
  const suggestedMax = Math.max(1, maxValue) * 1.2;
  const currentScales = baseOptions.scales ?? {};
  const currentY = baseOptions.scales?.['y'] ?? {};

  return {
    ...baseOptions,
    scales: {
      ...currentScales,
      y: {
        ...currentY,
        suggestedMin,
        suggestedMax,
        beginAtZero: true,
      },
    },
  };
}

/** Desglose de inventario para el widget del dashboard. */
export interface InventoryBreakdown {
  totalInventory: number;
  vehiclesToSell: number;
  chartData: ChartConfiguration<'doughnut'>['data'];
}

/** Construye los datos del gráfico de rosquilla de inventario a partir de conteos de carros y motos.
 *
 * @param counts Conteos de vehículos por tipo (total y disponibles).
 * @returns Datos formateados para el gráfico de rosquilla de inventario.
 */
export function buildInventoryChartData(counts: {
  cars: VehicleCount;
  motorcycles: VehicleCount;
}): InventoryBreakdown {
  const {
    cars: { total: totalCars, available: availableCars },
    motorcycles: { total: totalMotorcycles, available: availableMotorcycles },
  } = counts;

  const breakdown = [
    { label: 'Automóviles', value: totalCars, color: '#0d6efd' },
    { label: 'Motocicletas', value: totalMotorcycles, color: '#ffc107' },
  ].filter((item) => item.value > 0);

  const labels =
    breakdown.length > 0 ? breakdown.map((item) => item.label) : ['Sin datos'];
  const data = breakdown.length > 0 ? breakdown.map((item) => item.value) : [1];
  const backgroundColor =
    breakdown.length > 0 ? breakdown.map((item) => item.color) : ['#e9ecef'];

  return {
    totalInventory: totalCars + totalMotorcycles,
    vehiclesToSell: availableCars + availableMotorcycles,
    chartData: {
      labels,
      datasets: [
        {
          label: 'Inventario por tipo',
          data,
          backgroundColor,
          borderColor: '#ffffff',
          borderWidth: 2,
          hoverOffset: 4,
        },
      ],
    },
  };
}
