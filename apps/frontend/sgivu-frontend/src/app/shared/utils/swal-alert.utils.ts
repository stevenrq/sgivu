import Swal, { SweetAlertResult } from 'sweetalert2';

// ── Toast mixin (standalone, no DI needed) ──────────────
const Toast = Swal.mixin({
  toast: true,
  position: 'top-end',
  showConfirmButton: false,
  timerProgressBar: true,
  didOpen: (toast) => {
    toast.onmouseenter = Swal.stopTimer;
    toast.onmouseleave = Swal.resumeTimer;
  },
  customClass: { popup: 'swal-toast-popup' },
});

/**
 * Muestra un toast de éxito con título "Operación exitosa".
 * No bloquea el flujo del usuario.
 */
export function showSuccessAlert(
  message: string,
  timer = 3000,
): Promise<SweetAlertResult> {
  return Toast.fire({
    icon: 'success',
    title: message,
    timer,
  });
}

/**
 * Muestra un toast de error con título "Ha ocurrido un error".
 * No bloquea el flujo del usuario.
 */
export function showErrorAlert(
  message: string,
  timer = 4000,
): Promise<SweetAlertResult> {
  return Toast.fire({
    icon: 'error',
    title: message,
    timer,
  });
}

/**
 * Muestra un diálogo de confirmación con botones de confirmar/cancelar.
 * Este SÍ es bloqueante porque requiere interacción del usuario.
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
 * Muestra una alerta con icono y título personalizados (modal bloqueante).
 * Usada para advertencias y errores que requieren atención del usuario.
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
 * Muestra un toast de éxito temporizado (alias de showSuccessAlert).
 */
export function showTimedSuccessAlert(
  message: string,
  timer = 3000,
): Promise<SweetAlertResult> {
  return showSuccessAlert(message, timer);
}

