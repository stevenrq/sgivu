import {
  Component,
  DestroyRef,
  OnInit,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { User } from '../../models/user.model';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../../auth/services/auth.service';
import Swal from 'sweetalert2';
import { PermissionService } from '../../../auth/services/permission.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { UserUiHelperService } from '../../../../shared/services/user-ui-helper.service';

@Component({
  selector: 'app-user-profile',
  imports: [CommonModule, RouterLink, HasPermissionDirective],
  templateUrl: './user-profile.component.html',
  styleUrls: [
    '../../../../shared/styles/entity-detail-page.css',
    './user-profile.component.css',
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserProfileComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly permissionService = inject(PermissionService);
  private readonly router = inject(Router);
  private readonly userUiHelper = inject(UserUiHelperService);
  private readonly destroyRef = inject(DestroyRef);

  protected user: User | null = null;
  protected canEdit = false;
  protected isOwnProfile = false;
  protected canManageRolePermissions = false;
  protected userPermissionNames: Set<string> = new Set<string>();

  ngOnInit(): void {
    this.permissionService
      .getUserPermissions()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((permissions) => {
        this.userPermissionNames = permissions;
      });

    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const idString = params.get('id');
        if (idString) {
          const id = Number(idString);
          if (Number.isNaN(id)) {
            this.router.navigateByUrl('/users');
          } else {
            this.loadUserData(id);
          }
        } else {
          const user = this.authService.currentAuthenticatedUser();
          if (user) {
            this.isOwnProfile = true;
            this.loadUserData(user.id);
          }
        }
      });
  }

  private loadUserData(id: number): void {
    this.userService
      .getById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (user) => {
          this.user = user;
          this.user.roles = new Set(user.roles);
          this.permissionService
            .getUserPermissions()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((permissions) => {
              this.canEdit =
                this.isOwnProfile || permissions.has('user:update');
              this.canManageRolePermissions = permissions.has('role:update');
            });
        },
        error: () => {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No se pudo cargar el perfil del usuario.',
            confirmButtonColor: '#d33',
          });
          this.router.navigateByUrl('/users');
        },
      });
  }

  public updateStatus(id: number, status: boolean): void {
    this.userUiHelper.updateStatus(id, status, () =>
      this.loadUserData(this.user!.id),
    );
  }

  public deleteUser(id: number): void {
    this.userUiHelper.delete(id, () => this.loadUserData(this.user!.id));
  }
}
