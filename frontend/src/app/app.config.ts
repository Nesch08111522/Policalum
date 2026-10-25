import { registerLocaleData } from '@angular/common';
import localeEsPe from '@angular/common/locales/es-PE';
import { provideHttpClient, withInterceptors, withXhr } from '@angular/common/http';
import { ApplicationConfig, ErrorHandler, LOCALE_ID, provideZoneChangeDetection } from '@angular/core';
import { PreloadAllModules, provideRouter, TitleStrategy, withPreloading } from '@angular/router';
import { routes } from './app.routes';
import { ErpErrorHandler } from './core/erp-error.handler';
import { ErpTitleStrategy } from './core/erp-title.strategy';
import { apiErrorInterceptor } from './core/interceptors/api-error.interceptor';
import { authInterceptor } from './core/interceptors/auth.interceptor';

registerLocaleData(localeEsPe);

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withPreloading(PreloadAllModules)),
    // Angular 22 usa fetch por defecto y entrega la respuesta fuera de NgZone,
    // así que la vista no se entera. XHR queda dentro de la zona y refresca la pantalla.
    provideHttpClient(withXhr(), withInterceptors([authInterceptor, apiErrorInterceptor])),
    { provide: LOCALE_ID, useValue: 'es-PE' },
    { provide: TitleStrategy, useClass: ErpTitleStrategy },
    { provide: ErrorHandler, useClass: ErpErrorHandler },
  ],
};
