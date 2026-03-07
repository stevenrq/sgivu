import { Component, input, ChangeDetectionStrategy } from '@angular/core';

type KpiVariant =
  | 'primary'
  | 'success'
  | 'warning'
  | 'danger'
  | 'info'
  | 'dark';

@Component({
  selector: 'app-kpi-card',
  templateUrl: './kpi-card.component.html',
  styleUrl: './kpi-card.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KpiCardComponent {
  readonly label = input.required<string>();
  readonly value = input<string | number | null>(null);
  readonly description = input<string | undefined>();
  readonly icon = input('bi-info-circle');
  readonly variant = input<KpiVariant>('primary');
}
