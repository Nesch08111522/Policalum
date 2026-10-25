import { Injectable, signal } from '@angular/core';

const CLAVE = 'erp-sede';

/** Sede elegida en la barra superior. Vacío significa todas las sedes. Cada pestaña guarda la suya. */
@Injectable({ providedIn: 'root' })
export class SedeFiltroService {
  readonly sedeId = signal('');
  readonly nombre = signal('Sedes');

  constructor() {
    if (typeof sessionStorage === 'undefined') return;
    const raw = sessionStorage.getItem(CLAVE);
    if (!raw) return;
    try {
      const data = JSON.parse(raw) as { id?: string; nombre?: string };
      this.sedeId.set(data.id ?? '');
      this.nombre.set(data.id ? (data.nombre || 'Sede') : 'Sedes');
    } catch {
      sessionStorage.removeItem(CLAVE);
    }
  }

  elegir(id: string, nombre: string): void {
    this.sedeId.set(id);
    this.nombre.set(id ? nombre : 'Sedes');
    if (typeof sessionStorage === 'undefined') return;
    sessionStorage.setItem(CLAVE, JSON.stringify({ id, nombre: this.nombre() }));
  }
}
