import { Observable } from 'rxjs';

/**
 * Configuración para el submit de un formulario CRUD.
 */
export interface SubmitConfig {
  request$: Observable<unknown>;
  successMessage: string;
  errorMessage: string;
  redirectCommand: (string | number)[];
}

/**
 * Textos de éxito y error para operaciones de creación y actualización.
 */
export interface SubmitCopy {
  createSuccess: string;
  updateSuccess: string;
  createError: string;
  updateError: string;
  redirectCommand: (string | number)[];
}

/**
 * Textos de encabezado del formulario según el modo (creación o edición).
 */
export interface ViewCopy {
  createTitle: string;
  editTitle: string;
  createSubtitle: string;
  editSubtitle: string;
}

/**
 * Compone un SubmitConfig a partir de un SubmitCopy y el modo de edición.
 */
export function composeSubmitConfig(
  copy: SubmitCopy,
  request$: Observable<unknown>,
  isEditMode: boolean,
): SubmitConfig {
  return {
    request$,
    successMessage: isEditMode ? copy.updateSuccess : copy.createSuccess,
    errorMessage: isEditMode ? copy.updateError : copy.createError,
    redirectCommand: [...copy.redirectCommand],
  };
}
