import {
  Component,
  computed,
  input,
  ChangeDetectionStrategy,
} from '@angular/core';
import { PaginatedResponse } from '../../models/paginated-response';
import { Params, RouterLink } from '@angular/router';

@Component({
  selector: 'app-pager',
  imports: [RouterLink],
  templateUrl: './pager.component.html',
  styleUrls: ['./pager.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PagerComponent<T = unknown> {
  readonly pager = input.required<PaginatedResponse<T>>();
  readonly url = input.required<string>();
  readonly queryParams = input<Params | null>(null);

  readonly firstPageIndex = 0;

  readonly lastPageIndex = computed(() => {
    const p = this.pager();
    return p ? p.totalPages - 1 : 0;
  });

  readonly pages = computed(() => {
    const p = this.pager();
    return p ? this.generatePages(p.number, p.totalPages) : [];
  });

  public generatePages(
    currentPage: number,
    totalPages: number,
    pagesToShow = 5,
  ): number[] {
    if (pagesToShow % 2 === 0) {
      pagesToShow++;
    }

    const pages: number[] = [];

    if (totalPages <= pagesToShow) {
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i);
      }
      return pages;
    }

    const halfPagesToShow = Math.floor(pagesToShow / 2);
    let startPage: number;
    let endPage: number;
    const currentPageOneBased = currentPage + 1;

    if (currentPageOneBased <= halfPagesToShow) {
      startPage = 1;
      endPage = pagesToShow;
    } else if (currentPageOneBased + halfPagesToShow >= totalPages) {
      startPage = totalPages - pagesToShow + 1;
      endPage = totalPages;
    } else {
      startPage = currentPageOneBased - halfPagesToShow;
      endPage = currentPageOneBased + halfPagesToShow;
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }

    return pages;
  }
}
