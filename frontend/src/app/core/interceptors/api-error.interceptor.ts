import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TokenService } from '../../services/token.service';
import { ApiProblem } from '../api/openapi.types';

/**
 * Convierte cualquier HttpErrorResponse en un ApiProblem uniforme
 * que consumen las vistas del frontend.
 */
function normalize(error: HttpErrorResponse): ApiProblem {
  const body = error.error as {
    code?: string;
    error?: string;
    message?: string;
    errors?: Record<string, string>;
    fieldErrors?: Record<string, string>;
    traceId?: string;
  } | null;

  return {
    status: error.status,
    code: body?.code ?? `HTTP_${error.status}`,
    message:
      body?.message ??
      body?.error ??
      (error.status >= 500
        ? 'El servidor no pudo completar la operación.'
        : 'La operación no pudo completarse.'),
    fieldErrors: body?.fieldErrors ?? body?.errors ?? {},
    traceId: body?.traceId,
  };
}

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const tokenService = inject(TokenService);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) return throwError(() => error);
      if (error.status === 401 && !request.url.endsWith('/auth/login')) {
        tokenService.logout();
      }
      return throwError(() => normalize(error));
    }),
  );
};