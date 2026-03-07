import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { LoadingOverlayComponent } from '../loading-overlay/loading-overlay.component';

@Component({
  selector: 'app-data-table',
  imports: [LoadingOverlayComponent],
  templateUrl: './data-table.component.html',
  styleUrl: './data-table.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DataTableComponent {
  readonly loading = input(false);
  readonly minHeight = input(260);
  readonly responsive = input(true);
  readonly tableClass = input('table table-hover align-middle mb-0');
  readonly showHeader = input(true);
  readonly loadingLabel = input('Cargando datos...');
}
