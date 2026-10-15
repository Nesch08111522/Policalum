import { registerLocaleData } from '@angular/common';
import localeEsPe from '@angular/common/locales/es-PE';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, LOCALE_ID, provideZoneChangeDetection } from '@angular/core';
import { PreloadAllModules, provideRouter, TitleStrategy, withPreloading } from '@angular/router';
import { routes } from './app.routes';
import { ErpTitleStrategy } from './core/erp-title.strategy';
import { apiErrorInterceptor } from './core/interceptors/api-error.interceptor';
import { authInterceptor } from './core/interceptors/auth.interceptor';

registerLocaleData(localeEsPe);

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withPreloading(PreloadAllModules)),
    provideHttpClient(withInterceptors([authInterceptor, apiErrorInterceptor])),
    { provide: LOCALE_ID, useValue: 'es-PE' },
    { provide: TitleStrategy, useClass: ErpTitleStrategy },
  ],
};
