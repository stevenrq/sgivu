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
  CompanySearchFilters,
  CompanyService,
} from '../../services/company.service';
import { Company } from '../../models/company.model';
import { ClientUiHelperService } from '../../../../shared/services/client-ui-helper.service';
import { ListPageManager } from '../../../../shared/utils/list-page-manager';

type FilterEnabled = boolean | '' | 'true' | 'false';

@Component({
  selector: 'app-company-list',
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
  templateUrl: './company-list.component.html',
  styleUrl: './company-list.component.css',
})
export class CompanyListComponent implements OnInit {
  private readonly companyService = inject(CompanyService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly clientUiHelper = inject(ClientUiHelperService);
  private readonly destroyRef = inject(DestroyRef);

  readonly listManager = new ListPageManager<Company>(this.destroyRef);

  filters: CompanySearchFilters & { enabled?: FilterEnabled } =
    this.createFilterState();

  private activeFilters: CompanySearchFilters | null = null;
  private queryParams: Params | null = null;

  // ─── Propiedades del template ──────────────────────────

  readonly pagerUrl = '/clients/companies/page';
  readonly pagerLabel = 'empresas';

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

        this.loadCompanies(page, this.activeFilters ?? undefined);
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

  protected toggleStatus(company: Company): void {
    this.clientUiHelper.updateCompanyStatus(
      company.id,
      !company.enabled,
      () =>
        this.loadCompanies(
          this.listManager.currentPage(),
          this.activeFilters ?? undefined,
        ),
      company.companyName,
    );
  }

  // ─── Privados ──────────────────────────────────────────

  private loadCompanies(page: number, filters?: CompanySearchFilters): void {
    const activeFilters =
      filters &&
      !ListPageManager.areFiltersEmpty(filters as Record<string, unknown>)
        ? filters
        : undefined;

    this.listManager.loadPage(
      {
        fetchPager: (p) =>
          activeFilters
            ? this.companyService.searchPaginated(p, activeFilters)
            : this.companyService.getAllPaginated(p),
        fetchCounts: () => this.companyService.getCompanyCount(),
        errorMessage: 'Error al cargar las empresas.',
        countKeys: {
          active: [
            'activeCompanies',
            'activeClients',
            'activeOrganizations',
            'active',
          ],
          inactive: [
            'inactiveCompanies',
            'inactiveClients',
            'inactiveOrganizations',
            'inactive',
          ],
        },
        fallbackCounts: activeFilters
          ? undefined
          : () =>
              this.companyService.getAll().pipe(
                map((companies) => ({
                  ...ListPageManager.computeCounts(companies),
                  total: companies.length,
                  items: companies,
                })),
              ),
      },
      page,
    );
  }

  private navigateToPage(page: number, queryParams?: Params): void {
    const commands = ['/clients/companies/page', page];
    if (queryParams) {
      void this.router.navigate(commands, { queryParams });
    } else {
      void this.router.navigate(commands);
    }
  }

  // ─── Filtros ───────────────────────────────────────────

  private createFilterState(): CompanySearchFilters & {
    enabled?: FilterEnabled;
  } {
    return {
      companyName: '',
      taxId: '',
      email: '',
      phoneNumber: '',
      city: '',
      enabled: '',
    };
  }

  private buildFilters(): CompanySearchFilters {
    const f = this.filters;
    return {
      companyName: ListPageManager.normalizeFilterValue(f.companyName),
      taxId: ListPageManager.normalizeFilterValue(f.taxId),
      email: ListPageManager.normalizeFilterValue(f.email),
      phoneNumber: ListPageManager.normalizeFilterValue(f.phoneNumber),
      city: ListPageManager.normalizeFilterValue(f.city),
      enabled: ListPageManager.normalizeStatus(f.enabled),
    };
  }

  private extractFiltersFromQuery(map: ParamMap): {
    filters: CompanySearchFilters | null;
    uiState: CompanySearchFilters & { enabled?: FilterEnabled };
    queryParams: Params | null;
  } {
    const uiState = this.createFilterState();
    const filters: CompanySearchFilters = {};

    const assign = (
      paramKey: string,
      targetKey: keyof CompanySearchFilters,
    ): void => {
      const value = map.get(paramKey);
      if (value) {
        (filters as Record<string, unknown>)[targetKey] = value;
        (uiState as Record<string, unknown>)[targetKey] = value;
      }
    };

    assign('companyName', 'companyName');
    assign('companyTaxId', 'taxId');
    assign('companyEmail', 'email');
    assign('companyPhone', 'phoneNumber');
    assign('companyCity', 'city');

    const enabledValue = map.get('companyEnabled');
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

  private buildQueryParams(filters: CompanySearchFilters): Params | null {
    const params: Params = {};
    const assign = (key: string, value: string | undefined) => {
      if (value) {
        params[key] = value;
      }
    };

    assign('companyName', filters.companyName);
    assign('companyTaxId', filters.taxId);
    assign('companyEmail', filters.email);
    assign('companyPhone', filters.phoneNumber);
    assign('companyCity', filters.city);

    if (filters.enabled !== undefined) {
      params['companyEnabled'] = String(filters.enabled);
    }

    return Object.keys(params).length ? params : null;
  }
}
