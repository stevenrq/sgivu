import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { LoadingOverlayComponent } from '../loading-overlay/loading-overlay.component';
import { SkeletonLoaderComponent } from '../skeleton/skeleton-loader.component';

@Component({
  selector: 'app-data-table',
  imports: [LoadingOverlayComponent, SkeletonLoaderComponent],
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
  readonly useSkeleton = input(true);
  readonly skeletonRows = input(5);

  get skeletonItems(): number[] {
    return Array.from({ length: this.skeletonRows() }, (_, i) => i);
  }
}
