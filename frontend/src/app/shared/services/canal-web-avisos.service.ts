import { Injectable, signal } from '@angular/core';

/** Aviso del canal web (cotización o mensaje). Las pantallas abiertas recargan su lista. */
@Injectable({ providedIn: 'root' })
export class CanalWebAvisos {
  readonly revision = signal(0);

  avisar(): void {
    this.revision.update((valor) => valor + 1);
  }
}
