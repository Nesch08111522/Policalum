import { Injectable, signal } from '@angular/core';

/** Sede elegida en la barra superior. Vacío significa todas las sedes. */
@Injectable({ providedIn: 'root' })
export class SedeFiltroService {
  readonly sedeId = signal('');
  readonly nombre = signal('Sedes');

  elegir(id: string, nombre: string): void {
    this.sedeId.set(id);
    this.nombre.set(id ? nombre : 'Sedes');
  }
}
