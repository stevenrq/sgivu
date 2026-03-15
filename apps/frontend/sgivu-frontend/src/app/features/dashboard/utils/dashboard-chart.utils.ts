import { ChartConfiguration, ChartOptions } from 'chart.js';
import { DemandPredictionPoint } from '../../../shared/models/demand-prediction.model';
import { VehicleCount } from '../../vehicles/interfaces/vehicle-count.interface';
import {
  formatMonthKey,
  formatMonthLabel,
  parseMonth,
  parseMonthKey,
} from './dashboard-date.utils';

/** Paleta de colores modernos para gráficos. */
export const CHART_PALETTE = {
  primary: '#6366f1',
  primaryLight: 'rgba(99, 102, 241, 0.15)',
  primaryMedium: 'rgba(99, 102, 241, 0.35)',
  secondary: '#8b5cf6',
  secondaryLight: 'rgba(139, 92, 246, 0.12)',
  history: '#64748b',
  historyDot: '#475569',
  ciBand: 'rgba(99, 102, 241, 0.08)',
  ciBorder: 'rgba(99, 102, 241, 0.25)',
  doughnut: ['#6366f1', '#f59e0b', '#10b981', '#f43f5e'],
  doughnutHover: ['#4f46e5', '#d97706', '#059669', '#e11d48'],
} as const;

/** Opciones base del gráfico de líneas de demanda. */
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
      grid: { color: 'rgba(100,116,139,0.1)', lineWidth: 0.5 },
      ticks: {
        color: '#94a3b8',
        font: { weight: 'bold' },
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
        color: '#64748b',
        font: { weight: 'bold' },
      },
    },
    x: {
      grid: { display: false },
      ticks: {
        color: '#94a3b8',
        font: { weight: 'bold' },
        maxRotation: 45,
        minRotation: 45,
      },
      title: {
        display: true,
        text: 'Mes',
        color: '#64748b',
        font: { weight: 'bold' },
      },
    },
  },
  plugins: {
    legend: {
      labels: {
        color: '#64748b',
        usePointStyle: true,
        pointStyle: 'circle',
        padding: 16,
        font: { weight: 'bold' },
        filter: (legendItem) =>
          legendItem.text !== 'IC 95% base' &&
          legendItem.text !== 'IC 95% superior',
      },
    },
    tooltip: {
      backgroundColor: 'rgba(15, 23, 42, 0.9)',
      titleColor: '#f1f5f9',
      bodyColor: '#cbd5e1',
      borderColor: 'rgba(99, 102, 241, 0.3)',
      borderWidth: 1,
      cornerRadius: 10,
      padding: 12,
      bodySpacing: 6,
      usePointStyle: true,
      callbacks: {
        label: (context) => {
          const label = context.dataset.label || 'Valor';
          if (label === 'IC 95% base' || label === 'IC 95% superior') {
            return '';
          }
          const value =
            typeof context.parsed.y === 'number'
              ? context.parsed.y.toLocaleString('es-CO', {
                  minimumFractionDigits: 0,
                  maximumFractionDigits: 3,
                })
              : context.parsed.y;
          return ` ${label}: ${value} unidades`;
        },
      },
    },
  },
};

export const INVENTORY_CHART_OPTIONS: ChartOptions<'doughnut'> = {
  responsive: true,
  maintainAspectRatio: false,
  cutout: '65%',
  plugins: {
    legend: {
      position: 'bottom',
      labels: {
        color: '#64748b',
        usePointStyle: true,
        pointStyle: 'circle',
        padding: 16,
        font: { weight: 'bold' },
      },
    },
    tooltip: {
      backgroundColor: 'rgba(15, 23, 42, 0.9)',
      titleColor: '#f1f5f9',
      bodyColor: '#cbd5e1',
      borderColor: 'rgba(99, 102, 241, 0.3)',
      borderWidth: 1,
      cornerRadius: 10,
      padding: 12,
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
        label: 'IC 95% superior',
        data: upperBand,
        borderColor: CHART_PALETTE.ciBorder,
        backgroundColor: 'transparent',
        fill: false,
        tension: 0.3,
        borderWidth: 0,
        pointRadius: 0,
        pointHoverRadius: 0,
        spanGaps: true,
        order: 3,
      },
      {
        label: 'IC 95% base',
        data: lowerBand,
        borderColor: CHART_PALETTE.ciBorder,
        backgroundColor: CHART_PALETTE.ciBand,
        fill: '-1',
        tension: 0.3,
        borderWidth: 0,
        pointRadius: 0,
        pointHoverRadius: 0,
        spanGaps: true,
        order: 3,
      },
      {
        label: 'Demanda Predicha',
        data: demandValues,
        borderColor: CHART_PALETTE.primary,
        backgroundColor: CHART_PALETTE.primaryLight,
        fill: false,
        tension: 0.3,
        borderWidth: 2.5,
        pointBackgroundColor: CHART_PALETTE.primary,
        pointBorderColor: '#ffffff',
        pointBorderWidth: 2,
        pointRadius: 4,
        pointHoverRadius: 7,
        spanGaps: true,
        order: 1,
      },
      {
        label: 'Ventas Históricas',
        data: historicalValues,
        borderColor: CHART_PALETTE.history,
        backgroundColor: CHART_PALETTE.historyDot,
        fill: false,
        tension: 0.3,
        borderWidth: 2,
        borderDash: [6, 4],
        pointRadius: 5,
        pointHoverRadius: 7,
        pointBackgroundColor: CHART_PALETTE.historyDot,
        pointBorderColor: '#ffffff',
        pointBorderWidth: 2,
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
    { label: 'Automóviles', value: totalCars, color: CHART_PALETTE.doughnut[0], hoverColor: CHART_PALETTE.doughnutHover[0] },
    { label: 'Motocicletas', value: totalMotorcycles, color: CHART_PALETTE.doughnut[1], hoverColor: CHART_PALETTE.doughnutHover[1] },
  ].filter((item) => item.value > 0);

  const labels =
    breakdown.length > 0 ? breakdown.map((item) => item.label) : ['Sin datos'];
  const data = breakdown.length > 0 ? breakdown.map((item) => item.value) : [1];
  const backgroundColor =
    breakdown.length > 0 ? breakdown.map((item) => item.color) : ['#e2e8f0'];
  const hoverBackgroundColor =
    breakdown.length > 0 ? breakdown.map((item) => item.hoverColor) : ['#cbd5e1'];

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
          hoverBackgroundColor,
          borderColor: 'rgba(255,255,255,0.8)',
          borderWidth: 3,
          hoverOffset: 8,
        },
      ],
    },
  };
}
