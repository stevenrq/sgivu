import { AbstractControl } from '@angular/forms';

/**
 * Determina si se deben mostrar errores de validación para un control de formulario reactivo.
 *
 * @param control - Control de formulario a evaluar.
 * @returns `true` si el control es inválido y ha sido tocado o modificado.
 */
export function showControlErrors(
  control: AbstractControl | null | undefined,
): boolean {
  if (!control) {
    return false;
  }
  return !!control.invalid && (!!control.touched || !!control.dirty);
}
