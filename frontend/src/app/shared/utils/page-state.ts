import { signal } from '@angular/core';
import { Observable } from 'rxjs';
import { QueryMap } from '../../core/api/backend-api.service';

/**
 * Estado de paginación. Los campos notifican con signals porque el arranque
 * es zoneless: asignar el resultado del HTTP no refresca la plantilla solo.
 */
export class PageState<T> {
  private readonly rowsSignal = signal<T[]>([]);
  private readonly pageSignal = signal(0);
  private readonly sizeSignal = signal(20);
  private readonly sortSignal = signal('createdAt,desc');
  private readonly totalElementsSignal = signal(0);
  private readonly totalPagesSignal = signal(0);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);
  private readonly statusSignal = signal<number | null>(null);
  private generation = 0;

  get rows(): T[] { return this.rowsSignal(); }
  set rows(value: T[]) { this.rowsSignal.set(value); }
  get page(): number { return this.pageSignal(); }
  set page(value: number) { this.pageSignal.set(value); }
  get size(): number { return this.sizeSignal(); }
  set size(value: number) { this.sizeSignal.set(value); }
  get sort(): string { return this.sortSignal(); }
  set sort(value: string) { this.sortSignal.set(value); }
  get totalElements(): number { return this.totalElementsSignal(); }
  set totalElements(value: number) { this.totalElementsSignal.set(value); }
  get totalPages(): number { return this.totalPagesSignal(); }
  set totalPages(value: number) { this.totalPagesSignal.set(value); }
  get loading(): boolean { return this.loadingSignal(); }
  set loading(value: boolean) { this.loadingSignal.set(value); }
  get error(): string | null { return this.errorSignal(); }
  set error(value: string | null) { this.errorSignal.set(value); }
  get status(): number | null { return this.statusSignal(); }
  set status(value: number | null) { this.statusSignal.set(value); }

  /** Marca una consulta nueva. Las respuestas de consultas anteriores se descartan. */
  begin(): number {
    this.generation += 1;
    this.loading = true;
    this.error = null;
    this.status = null;
    return this.generation;
  }

  isCurrent(token: number): boolean {
    return token === this.generation;
  }

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

/** Ignora la respuesta si otra consulta del mismo listado se inició después. */
export function ignoreStale<T>(state: PageState<unknown>) {
  return (source: Observable<T>) =>
    new Observable<T>((subscriber) => {
      const token = state.begin();
      const subscription = source.subscribe({
        next: (value) => {
          if (state.isCurrent(token)) subscriber.next(value);
          else subscriber.complete();
        },
        error: (error: unknown) => {
          if (state.isCurrent(token)) subscriber.error(error);
          else subscriber.complete();
        },
        complete: () => subscriber.complete(),
      });
      return () => subscription.unsubscribe();
    });
}
