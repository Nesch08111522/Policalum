import { Injectable, signal } from '@angular/core';

const COLLAPSED_KEY = 'erp-sidebar-collapsed';

@Injectable({ providedIn: 'root' })
export class LayoutService {
  readonly collapsed = signal(this.readCollapsed());
  readonly mobileOpen = signal(false);

  toggleCollapsed(): void {
    this.collapsed.update((value) => !value);
    if (typeof localStorage !== 'undefined') localStorage.setItem(COLLAPSED_KEY, this.collapsed() ? '1' : '0');
  }

  private readCollapsed(): boolean {
    if (typeof localStorage === 'undefined') return false;
    return localStorage.getItem(COLLAPSED_KEY) === '1';
  }

  toggleMobile(): void {
    this.mobileOpen.update((value) => !value);
  }

  closeMobile(): void {
    this.mobileOpen.set(false);
  }
}
