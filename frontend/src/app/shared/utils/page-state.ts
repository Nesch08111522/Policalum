import { QueryMap } from '../../core/api/backend-api.service';

export class PageState<T> {
  rows: T[] = [];
  page = 0;
  size = 20;
  sort = 'createdAt,desc';
  totalElements = 0;
  totalPages = 0;
  loading = false;
  error: string | null = null;
  status: number | null = null;

  reset(): void {
    this.rows = [];
    this.totalElements = 0;
    this.totalPages = 0;
  }

  apply(result: { content?: T[]; totalElements?: number; totalPages?: number; number?: number } | null | undefined): void {
    this.rows = result?.content ?? [];
    this.totalElements = result?.totalElements ?? this.rows.length;
    this.totalPages = result?.totalPages ?? (this.rows.length ? 1 : 0);
    this.page = result?.number ?? this.page;
    this.loading = false;
    this.error = null;
    this.status = null;
  }

  fail(message: string, status: number | null): void {
    this.loading = false;
    this.rows = [];
    this.totalElements = 0;
    this.totalPages = 0;
    this.error = message;
    this.status = status;
  }

  query(extra: Record<string, string | number | boolean | null | undefined> = {}): QueryMap {
    const params: QueryMap = { page: this.page, size: this.size, sort: this.sort };
    for (const [key, value] of Object.entries(extra)) {
      if (value !== undefined && value !== null && value !== '') params[key] = value;
    }
    return params;
  }
}
