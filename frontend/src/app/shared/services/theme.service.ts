import { Injectable, signal } from '@angular/core';

export type ThemeName = 'light' | 'dark';

/** Noche azul de la marca por defecto. Día es el fondo blanco. */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<ThemeName>('dark');

  constructor() {
    const saved = typeof localStorage === 'undefined' ? null : localStorage.getItem('erp-theme');
    this.apply(saved === 'light' ? 'light' : 'dark');
  }

  toggle(): void {
    this.apply(this.theme() === 'dark' ? 'light' : 'dark');
  }

  apply(theme: ThemeName): void {
    this.theme.set(theme);
    document.documentElement.setAttribute('data-theme', theme);
    document.documentElement.style.colorScheme = theme === 'dark' ? 'dark' : 'light';
    if (typeof localStorage !== 'undefined') localStorage.setItem('erp-theme', theme);
  }
}
