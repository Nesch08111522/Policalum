import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenService } from '../../services/token.service';

/** Adds the JWT only to API calls; public attendance endpoints remain usable. */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = inject(TokenService).getToken();
  if (!token || !request.url.includes('/api/')) return next(request);
  return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
