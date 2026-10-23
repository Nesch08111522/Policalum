import { Injectable, signal } from '@angular/core';
import * as Api from '../../core/api/openapi.types';

const TURNO_KEY = 'erp-turno-caja';

/** Turno de caja compartido entre Caja, ventas, cotizaciones y el punto de venta. */
@Injectable({ providedIn: 'root' })
export class CajaSessionService {
  readonly turno = signal<Api.TurnoCajaResponseDTO | null>(this.leer());
  /** Aumenta cuando una venta, anulación o reembolso mueve el efectivo del turno. */
  readonly actividad = signal(0);

  id(): string {
    return this.turno()?.id ?? '';
  }

  recordar(turno: Api.TurnoCajaResponseDTO | null): void {
    this.turno.set(turno?.id ? turno : null);
    if (typeof sessionStorage === 'undefined') return;
    if (!turno?.id) sessionStorage.removeItem(TURNO_KEY);
    else sessionStorage.setItem(TURNO_KEY, JSON.stringify(turno));
  }

  tocar(): void {
    this.actividad.update((valor) => valor + 1);
  }

  private leer(): Api.TurnoCajaResponseDTO | null {
    if (typeof sessionStorage === 'undefined') return null;
    const raw = sessionStorage.getItem(TURNO_KEY);
    if (!raw) return null;
    if (raw.startsWith('{')) {
      try {
        const parsed = JSON.parse(raw) as Api.TurnoCajaResponseDTO;
        return parsed?.id ? parsed : null;
      } catch {
        return null;
      }
    }
    return { id: raw, estado: 'ABIERTO' };
  }
}
