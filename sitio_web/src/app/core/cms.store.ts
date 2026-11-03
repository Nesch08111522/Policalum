import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { environment } from '../../environments/environment';

export interface CmsPublico {
  primaryColor: string;
  secondaryColor: string;
  baseFontSize: number;
  themeMode: string;
  aviso: string;
  avisoActivo: boolean;
}

@Injectable({ providedIn: 'root' })
export class CmsStore {
  private readonly http = inject(HttpClient);
  readonly paquete = signal<CmsPublico | null>(null);
  readonly revision = signal(0);
  private eventos?: EventSource;

  constructor() {
    this.cargar();
    this.escuchar();
  }

  cargar(): void {
    this.http.get<CmsPublico>(`${environment.apiUrl}/api/v1/public/cms`).subscribe({
      next: (paquete) => {
        this.paquete.set(paquete);
        this.aplicar(paquete);
        this.revision.update((valor) => valor + 1);
      },
      error: () => undefined,
    });
  }

  private escuchar(): void {
    if (typeof EventSource === 'undefined') return;
    this.eventos = new EventSource(`${environment.apiUrl}/api/v1/public/cms/eventos`);
    this.eventos.addEventListener('cms', () => this.cargar());
    this.eventos.onerror = () => undefined;
  }

  private aplicar(paquete: CmsPublico): void {
    const raiz = document.documentElement;
    raiz.style.setProperty('--cms-primary', paquete.primaryColor);
    raiz.style.setProperty('--cms-secondary', paquete.secondaryColor);
    raiz.style.setProperty('--cms-base', `${paquete.baseFontSize}px`);
    raiz.style.setProperty('--cms-on-primary', textoSobre(paquete.primaryColor));
    raiz.dataset['theme'] = paquete.themeMode || 'aether';
  }
}

function textoSobre(hex: string): string {
  const limpio = hex.replace('#', '');
  if (limpio.length !== 6) return '#ffffff';
  const r = parseInt(limpio.slice(0, 2), 16);
  const g = parseInt(limpio.slice(2, 4), 16);
  const b = parseInt(limpio.slice(4, 6), 16);
  const luma = (r * 299 + g * 587 + b * 114) / 1000;
  return luma > 160 ? '#050505' : '#ffffff';
}
