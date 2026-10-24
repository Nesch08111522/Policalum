import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TimeoutError, throwError, timeout } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TokenService } from '../../services/token.service';
import { ToastService } from '../../shared/services/toast.service';
import { ApiProblem } from '../api/openapi.types';

const REQUEST_TIMEOUT_MS = 20000;

const TECNICO = /jdbc|sql\s*\[|psql|syntax error|no se pudo determinar el tipo|could not determine data type|exception|nullpointer|stacktrace/i;

function textoVisible(status: number, crudo: string | undefined): string {
  const limpio = (crudo ?? '').replace(/\s+/g, ' ').trim();
  if (status === 0) return 'No hay conexión con el servidor. Revisa que el backend esté encendido.';
  if (status === 404) return 'No se encontró la información solicitada.';
  if (status === 408) return 'La solicitud tardó demasiado. El servidor no respondió.';
  if (status >= 500 || TECNICO.test(limpio)) return 'No se pudieron consultar los datos. Vuelve a intentar.';
  if (status === 401) return limpio || 'Tu sesión expiró. Vuelve a iniciar sesión.';
  if (status === 403) return limpio || 'No tienes permiso para esta operación.';
  return limpio || 'La operación no pudo completarse.';
}

function normalize(error: HttpErrorResponse): ApiProblem {
  const body = error.error as {
    code?: string;
    error?: string;
    message?: string;
    details?: string;
    errors?: Record<string, string>;
    fieldErrors?: Record<string, string>;
    traceId?: string;
    errores?: Array<{ fila?: number; motivo?: string }>;
  } | string | null;
  const objeto = body && typeof body === 'object' ? body : null;
  const crudo = objeto?.message ?? objeto?.error ?? (typeof body === 'string' ? body : undefined);

  return {
    status: error.status,
    code: objeto?.code ?? `HTTP_${error.status || 'NETWORK'}`,
    message: textoVisible(error.status, crudo),
    fieldErrors: objeto?.fieldErrors ?? objeto?.errors ?? {},
    traceId: objeto?.traceId,
    errores: objeto?.errores,
  };
}

function esLogin(url: string): boolean {
  return url.includes('/auth/login');
}

/** Avisa sin cerrar sesión. El 401 de sesión vencida sigue en el bloque de logout. */
function avisar(toast: ToastService, problem: ApiProblem): void {
  const status = problem.status ?? 0;
  if (status === 0 || status === 404 || status >= 500) toast.error(problem.message || 'La operación no pudo completarse.');
}

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const tokenService = inject(TokenService);
  const toast = inject(ToastService);
  return next(request).pipe(
    timeout(REQUEST_TIMEOUT_MS),
    catchError((error: unknown) => {
      if (error instanceof TimeoutError) {
        const problem: ApiProblem = {
          status: 408,
          code: 'TIMEOUT',
          message: 'La solicitud tardó demasiado. El servidor no respondió.',
        };
        toast.error(problem.message || 'La solicitud tardó demasiado. El servidor no respondió.');
        return throwError(() => problem);
      }
      if (!(error instanceof HttpErrorResponse)) return throwError(() => error);
      const problem = normalize(error);
      if (error.status === 401 && !esLogin(request.url)) {
        if (typeof sessionStorage !== 'undefined') {
          sessionStorage.setItem('auth_notice', 'Tu sesión expiró. Vuelve a iniciar sesión.');
        }
        tokenService.logout();
        return throwError(() => problem);
      }
      if (!esLogin(request.url)) avisar(toast, problem);
      return throwError(() => problem);
    }),
  );
};
