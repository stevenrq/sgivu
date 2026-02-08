import { DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import Swal from 'sweetalert2';
import { PurchaseSaleService } from './purchase-sale.service';

export type ExportFormat = 'pdf' | 'excel' | 'csv';
type ReportExtension = 'pdf' | 'xlsx' | 'csv';

@Injectable({ providedIn: 'root' })
export class PurchaseSaleReportService {
  private readonly purchaseSaleService = inject(PurchaseSaleService);

  readonly exportLoading = signal<Record<ExportFormat, boolean>>({
    pdf: false,
    excel: false,
    csv: false,
  });

  download(
    format: ExportFormat,
    destroyRef: DestroyRef,
    startDate?: string | null,
    endDate?: string | null,
  ): void {
    if (startDate && endDate && startDate > endDate) {
      void Swal.fire({
        icon: 'warning',
        title: 'Rango inválido',
        text: 'La fecha inicial no puede ser posterior a la fecha final.',
      });
      return;
    }

    this.setLoading(format, true);
    const start = startDate ?? undefined;
    const end = endDate ?? undefined;
    const request$ = this.getReportObservable(format, start, end);

    request$
      .pipe(
        finalize(() => this.setLoading(format, false)),
        takeUntilDestroyed(destroyRef),
      )
      .subscribe({
        next: (blob) => {
          const extension = this.getExtension(format);
          const fileName = this.buildFileName(extension, startDate, endDate);
          this.triggerDownload(blob, fileName);
          void Swal.fire({
            icon: 'success',
            title: 'Operación exitosa',
            text: `Reporte ${extension.toUpperCase()} generado correctamente.`,
            timer: 2200,
            showConfirmButton: false,
          });
        },
        error: () => {
          void Swal.fire({
            icon: 'error',
            title: 'Oops...',
            text: 'Se presentó un inconveniente al generar el reporte. Intenta nuevamente.',
          });
        },
      });
  }

  private setLoading(format: ExportFormat, loading: boolean): void {
    this.exportLoading.update((prev) => ({ ...prev, [format]: loading }));
  }

  private getReportObservable(
    format: ExportFormat,
    start?: string,
    end?: string,
  ) {
    switch (format) {
      case 'pdf':
        return this.purchaseSaleService.downloadPdf(start, end);
      case 'excel':
        return this.purchaseSaleService.downloadExcel(start, end);
      case 'csv':
      default:
        return this.purchaseSaleService.downloadCsv(start, end);
    }
  }

  private getExtension(format: ExportFormat): ReportExtension {
    if (format === 'pdf') return 'pdf';
    if (format === 'excel') return 'xlsx';
    return 'csv';
  }

  private buildFileName(
    extension: ReportExtension,
    startDate?: string | null,
    endDate?: string | null,
  ): string {
    const today = new Date().toISOString().split('T')[0];
    const rangeLabel =
      startDate || endDate
        ? `${startDate ?? 'inicio'}-a-${endDate ?? 'fin'}`
        : 'completo';
    return `reporte-compras-ventas-${rangeLabel}-${today}.${extension}`;
  }

  private triggerDownload(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }
}
