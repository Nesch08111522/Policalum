import { Injectable } from '@angular/core';
import { ES } from './es';

/** Catálogo i18n preparado. Hoy solo existe español; otro idioma se agrega como diccionario. */
@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly catalogs: Record<string, Record<string, string>> = { es: ES };
  locale: 'es' = 'es';

  t(key: string): string {
    return this.catalogs[this.locale]?.[key] ?? key;
  }
}
