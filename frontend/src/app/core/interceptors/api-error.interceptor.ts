import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TimeoutError, throwError, timeout } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TokenService } from '../../services/token.service';
import { ApiProblem } from '../api/openapi.types';

const REQUEST_TIMEOUT_MS = 20000;

function normalize(error: HttpErrorResponse): ApiProblem {
  const body = error.error as {
    code?: string;
    error?: string;
    message?: string;
    errors?: Record<string, string>;
    fieldErrors?: Record<string, string>;
    traceId?: string;
  } | null;

  let message = body?.message ?? body?.error;
  if (!message && error.status === 0) message = 'No hay conexión con el servidor.';
  if (!message && error.status === 401) message = 'Tu sesión expiró. Vuelve a iniciar sesión.';
  if (!message && error.status === 403) message = 'No tienes permiso para esta operación.';
  if (!message && error.status === 404) message = 'No se encontró el recurso solicitado.';
  if (!message && error.status >= 500) message = 'El servidor no pudo completar la operación.';
  if (!message) message = 'La operación no pudo completarse.';

  return {
    status: error.status,
    code: body?.code ?? `HTTP_${error.status || 'NETWORK'}`,
    message,
    fieldErrors: body?.fieldErrors ?? body?.errors ?? {},
    traceId: body?.traceId,
  };
}

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const tokenService = inject(TokenService);
  return next(request).pipe(
    timeout(REQUEST_TIMEOUT_MS),
    catchError((error: unknown) => {
      if (error instanceof TimeoutError) {
        const problem: ApiProblem = {
          status: 408,
          code: 'TIMEOUT',
          message: 'La solicitud tardó demasiado. Inténtalo de nuevo.',
        };
        return throwError(() => problem);
      }
      if (!(error instanceof HttpErrorResponse)) return throwError(() => error);
      if (error.status === 401 && !request.url.endsWith('/auth/login')) {
        if (typeof sessionStorage !== 'undefined') {
          sessionStorage.setItem('auth_notice', 'Tu sesión expiró. Vuelve a iniciar sesión.');
        }
        tokenService.logout();
      }
      return throwError(() => normalize(error));
    }),
  );
};
