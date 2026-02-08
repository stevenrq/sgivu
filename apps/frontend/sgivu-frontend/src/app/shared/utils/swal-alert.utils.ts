import Swal, { SweetAlertResult } from 'sweetalert2';

/**
 * Muestra una alerta de éxito con título "Operación exitosa".
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
