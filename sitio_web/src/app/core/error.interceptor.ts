import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AvisoService } from './aviso.service';
import { mensajeError } from './portal.api';

/** Muestra el mensaje JSON del backend sin tumbar la vista. */
export const portalErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const avisos = inject(AvisoService);
  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 0 && error.status < 400) return throwError(() => error);
      avisos.mostrar(mensajeError(error));
      return throwError(() => error);
    }),
  );
};
