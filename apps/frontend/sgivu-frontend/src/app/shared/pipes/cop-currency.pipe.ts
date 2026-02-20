import { Pipe, PipeTransform } from '@angular/core';
import { formatCopCurrency } from '../utils/currency.utils';

/** Pipe para formatear valores numéricos en pesos colombianos (COP). */
@Pipe({
  name: 'copCurrency',
  standalone: true,
})
export class CopCurrencyPipe implements PipeTransform {
  transform(
    value: number | null | undefined,
    options?: Partial<Intl.NumberFormatOptions>,
  ): string {
    return formatCopCurrency(value, options);
  }
}
