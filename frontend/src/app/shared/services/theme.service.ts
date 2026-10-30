import { Injectable, signal } from '@angular/core';

/** MarketNest es el único tema. El modo oscuro quedó retirado. */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<'marketnest'>('marketnest');

  constructor() {
    document.documentElement.setAttribute('data-theme', 'marketnest');
    document.documentElement.style.colorScheme = 'light';
    if (typeof localStorage !== 'undefined') localStorage.removeItem('erp-theme');
  }
}
