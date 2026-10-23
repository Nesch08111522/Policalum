import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TimeoutError, throwError, timeout } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TokenService } from '../../services/token.service';
import { ToastService } from '../../shared/services/toast.service';
import { ApiProblem } from '../api/openapi.types';

const REQUEST_TIMEOUT_MS = 20000;

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
  } | null;

  let message = body?.message ?? body?.error;
  if (body?.details && error.status >= 500) message = acotar(body.details);
  if (error.status === 0) {
    message = 'No hay conexión con el servidor. Puede estar apagado o la respuesta fue bloqueada (CORS).';
  }
  if (!message && error.status === 401) message = 'Tu sesión expiró. Vuelve a iniciar sesión.';
  if (!message && error.status === 403) message = 'No tienes permiso para esta operación.';
  if (!message && error.status === 404) message = body?.message || 'No se encontró el recurso solicitado.';
  if (!message && error.status >= 500) message = 'El servidor no pudo completar la operación.';
  if (!message) message = 'La operación no pudo completarse.';

  return {
    status: error.status,
    code: body?.code ?? `HTTP_${error.status || 'NETWORK'}`,
    message,
    fieldErrors: body?.fieldErrors ?? body?.errors ?? {},
    traceId: body?.traceId,
    errores: body?.errores,
  };
}

function acotar(texto: string): string {
  const limpio = texto.replace(/\s+/g, ' ').trim();
  return limpio.length > 240 ? `${limpio.slice(0, 237)}…` : limpio;
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
      avisar(toast, problem);
      return throwError(() => problem);
    }),
  );
};
