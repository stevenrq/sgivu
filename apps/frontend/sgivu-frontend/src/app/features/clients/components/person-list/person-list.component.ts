import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  Router,
  RouterLink,
  ActivatedRoute,
  ParamMap,
  Params,
} from '@angular/router';
import { combineLatest, map } from 'rxjs';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { PagerComponent } from '../../../../shared/components/pager/pager.component';
import { KpiCardComponent } from '../../../../shared/components/kpi-card/kpi-card.component';
import { DataTableComponent } from '../../../../shared/components/data-table/data-table.component';
import { RowNavigateDirective } from '../../../../shared/directives/row-navigate.directive';
import {
  PersonSearchFilters,
  PersonService,
} from '../../services/person.service';
import { Person } from '../../models/person.model.';
import { ClientUiHelperService } from '../../../../shared/services/client-ui-helper.service';
import { ListPageManager } from '../../../../shared/utils/list-page-manager';

type FilterEnabled = boolean | '' | 'true' | 'false';

@Component({
  selector: 'app-person-list',
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    HasPermissionDirective,
    PagerComponent,
    KpiCardComponent,
    DataTableComponent,
    RowNavigateDirective,
  ],
  templateUrl: './person-list.component.html',
  styleUrl: './person-list.component.css',
})
export class PersonListComponent implements OnInit {
  private readonly personService = inject(PersonService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly clientUiHelper = inject(ClientUiHelperService);
  private readonly destroyRef = inject(DestroyRef);

  readonly listManager = new ListPageManager<Person>(this.destroyRef);

  filters: PersonSearchFilters & { enabled?: FilterEnabled } =
    this.createFilterState();

  private activeFilters: PersonSearchFilters | null = null;
  private queryParams: Params | null = null;

  // ─── Propiedades del template ──────────────────────────

  readonly pagerUrl = '/clients/persons/page';
  readonly pagerLabel = 'personas';

  get activePagerQueryParams(): Params | null {
    return this.queryParams;
  }

  // ─── Lifecycle ─────────────────────────────────────────

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

        this.loadPersons(page, this.activeFilters ?? undefined);
      });
  }

  // ─── Acciones del template ─────────────────────────────

  protected applyFilters(): void {
    const filters = this.buildFilters();
    if (ListPageManager.areFiltersEmpty(filters as Record<string, unknown>)) {
      this.navigateToPage(0);
      return;
    }
    const queryParams = this.buildQueryParams(filters);
    this.navigateToPage(0, queryParams ?? undefined);
  }

  protected clearFilters(): void {
    this.filters = this.createFilterState();
    this.activeFilters = null;
    this.queryParams = null;
    this.navigateToPage(0);
  }

  protected goToPage(page: number): void {
    this.navigateToPage(page, this.queryParams ?? undefined);
  }

  protected toggleStatus(person: Person): void {
    this.clientUiHelper.updatePersonStatus(
      person.id,
      !person.enabled,
      () =>
        this.loadPersons(
          this.listManager.currentPage(),
          this.activeFilters ?? undefined,
        ),
      `${person.firstName} ${person.lastName}`.trim(),
    );
  }

  // ─── Privados ──────────────────────────────────────────

  private loadPersons(page: number, filters?: PersonSearchFilters): void {
    const activeFilters =
      filters &&
      !ListPageManager.areFiltersEmpty(filters as Record<string, unknown>)
        ? filters
        : undefined;

    this.listManager.loadPage(
      {
        fetchPager: (p) =>
          activeFilters
            ? this.personService.searchPaginated(p, activeFilters)
            : this.personService.getAllPaginated(p),
        fetchCounts: () => this.personService.getPersonCount(),
        errorMessage: 'Error al cargar las personas.',
        countKeys: {
          active: ['activeClients', 'activePersons', 'activePeople', 'active'],
          inactive: [
            'inactiveClients',
            'inactivePersons',
            'inactivePeople',
            'inactive',
          ],
        },
        computeCountsFn: ListPageManager.computeEnabledCounts,
        fallbackCounts: activeFilters
          ? undefined
          : () =>
              this.personService.getAll().pipe(
                map((persons) => ({
                  ...ListPageManager.computeEnabledCounts(persons),
                  total: persons.length,
                  items: persons,
                })),
              ),
      },
      page,
    );
  }

  private navigateToPage(page: number, queryParams?: Params): void {
    const commands = ['/clients/persons/page', page];
    if (queryParams) {
      void this.router.navigate(commands, { queryParams });
    } else {
      void this.router.navigate(commands);
    }
  }

  // ─── Filtros ───────────────────────────────────────────

  private createFilterState(): PersonSearchFilters & {
    enabled?: FilterEnabled;
  } {
    return {
      name: '',
      email: '',
      nationalId: '',
      phoneNumber: '',
      city: '',
      enabled: '',
    };
  }

  private buildFilters(): PersonSearchFilters {
    const f = this.filters;
    return {
      name: ListPageManager.normalizeFilterValue(f.name),
      email: ListPageManager.normalizeFilterValue(f.email),
      nationalId: ListPageManager.normalizeFilterValue(f.nationalId),
      phoneNumber: ListPageManager.normalizeFilterValue(f.phoneNumber),
      city: ListPageManager.normalizeFilterValue(f.city),
      enabled: ListPageManager.normalizeStatus(f.enabled),
    };
  }

  private extractFiltersFromQuery(map: ParamMap): {
    filters: PersonSearchFilters | null;
    uiState: PersonSearchFilters & { enabled?: FilterEnabled };
    queryParams: Params | null;
  } {
    const uiState = this.createFilterState();
    const filters: PersonSearchFilters = {};

    const assign = (
      paramKey: string,
      targetKey: keyof PersonSearchFilters,
    ): void => {
      const value = map.get(paramKey);
      if (value) {
        (filters as Record<string, unknown>)[targetKey] = value;
        (uiState as Record<string, unknown>)[targetKey] = value;
      }
    };

    assign('personName', 'name');
    assign('personEmail', 'email');
    assign('personNationalId', 'nationalId');
    assign('personPhone', 'phoneNumber');
    assign('personCity', 'city');

    const enabledValue = map.get('personEnabled');
    if (enabledValue !== null) {
      filters.enabled = enabledValue === 'true';
      uiState.enabled = enabledValue === 'true' ? 'true' : 'false';
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

  private buildQueryParams(filters: PersonSearchFilters): Params | null {
    const params: Params = {};
    const assign = (key: string, value: string | undefined) => {
      if (value) {
        params[key] = value;
      }
    };

    assign('personName', filters.name);
    assign('personEmail', filters.email);
    assign('personNationalId', filters.nationalId);
    assign('personPhone', filters.phoneNumber);
    assign('personCity', filters.city);

    if (filters.enabled !== undefined) {
      params['personEnabled'] = String(filters.enabled);
    }

    return Object.keys(params).length ? params : null;
  }
}
