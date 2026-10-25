import { Injectable, signal } from '@angular/core';
import { PerfilApiService } from '../../features/perfil/data-access/perfil.api';

/** Foto de la cuenta, compartida por el encabezado, el menú y la ficha de perfil. */
@Injectable({ providedIn: 'root' })
export class AvatarService {
  readonly url = signal<string | null>(null);
  private actual: string | null = null;

  constructor(private readonly perfil: PerfilApiService) {}

  cargar(): void {
    this.perfil.actual().subscribe({
      next: (perfil) => {
        if (!perfil.foto) {
          this.limpiar();
          return;
        }
        this.perfil.verFoto().subscribe({
          next: (blob) => this.poner(blob),
          error: () => this.limpiar(),
        });
      },
      error: () => this.limpiar(),
    });
  }

  poner(blob: Blob): void {
    if (!blob || blob.size === 0) {
      this.limpiar();
      return;
    }
    const siguiente = URL.createObjectURL(blob);
    this.revocar();
    this.actual = siguiente;
    this.url.set(siguiente);
  }

  limpiar(): void {
    this.revocar();
    this.url.set(null);
  }

  private revocar(): void {
    if (!this.actual) return;
    URL.revokeObjectURL(this.actual);
    this.actual = null;
  }
}
