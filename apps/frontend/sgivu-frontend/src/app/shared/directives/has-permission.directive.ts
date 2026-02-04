import {
  Directive,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  TemplateRef,
  ViewContainerRef,
  inject,
} from '@angular/core';
import { forkJoin, map, Subject, takeUntil } from 'rxjs';
import { PermissionService } from '../../features/auth/services/permission.service';

/**
 * Directiva estructural para controlar la visibilidad de elementos HTML basándose en los permisos del usuario.
 * Puede verificar un solo permiso, o un arreglo de permisos con lógica AND/OR.
 *
 * @example
 * <!-- Muestra si el usuario tiene el permiso 'user:create' -->
 * <div *appHasPermission="'user:create'">...</div>
 *
 * <!-- Muestra si el usuario tiene 'user:update' O 'user:edit' (lógica OR por defecto) -->
 * <div *appHasPermission="['user:update', 'user:edit']">...</div>
 *
 * <!-- Muestra si el usuario tiene 'user:read' Y 'report:view' (lógica AND explícita) -->
 * <div *appHasPermission="['user:read', 'report:view']; logic: 'AND'">...</div>
 */
@Directive({
  selector: '[appHasPermission]',
})
export class HasPermissionDirective implements OnChanges, OnDestroy {
  private readonly templateRef = inject(TemplateRef<void>);
  private readonly viewContainerRef = inject(ViewContainerRef);
  private readonly permissionService = inject(PermissionService);

  @Input('appHasPermission')
  permissions: string | string[] | undefined;

  @Input('appHasPermissionLogic')
  logic: 'AND' | 'OR' = 'OR';

  private hasView = false;

  private readonly destroy$ = new Subject<void>();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['permissions'] || changes['logic']) {
      this.updateView();
    }
  }

  private updateView(): void {
    this.destroy$.next();

    let permissionsToCheck: string[] = [];
    if (typeof this.permissions === 'string') {
      permissionsToCheck = [this.permissions];
    } else if (Array.isArray(this.permissions)) {
      permissionsToCheck = this.permissions;
    }

    if (permissionsToCheck.length === 0) {
      this.clearView();
      return;
    }

    const permissionChecks$ = permissionsToCheck.map((p) =>
      this.permissionService.hasPermission(p),
    );

    const finalPermission$ =
      permissionsToCheck.length > 1 && this.logic === 'AND'
        ? forkJoin(permissionChecks$).pipe(
            map((results) => results.every(Boolean)),
          )
        : forkJoin(permissionChecks$).pipe(
            map((results) => results.some(Boolean)),
          );

    finalPermission$
      .pipe(takeUntil(this.destroy$))
      .subscribe((hasPermission) => {
        if (hasPermission) {
          this.onPermissionGranted();
        } else {
          this.onPermissionDenied();
        }
      });
  }

  private onPermissionGranted(): void {
    if (!this.hasView) {
      this.viewContainerRef.createEmbeddedView(this.templateRef);
      this.hasView = true;
    }
  }

  private onPermissionDenied(): void {
    if (this.hasView) {
      this.clearView();
    }
  }

  private clearView(): void {
    this.viewContainerRef.clear();
    this.hasView = false;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
