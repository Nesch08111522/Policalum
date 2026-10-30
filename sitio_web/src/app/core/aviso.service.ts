import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AvisoService {
  readonly texto = signal('');

  mostrar(mensaje: string): void {
    this.texto.set(mensaje);
  }

  cerrar(): void {
    this.texto.set('');
  }
}
