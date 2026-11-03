import { Injectable, signal } from '@angular/core';

export type Tema = 'dia' | 'noche';

const CLAVE = 'policalum-tema';

@Injectable({ providedIn: 'root' })
export class TemaService {
  readonly modo = signal<Tema>(this.leer());

  constructor() {
    this.aplicar(this.modo());
  }

  alternar(): void {
    const siguiente: Tema = this.modo() === 'noche' ? 'dia' : 'noche';
    this.modo.set(siguiente);
    localStorage.setItem(CLAVE, siguiente);
    this.aplicar(siguiente);
  }

  private aplicar(tema: Tema): void {
    document.documentElement.dataset['tema'] = tema;
  }

  private leer(): Tema {
    try {
      const guardado = localStorage.getItem(CLAVE);
      if (guardado === 'dia' || guardado === 'noche') return guardado;
    } catch {
      return 'noche';
    }
    return 'noche';
  }
}
