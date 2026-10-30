import { Injectable, inject, signal } from '@angular/core';
import { PortalApi } from './portal.api';
import { EmpresaPublica } from './models';

@Injectable({ providedIn: 'root' })
export class EmpresaStore {
  private readonly api = inject(PortalApi);
  readonly empresa = signal<EmpresaPublica>({ nombreComercial: 'POLICALUM' });

  constructor() {
    this.api.empresa().subscribe({
      next: (empresa) => this.empresa.set(empresa),
      error: () => undefined,
    });
  }
}
