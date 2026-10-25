import { HttpBackend, HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, TimeoutError, throwError, timeout } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { LoginResponse } from '../../models/user.model';
import { TokenService } from '../../services/token.service';
import { ToastService } from '../../shared/services/toast.service';
import { ApiProblem } from '../api/openapi.types';

const REQUEST_TIMEOUT_MS = 20000;
const EXPORT_TIMEOUT_MS = 60000;

const TECNICO = /jdbc|sql\s*\[|psql|syntax error|no se pudo determinar el tipo|could not determine data type|exception|nullpointer|stacktrace/i;

let refrescoEnCurso: Observable<string> | null = null;

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
  const objeto = body && typeof body === 'object' && !(body instanceof Blob) ? body : null;
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

function esAuthPublica(url: string): boolean {
  return url.includes('/auth/login') || url.includes('/auth/refresh') || url.includes('/auth/logout');
}

function esperaLarga(url: string): boolean {
  return url.includes('/exportacion') || url.includes('/documento') || url.includes('/importar');
}

/** Avisa sin cerrar sesión. El 401 de sesión vencida sigue en el bloque de logout. Los 400 con errores de campo se muestran en el formulario. */
function avisar(toast: ToastService, problem: ApiProblem): void {
  const status = problem.status ?? 0;
  const tieneCampos = !!problem.fieldErrors && Object.keys(problem.fieldErrors).length > 0;
  if (status === 400 && tieneCampos) return;
  if (status === 0 || status === 400 || status === 403 || status === 404 || status === 409 || status >= 500) {
    toast.error(problem.message || 'La operación no pudo completarse.');
  }
}

function renovar(backend: HttpBackend, tokens: TokenService): Observable<string> {
  const refresh = tokens.getRefresh();
  if (!refresh) return throwError(() => new Error('sin refresh'));
  if (!refrescoEnCurso) {
    const http = new HttpClient(backend);
    refrescoEnCurso = http.post<LoginResponse>(`${environment.apiUrl}/api/v1/auth/refresh`, { refreshToken: refresh }).pipe(
      tap((respuesta) => {
        tokens.setToken(respuesta.token);
        tokens.setRefresh(respuesta.refreshToken);
        tokens.setUser({
          id: respuesta.usuarioId,
          nombre: respuesta.nombre,
          email: respuesta.email,
          rol: respuesta.rol,
          totpConfigurado: respuesta.totpConfigurado,
        });
      }),
      map((respuesta) => respuesta.token),
      finalize(() => { refrescoEnCurso = null; }),
      shareReplay(1),
    );
  }
  return refrescoEnCurso;
}

function cerrarSesion(tokens: TokenService): void {
  if (typeof sessionStorage !== 'undefined') {
    sessionStorage.setItem('auth_notice', 'Tu sesión expiró. Vuelve a iniciar sesión.');
  }
  tokens.logout();
}

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const tokenService = inject(TokenService);
  const toast = inject(ToastService);
  const backend = inject(HttpBackend);
  const limite = esperaLarga(request.url) ? EXPORT_TIMEOUT_MS : REQUEST_TIMEOUT_MS;
  return next(request).pipe(
    timeout(limite),
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
      const puedeRenovar = error.status === 401
        && !esAuthPublica(request.url)
        && !request.headers.has('X-Retry')
        && !!tokenService.getRefresh();
      if (puedeRenovar) {
        return renovar(backend, tokenService).pipe(
          switchMap((token) => next(request.clone({
            setHeaders: { Authorization: `Bearer ${token}`, 'X-Retry': '1' },
          })).pipe(
            timeout(limite),
            catchError((reintento: unknown) => {
              if (reintento instanceof HttpErrorResponse && reintento.status === 401) cerrarSesion(tokenService);
              const fallo = reintento instanceof HttpErrorResponse ? normalize(reintento) : reintento;
              return throwError(() => fallo);
            }),
          )),
          catchError(() => {
            cerrarSesion(tokenService);
            return throwError(() => problem);
          }),
        );
      }
      if (error.status === 401 && !esAuthPublica(request.url)) {
        cerrarSesion(tokenService);
        return throwError(() => problem);
      }
      if (!request.url.includes('/auth/login')) avisar(toast, problem);
      return throwError(() => problem);
    }),
  );
};
