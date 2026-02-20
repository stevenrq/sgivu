import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { LoadingOverlayComponent } from '../loading-overlay/loading-overlay.component';

@Component({
  selector: 'app-form-shell',
  imports: [LoadingOverlayComponent],
  templateUrl: './form-shell.component.html',
  styleUrl: './form-shell.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormShellComponent {
  readonly title = input('');
  readonly subtitle = input('');
  readonly icon = input('');
  readonly loading = input(false);
  readonly pageClass = input('');
  readonly cardClass = input('');
  readonly headerClass = input('');
  readonly footerClass = input('');
  readonly titleClass = input('');
  readonly subtitleClass = input('');
  readonly bodyClass = input('');
}
