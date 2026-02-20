import {
  Component,
  DestroyRef,
  OnInit,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ActivatedRoute,
  ParamMap,
  Params,
  Router,
  RouterLink,
} from '@angular/router';
import { combineLatest, map } from 'rxjs';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { PagerComponent } from '../../../../shared/components/pager/pager.component';
import { UserSearchFilters, UserService } from '../../services/user.service';
import { User } from '../../models/user.model';
import { UserUiHelperService } from '../../../../shared/services/user-ui-helper.service';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { KpiCardComponent } from '../../../../shared/components/kpi-card/kpi-card.component';
import { DataTableComponent } from '../../../../shared/components/data-table/data-table.component';
import { RowNavigateDirective } from '../../../../shared/directives/row-navigate.directive';
import { ListPageManager } from '../../../../shared/utils/list-page-manager';

@Component({
  selector: 'app-user-list',
  imports: [
    CommonModule,
    FormsModule,
    PagerComponent,
    RouterLink,
    HasPermissionDirective,
    PageHeaderComponent,
    KpiCardComponent,
    DataTableComponent,
    RowNavigateDirective,
  ],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserListComponent implements OnInit {
  private readonly userService = inject(UserService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly userUiHelper = inject(UserUiHelperService);
  private readonly destroyRef = inject(DestroyRef);

  readonly listManager = new ListPageManager<User>(this.destroyRef);

  readonly searchTermMaxLength = 80;
  readonly pagerUrl = '/users/page';
  readonly title = 'Usuarios registrados';
  readonly subtitle =
    'Administra las cuentas de usuario, roles y permisos del sistema.';
  readonly createLabel = 'Crear Usuario';
  readonly emptyMessage = 'No existen usuarios registrados en este momento.';

  readonly roleOptions: string[] = [
    'ADMIN',
    'MANAGER',
    'SALE',
    'PURCHASE',
    'USER',
  ];

  filters: UserSearchFilters & { enabled?: boolean | null } =
    this.createDefaultFilters();

  private activeFilters: UserSearchFilters | null = null;
  private queryParams: Params | null = null;

  get activePagerQueryParams(): Params | null {
    return this.queryParams;
  }

  ngOnInit(): void {
    combineLatest([this.route.paramMap, this.route.queryParamMap])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(([params, query]) => {
        const page = ListPageManager.parsePage(params.get('page'));
        const filterInfo = this.extractFiltersFromQuery(query);

        this.filters = filterInfo.uiState;
        this.activeFilters = filterInfo.filters;
        this.queryParams = filterInfo.queryParams;

        if (page < 0) {
          this.navigateToPage(0, filterInfo.queryParams ?? undefined);
          return;
        }

        this.loadUsers(page, this.activeFilters ?? undefined);
      });
  }

  protected search(): void {
    const filters = this.buildActiveFilters();
    if (ListPageManager.areFiltersEmpty(filters as Record<string, unknown>)) {
      this.navigateToPage(0);
      return;
    }
    const queryParams = this.buildQueryParams(filters);
    this.navigateToPage(0, queryParams ?? undefined);
  }

  protected reset(): void {
    this.filters = this.createDefaultFilters();
    this.activeFilters = null;
    this.queryParams = null;
    this.navigateToPage(0);
  }

  updateStatus(id: number, status: boolean): void {
    this.userUiHelper.updateStatus(id, status, () =>
      this.loadUsers(
        this.listManager.currentPage(),
        this.activeFilters ?? undefined,
      ),
    );
  }

  private loadUsers(page: number, filters?: UserSearchFilters): void {
    const activeFilters =
      filters &&
      !ListPageManager.areFiltersEmpty(filters as Record<string, unknown>)
        ? filters
        : undefined;

    this.listManager.loadPage(
      {
        fetchPager: (p) =>
          activeFilters
            ? this.userService.searchUsersPaginated(p, activeFilters)
            : this.userService.getAllPaginated(p),
        fetchCounts: () => this.userService.getUserCount(),
        errorMessage: 'Error al cargar usuarios.',
        countKeys: {
          active: ['activeUsers', 'active'],
          inactive: ['inactiveUsers', 'inactive'],
        },
        computeCountsFn: ListPageManager.computeEnabledCounts,
        fallbackCounts: activeFilters
          ? undefined
          : () =>
              this.userService.getAll().pipe(
                map((users) => ({
                  ...ListPageManager.computeEnabledCounts(users),
                  total: users.length,
                  items: users,
                })),
              ),
      },
      page,
    );
  }

  private navigateToPage(page: number, queryParams?: Params): void {
    const commands = ['/users/page', page];
    if (queryParams) {
      void this.router.navigate(commands, { queryParams });
    } else {
      void this.router.navigate(commands);
    }
  }

  private createDefaultFilters(): UserSearchFilters & {
    enabled?: boolean | null;
  } {
    return {
      name: '',
      username: '',
      email: '',
      role: '',
      enabled: null,
    };
  }

  private buildActiveFilters(): UserSearchFilters {
    return {
      name: this.filters.name?.trim().slice(0, this.searchTermMaxLength),
      username: this.filters.username?.trim(),
      email: this.filters.email?.trim(),
      role: this.filters.role || undefined,
      enabled: this.filters.enabled ?? undefined,
    };
  }

  private extractFiltersFromQuery(queryMap: ParamMap): {
    filters: UserSearchFilters | null;
    uiState: UserSearchFilters & { enabled?: boolean | null };
    queryParams: Params | null;
  } {
    const uiState = this.createDefaultFilters();
    const filters: UserSearchFilters = {};

    const assign = (
      paramKey: string,
      targetKey: keyof UserSearchFilters,
    ): void => {
      const value = queryMap.get(paramKey);
      if (value) {
        (filters as Record<string, unknown>)[targetKey] = value;
        (uiState as Record<string, unknown>)[targetKey] = value;
      }
    };

    assign('userName', 'name');
    assign('userUsername', 'username');
    assign('userEmail', 'email');
    assign('userRole', 'role');

    const enabledValue = queryMap.get('userEnabled');
    if (enabledValue === null) {
      uiState.enabled = null;
    } else {
      const enabled = enabledValue === 'true';
      filters.enabled = enabled;
      uiState.enabled = enabled;
    }

    const hasFilters = !ListPageManager.areFiltersEmpty(
      filters as Record<string, unknown>,
    );
    return {
      filters: hasFilters ? filters : null,
      uiState,
      queryParams: hasFilters ? this.buildQueryParams(filters) : null,
    };
  }

  private buildQueryParams(filters: UserSearchFilters): Params | null {
    const params: Params = {};

    if (filters.name) {
      params['userName'] = filters.name;
    }
    if (filters.username) {
      params['userUsername'] = filters.username;
    }
    if (filters.email) {
      params['userEmail'] = filters.email;
    }
    if (filters.role) {
      params['userRole'] = filters.role;
    }
    if (filters.enabled !== undefined) {
      params['userEnabled'] = String(filters.enabled);
    }

    return Object.keys(params).length ? params : null;
  }
}
