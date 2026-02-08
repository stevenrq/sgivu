import Swal, { SweetAlertResult } from 'sweetalert2';

/**
 * Muestra una alerta de éxito con título "Operación exitosa".
 *
 * @param message - Mensaje a mostrar en la alerta.
 * @returns Promise que resuelve con el resultado de la alerta.
 */
export function showSuccessAlert(message: string): Promise<SweetAlertResult> {
  return Swal.fire({
    icon: 'success',
    title: 'Operación exitosa',
    text: message,
    confirmButtonColor: '#0d6efd',
  });
}

/**
 * Muestra una alerta de error con título "Ha ocurrido un error".
 *
 * @param message - Mensaje a mostrar en la alerta.
 * @returns Promise que resuelve con el resultado de la alerta.
 */
export function showErrorAlert(message: string): Promise<SweetAlertResult> {
  return Swal.fire({
    icon: 'error',
    title: 'Ha ocurrido un error',
    text: message,
    confirmButtonColor: '#d33',
  });
}

/**
 * Muestra un diálogo de confirmación con botones de confirmar/cancelar.
 *
 * @param options - Configuración del diálogo, incluyendo título, texto, y opciones de botones.
 * @return Promise que resuelve con el resultado de la alerta, indicando si se confirmó o canceló.
 */
export function showConfirmDialog(options: {
  title: string;
  text: string;
  confirmText?: string;
  cancelText?: string;
  icon?: 'warning' | 'info' | 'question';
}): Promise<SweetAlertResult> {
  return Swal.fire({
    icon: options.icon ?? 'warning',
    title: options.title,
    text: options.text,
    showCancelButton: true,
    confirmButtonText: options.confirmText ?? 'Sí, confirmar',
    cancelButtonText: options.cancelText ?? 'Cancelar',
  });
}

/**
 * Muestra una alerta con icono y título personalizados.
 *
 * @param options - Configuración de la alerta, incluyendo icono, título y texto.
 * @returns Promise que resuelve con el resultado de la alerta.
 */
export function showAlert(options: {
  icon: 'success' | 'error' | 'warning' | 'info';
  title: string;
  text: string;
}): Promise<SweetAlertResult> {
  return Swal.fire({
    icon: options.icon,
    title: options.title,
    text: options.text,
  });
}

/**
 * Muestra una alerta de éxito temporizada que se cierra automáticamente.
 *
 * @param message - Mensaje a mostrar en la alerta.
 * @param timer - Duración en milisegundos antes de cerrar la alerta (por defecto, 2200ms).
 * @returns Promise que resuelve con el resultado de la alerta.
 */
export function showTimedSuccessAlert(
  message: string,
  timer = 2200,
): Promise<SweetAlertResult> {
  return Swal.fire({
    icon: 'success',
    title: 'Operación exitosa',
    text: message,
    timer,
    showConfirmButton: false,
  });
}
