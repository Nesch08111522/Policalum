import { Injectable, inject, signal } from '@angular/core';
import { EmpresaPublica } from './models';
import { PortalApi } from './portal.api';

@Injectable({ providedIn: 'root' })
export class MarcaService {
  readonly empresa = signal<EmpresaPublica>({ nombreComercial: 'POLICALUM' });

  constructor() {
    inject(PortalApi).empresa().subscribe({
      next: (empresa) => this.empresa.set(empresa),
      error: () => undefined,
    });
  }
}
