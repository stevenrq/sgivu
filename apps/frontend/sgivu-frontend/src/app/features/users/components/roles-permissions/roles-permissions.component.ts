import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { DataTableComponent } from '../../../../shared/components/data-table/data-table.component';

interface BootstrapModal {
  show(): void;
  hide(): void;
}

declare let bootstrap: {
  Modal: new (element: Element) => BootstrapModal;
};

interface Permission {
  key: string;
  label: string;
}

interface PermissionGroup {
  groupName: string;
  permissions: Permission[];
}

type RolePermissions = Record<string, string[]>;

interface Role {
  id: number;
  name: string;
  description: string;
  userCount: number;
  permissions: RolePermissions;
}

@Component({
  selector: 'app-roles-permissions',
  imports: [CommonModule, FormsModule, PageHeaderComponent, DataTableComponent],
  templateUrl: './roles-permissions.component.html',
  styleUrls: ['./roles-permissions.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RolesPermissionsComponent implements OnInit {
  roles: Role[] = [];
  permissionGroups: PermissionGroup[] = [];

  selectedRole: Role | null = null;
  roleToCreateOrEdit: Partial<Role> = {};
  roleToDelete: Role | null = null;

  private roleModal: BootstrapModal | null = null;
  private permissionsModal: BootstrapModal | null = null;
  private deleteModal: BootstrapModal | null = null;

  roleModalTitle = 'Crear Nuevo Rol';

  ngOnInit(): void {
    this.loadInitialData();
    this.initializeModals();
  }

  private loadInitialData(): void {
    this.roles = [
      {
        id: 1,
        name: 'Administrador',
        description:
          'Acceso total al sistema. Gestiona usuarios, roles y configuraciones.',
        userCount: 2,
        permissions: {
          users: ['read', 'create', 'update', 'delete'],
          vehicles: ['read', 'create', 'update', 'delete'],
          reports: ['read', 'create', 'update', 'delete'],
          predictions: ['read'],
        },
      },
      {
        id: 2,
        name: 'Gerente',
        description:
          'Supervisa operaciones, accede a informes y predicciones de demanda.',
        userCount: 3,
        permissions: {
          users: ['read'],
          vehicles: ['read', 'update'],
          reports: ['read', 'create'],
          predictions: ['read'],
        },
      },
      {
        id: 3,
        name: 'Vendedor',
        description:
          'Gestiona la venta de vehículos y la documentación asociada.',
        userCount: 5,
        permissions: {
          users: [],
          vehicles: ['read', 'update'],
          reports: ['read'],
          predictions: [],
        },
      },
      {
        id: 4,
        name: 'Mecánico',
        description:
          'Registra y actualiza el estado de mantenimiento de los vehículos.',
        userCount: 4,
        permissions: {
          users: [],
          vehicles: ['read', 'update'],
          reports: [],
          predictions: [],
        },
      },
    ];

    this.permissionGroups = [
      {
        groupName: 'Gestión de Usuarios',
        permissions: [
          { key: 'users:read', label: 'Ver Usuarios' },
          { key: 'users:create', label: 'Crear Usuarios' },
          { key: 'users:update', label: 'Editar Usuarios' },
          { key: 'users:delete', label: 'Eliminar Usuarios' },
        ],
      },
      {
        groupName: 'Gestión de Inventario',
        permissions: [
          { key: 'vehicles:read', label: 'Ver Vehículos' },
          { key: 'vehicles:create', label: 'Registrar Compra de Vehículo' },
          {
            key: 'vehicles:update',
            label: 'Actualizar Vehículo / Registrar Venta',
          },
          { key: 'vehicles:delete', label: 'Eliminar Vehículo del Inventario' },
        ],
      },
      {
        groupName: 'Gestión de Informes y Predicciones',
        permissions: [
          { key: 'reports:read', label: 'Ver Informes' },
          { key: 'reports:create', label: 'Generar Nuevos Informes' },
          { key: 'reports:update', label: 'Personalizar Informes' },
          { key: 'reports:delete', label: 'Eliminar Informes' },
          {
            key: 'predictions:read',
            label: 'Consultar Predicciones de Demanda',
          },
        ],
      },
    ];
  }

  private initializeModals(): void {
    const roleModalElement = document.getElementById('roleModal');
    const permissionsModalElement = document.getElementById('permissionsModal');
    const deleteConfirmationModalElement = document.getElementById(
      'deleteConfirmationModal',
    );

    if (roleModalElement) {
      this.roleModal = new bootstrap.Modal(roleModalElement);
    }
    if (permissionsModalElement) {
      this.permissionsModal = new bootstrap.Modal(permissionsModalElement);
    }
    if (deleteConfirmationModalElement) {
      this.deleteModal = new bootstrap.Modal(deleteConfirmationModalElement);
    }
  }

  openCreateRoleModal(): void {
    this.roleToCreateOrEdit = { name: '', description: '' };
    this.roleModalTitle = 'Crear Nuevo Rol';
    this.roleModal?.show();
  }

  openEditRoleModal(role: Role): void {
    this.roleToCreateOrEdit = { ...role };
    this.roleModalTitle = 'Editar Rol';
    this.roleModal?.show();
  }

  saveRole(): void {
    if (!this.roleToCreateOrEdit.name || !this.roleToCreateOrEdit.description) {
      alert('Por favor, completa todos los campos.');
      return;
    }

    if (this.roleToCreateOrEdit.id) {
      const index = this.roles.findIndex(
        (r) => r.id === this.roleToCreateOrEdit.id,
      );
      if (index !== -1) {
        this.roles[index] = this.roleToCreateOrEdit as Role;
      }
    } else {
      const newId =
        this.roles.length > 0
          ? Math.max(...this.roles.map((r) => r.id)) + 1
          : 1;
      this.roles.push({
        id: newId,
        name: this.roleToCreateOrEdit.name,
        description: this.roleToCreateOrEdit.description,
        userCount: 0,
        permissions: {},
      });
    }
    this.roleModal?.hide();
  }

  openDeleteModal(role: Role): void {
    this.roleToDelete = role;

    const modalEl = document.getElementById('deleteConfirmationModal');
    if (modalEl) {
      this.deleteModal = new bootstrap.Modal(modalEl);
      this.deleteModal?.show();
    }
  }

  confirmDelete(): void {
    if (this.roleToDelete) {
      this.roles = this.roles.filter((r) => r.id !== this.roleToDelete!.id);
      this.deleteModal?.hide();
      this.roleToDelete = null;
    }
  }

  openPermissionsModal(role: Role): void {
    this.selectedRole = role;

    const modalEl = document.getElementById('permissionsModal');
    if (modalEl) {
      this.permissionsModal = new bootstrap.Modal(modalEl);
      this.permissionsModal?.show();
    }
  }

  hasPermission(permissionKey: string): boolean {
    if (!this.selectedRole) return false;
    const [module, action] = permissionKey.split(':');
    return this.selectedRole.permissions[module]?.includes(action) || false;
  }

  togglePermission(event: Event, permissionKey: string): void {
    if (!this.selectedRole) return;
    const [module, action] = permissionKey.split(':');

    if (!this.selectedRole.permissions[module]) {
      this.selectedRole.permissions[module] = [];
    }

    const target = event.target as HTMLInputElement;
    const isChecked = target.checked;
    const permissions = this.selectedRole.permissions[module];
    const actionIndex = permissions.indexOf(action);

    if (isChecked && actionIndex === -1) {
      permissions.push(action);
    } else if (!isChecked && actionIndex !== -1) {
      permissions.splice(actionIndex, 1);
    }
  }

  savePermissions(): void {
    console.log('Permissions saved for:', this.selectedRole);
    this.permissionsModal?.hide();
  }
}
