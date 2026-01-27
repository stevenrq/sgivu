import { Injectable, inject } from '@angular/core';
import { UserService } from '../../features/users/services/user.service';
import Swal from 'sweetalert2';

@Injectable({
  providedIn: 'root',
})
export class UserUiHelperService {
  private readonly userService = inject(UserService);

  updateStatus(id: number, status: boolean, onSuccess: () => void): void {
    Swal.fire({
      title: '¿Estás seguro?',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Sí',
      cancelButtonText: 'No',
    }).then((result) => {
      if (result.isConfirmed) {
        this.userService.updateStatus(id, status).subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: 'Estado actualizado exitosamente',
              confirmButtonColor: '#3085d6',
            });
            onSuccess();
          },
          error: () => {
            Swal.fire({
              icon: 'error',
              title: 'Error',
              text: 'No se pudo actualizar el estado del usuario. Intenta nuevamente.',
              confirmButtonColor: '#d33',
            });
          },
        });
      }
    });
  }

  delete(id: number, onSuccess: () => void): void {
    Swal.fire({
      title: '¿Estás seguro?',
      text: 'Esta acción no se puede revertir.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#3085d6',
      cancelButtonColor: '#d33',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
    }).then((result) => {
      if (result.isConfirmed) {
        this.userService.delete(id).subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: 'Usuario eliminado',
              text: 'El usuario fue eliminado exitosamente.',
              confirmButtonColor: '#3085d6',
            });
            onSuccess();
          },
          error: () => {
            Swal.fire({
              icon: 'error',
              title: 'Error',
              text: 'No se pudo eliminar el usuario. Intenta nuevamente.',
              confirmButtonColor: '#d33',
            });
          },
        });
      }
    });
  }
}
