import { ErrorHandler, Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';

/** Lleva un fallo no controlado a la página 500, sin repetir la navegación si ya estamos ahí. */
@Injectable()
export class ErpErrorHandler implements ErrorHandler {
  private readonly router = inject(Router);
  private ultimo = 0;

  handleError(error: unknown): void {
    console.error(error);
    const status = (error as { status?: unknown } | null)?.status;
    if (typeof status === 'number') return;
    const actual = this.router.url || '';
    if (actual.startsWith('/error/')) return;
    const ahora = Date.now();
    if (ahora - this.ultimo < 2000) return;
    this.ultimo = ahora;
    void this.router.navigate(['/error', '500'], { queryParams: { desde: actual } });
  }
}
