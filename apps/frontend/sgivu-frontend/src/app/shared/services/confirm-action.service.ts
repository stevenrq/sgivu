import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import Swal, { SweetAlertIcon } from 'sweetalert2';

/**
 * Configuración para confirmar y ejecutar una acción con diálogos Swal.
 */
export interface ConfirmActionConfig<T> {
  /** Título del diálogo de confirmación. */
  title?: string;
  /** Texto descriptivo del diálogo de confirmación. */
  text?: string;
  /** Icono del diálogo de confirmación. */
  icon?: SweetAlertIcon;
  /** Texto del botón de confirmación. */
  confirmText?: string;
  /** Texto del botón de cancelación. */
  cancelText?: string;
  /** Color del botón de confirmación. */
  confirmButtonColor?: string;
  /** Color del botón de cancelación. */
  cancelButtonColor?: string;
  /** Observable que ejecuta la acción HTTP. */
  action$: Observable<T>;
  /** Título del diálogo de éxito. */
  successTitle?: string;
  /** Texto del diálogo de éxito. */
  successText?: string;
  /** Título del diálogo de error. */
  errorTitle?: string;
  /** Texto del diálogo de error. */
  errorText?: string;
  /** Callback ejecutado tras el éxito. */
  onSuccess?: (result: T) => void;
  /** Callback ejecutado tras un error. */
  onError?: (error: unknown) => void;
}

/**
 * Servicio genérico para confirmar y ejecutar acciones con diálogos SweetAlert2.
 * Reemplaza la lógica repetida en UserUiHelperService, ClientUiHelperService y VehicleUiHelperService.
 */
@Injectable({
  providedIn: 'root',
})
export class ConfirmActionService {
  /**
   * Muestra un diálogo de confirmación y, si el usuario confirma,
   * ejecuta la acción observable y muestra un diálogo de éxito o error.
   */
  confirmAndExecute<T>(config: ConfirmActionConfig<T>): void {
    void Swal.fire({
      title: config.title ?? '¿Estás seguro?',
      text: config.text,
      icon: config.icon ?? 'warning',
      showCancelButton: true,
      confirmButtonColor: config.confirmButtonColor ?? '#d33',
      cancelButtonColor: config.cancelButtonColor ?? '#3085d6',
      confirmButtonText: config.confirmText ?? 'Sí',
      cancelButtonText: config.cancelText ?? 'No',
    }).then((result) => {
      if (!result.isConfirmed) {
        return;
      }

      config.action$.subscribe({
        next: (value) => {
          void Swal.fire({
            icon: 'success',
            title: config.successTitle ?? 'Operación exitosa',
            text: config.successText,
            confirmButtonColor: '#3085d6',
          });
          config.onSuccess?.(value);
        },
        error: (err) => {
          void Swal.fire({
            icon: 'error',
            title: config.errorTitle ?? 'Error',
            text:
              config.errorText ??
              'No se pudo completar la operación. Intenta nuevamente.',
            confirmButtonColor: '#d33',
          });
          config.onError?.(err);
        },
      });
    });
  }
}
