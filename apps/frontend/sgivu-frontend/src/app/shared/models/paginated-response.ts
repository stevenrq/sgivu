/**
 * Representa una respuesta paginada genérica.
 */
export class PaginatedResponse<T> {
  content!: T[];
  pageable!: Pageable;
  last!: boolean;
  totalPages!: number;
  totalElements!: number;
  size!: number;
  number!: number;
  sort!: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  first!: boolean;
  numberOfElements!: number;
  empty!: boolean;
}

/**
 * Representa la información de paginación y ordenamiento.
 */
class Pageable {
  pageNumber!: number;
  pageSize!: number;
  sort!: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  offset!: number;
  paged!: boolean;
  unpaged!: boolean;
}
