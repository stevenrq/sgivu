import { Injectable, inject } from '@angular/core';
import { VehicleStatus } from '../../features/vehicles/models/vehicle-status.enum';
import { CarService } from '../../features/vehicles/services/car.service';
import { MotorcycleService } from '../../features/vehicles/services/motorcycle.service';
import { ConfirmActionService } from './confirm-action.service';

@Injectable({
  providedIn: 'root',
})
export class VehicleUiHelperService {
  private readonly carService = inject(CarService);
  private readonly motorcycleService = inject(MotorcycleService);
  private readonly confirmAction = inject(ConfirmActionService);

  updateCarStatus(
    id: number,
    nextStatus: VehicleStatus,
    onSuccess: () => void,
    plate?: string,
  ): void {
    const description = plate
      ? `el automóvil con placa ${plate}`
      : 'el automóvil seleccionado';

    this.confirmAction.confirmAndExecute({
      title: '¿Estás seguro?',
      text: `Se ${this.describeAction(nextStatus)}á ${description}.`,
      action$: this.carService.changeStatus(id, nextStatus),
      successTitle: 'Estado actualizado con éxito',
      errorText:
        'No se pudo actualizar el estado del vehículo. Intenta nuevamente.',
      onSuccess: () => onSuccess(),
    });
  }

  updateMotorcycleStatus(
    id: number,
    nextStatus: VehicleStatus,
    onSuccess: () => void,
    plate?: string,
  ): void {
    const description = plate
      ? `la motocicleta con placa ${plate}`
      : 'la motocicleta seleccionada';

    this.confirmAction.confirmAndExecute({
      title: '¿Estás seguro?',
      text: `Se ${this.describeAction(nextStatus)}á ${description}.`,
      action$: this.motorcycleService.changeStatus(id, nextStatus),
      successTitle: 'Estado actualizado con éxito',
      errorText:
        'No se pudo actualizar el estado del vehículo. Intenta nuevamente.',
      onSuccess: () => onSuccess(),
    });
  }

  private describeAction(status: VehicleStatus): string {
    if (status === VehicleStatus.INACTIVE) {
      return 'desactivar';
    }
    if (status === VehicleStatus.AVAILABLE) {
      return 'activar';
    }
    return 'actualizar';
  }
}

