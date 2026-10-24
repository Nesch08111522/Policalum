import { Injectable, signal } from '@angular/core';

export type ToastKind = 'success' | 'error' | 'warning' | 'info';

export interface ToastMessage {
  id: number;
  kind: ToastKind;
  text: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private seq = 0;
  private ultimo = '';
  private ultimoEn = 0;
  readonly toasts = signal<ToastMessage[]>([]);

  success(text: string): void { this.push('success', text); }
  error(text: string): void { this.push('error', text); }
  warning(text: string): void { this.push('warning', text); }
  info(text: string): void { this.push('info', text); }

  dismiss(id: number): void {
    this.toasts.update((items) => items.filter((item) => item.id !== id));
  }

  private push(kind: ToastKind, text: string): void {
    const clave = `${kind}|${text}`;
    const ahora = Date.now();
    if (clave === this.ultimo && ahora - this.ultimoEn < 1500) return;
    this.ultimo = clave;
    this.ultimoEn = ahora;
    const id = ++this.seq;
    this.toasts.update((items) => [...items, { id, kind, text }].slice(-4));
    setTimeout(() => this.dismiss(id), 4500);
  }
}
