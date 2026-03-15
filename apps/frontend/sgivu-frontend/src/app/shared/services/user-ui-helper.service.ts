import { Injectable, inject } from '@angular/core';
import { UserService } from '../../features/users/services/user.service';
import { ConfirmActionService } from './confirm-action.service';

@Injectable({
  providedIn: 'root',
})
export class UserUiHelperService {
  private readonly userService = inject(UserService);
  private readonly confirmAction = inject(ConfirmActionService);

  updateStatus(id: number, status: boolean, onSuccess: () => void): void {
    this.confirmAction.confirmAndExecute({
      title: '¿Estás seguro?',
      action$: this.userService.updateStatus(id, status),
      successTitle: 'Estado actualizado exitosamente',
      errorText:
        'No se pudo actualizar el estado del usuario. Intenta nuevamente.',
      onSuccess: () => onSuccess(),
    });
  }

  delete(id: number, onSuccess: () => void): void {
    this.confirmAction.confirmAndExecute({
      title: '¿Estás seguro?',
      text: 'Esta acción no se puede revertir.',
      confirmText: 'Sí, eliminar',
      cancelText: 'Cancelar',
      confirmButtonColor: '#3085d6',
      cancelButtonColor: '#d33',
      action$: this.userService.delete(id),
      successTitle: 'Usuario eliminado',
      errorText:
        'No se pudo eliminar el usuario. Intenta nuevamente.',
      onSuccess: () => onSuccess(),
    });
  }
}

