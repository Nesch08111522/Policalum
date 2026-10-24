import { Injectable, signal } from '@angular/core';

/** Avisa a stock y kardex que una compra, venta o devolución movió existencias. */
@Injectable({ providedIn: 'root' })
export class InventarioSyncService {
  readonly revision = signal(0);

  marcarCambio(): void {
    this.revision.update((valor) => valor + 1);
  }
}
