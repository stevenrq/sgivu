import {
  Directive,
  HostBinding,
  HostListener,
  Input,
  inject,
} from '@angular/core';
import { NavigationExtras, Router } from '@angular/router';

/**
 * Hace que un `<tr>` o cualquier elemento sea navegable por click.
 * Ignora clicks en elementos interactivos hijos (`a`, `button`, `input`, etc.)
 * para evitar navegación accidental al interactuar con controles dentro de la fila.
 */
@Directive({
  selector: '[appRowNavigate]',
  standalone: true,
})
export class RowNavigateDirective {
  private readonly router = inject(Router);

  @Input({ required: true }) appRowNavigate:
    | string
    | (string | number | undefined)[] = '';

  @Input() queryParams?: NavigationExtras['queryParams'];

  @Input() navigationExtras?: NavigationExtras;

  @Input() appRowNavigateDisabled = false;

  @HostBinding('class.row-navigate')
  readonly clickableClass = true;

  @HostListener('click', ['$event'])
  async handleClick(event: Event): Promise<void> {
    if (this.appRowNavigateDisabled) return;

    const target = event.target as HTMLElement | null;
    if (target?.closest('a, button, select, option, input, textarea, label')) {
      return; // Respeta interacciones internas.
    }

    event.preventDefault();
    event.stopPropagation();

    const extras: NavigationExtras = {
      queryParams: this.queryParams,
      ...this.navigationExtras,
    };

    if (typeof this.appRowNavigate === 'string') {
      await this.router.navigateByUrl(this.appRowNavigate, extras);
    } else {
      await this.router.navigate(this.appRowNavigate, extras);
    }
  }
}
