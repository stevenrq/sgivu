import { Component, input, ChangeDetectionStrategy } from '@angular/core';

type SkeletonVariant = 'text' | 'card' | 'table-row' | 'chart' | 'circle';

/**
 * Componente de skeleton loader reutilizable.
 *
 * Muestra un placeholder animado con efecto shimmer que imita
 * la forma del contenido real mientras se carga.
 *
 * Variantes disponibles:
 * - `text`: Líneas de texto (configurable con `count`)
 * - `card`: Una tarjeta completa (icono + título + valor)
 * - `table-row`: Filas de tabla (configurable con `count`)
 * - `chart`: Placeholder para un gráfico
 * - `circle`: Círculo (avatar, ícono)
 */
@Component({
  selector: 'app-skeleton-loader',
  templateUrl: './skeleton-loader.component.html',
  styleUrl: './skeleton-loader.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SkeletonLoaderComponent {
  readonly variant = input<SkeletonVariant>('text');
  readonly count = input(1);
  readonly width = input<string | undefined>(undefined);
  readonly height = input<string | undefined>(undefined);

  get items(): number[] {
    return Array.from({ length: this.count() }, (_, i) => i);
  }
}
