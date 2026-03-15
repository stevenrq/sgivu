import { Injectable, inject } from '@angular/core';
import { PersonService } from '../../features/clients/services/person.service';
import { CompanyService } from '../../features/clients/services/company.service';
import { ConfirmActionService } from './confirm-action.service';

@Injectable({
  providedIn: 'root',
})
export class ClientUiHelperService {
  private readonly personService = inject(PersonService);
  private readonly companyService = inject(CompanyService);
  private readonly confirmAction = inject(ConfirmActionService);

  updatePersonStatus(
    id: number,
    nextStatus: boolean,
    onSuccess: () => void,
    personName?: string,
  ): void {
    const description = personName
      ? `el cliente ${personName}`
      : 'el cliente persona';
    const action = nextStatus ? 'activar' : 'desactivar';

    this.confirmAction.confirmAndExecute({
      title: '¿Estás seguro?',
      text: `Se ${action}á ${description}.`,
      action$: this.personService.updateStatus(id, nextStatus),
      successTitle: 'Estado actualizado exitosamente',
      errorText:
        'No se pudo actualizar el estado del cliente. Intenta nuevamente.',
      onSuccess: () => onSuccess(),
    });
  }

  updateCompanyStatus(
    id: number,
    nextStatus: boolean,
    onSuccess: () => void,
    companyName?: string,
  ): void {
    const description = companyName
      ? `la empresa ${companyName}`
      : 'la empresa';
    const action = nextStatus ? 'activar' : 'desactivar';

    this.confirmAction.confirmAndExecute({
      title: '¿Estás seguro?',
      text: `Se ${action}á ${description}.`,
      action$: this.companyService.updateStatus(id, nextStatus),
      successTitle: 'Estado actualizado exitosamente',
      errorText:
        'No se pudo actualizar el estado del cliente. Intenta nuevamente.',
      onSuccess: () => onSuccess(),
    });
  }
}

