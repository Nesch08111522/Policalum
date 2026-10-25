import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import * as Api from '../../core/api/openapi.types';
import { environment } from '../../../environments/environment';
import { CajaSessionService } from './caja-session.service';

interface TurnoAbierto {
  abierto?: boolean;
  turno?: Api.TurnoCajaResponseDTO | null;
}

/** Al entrar al sistema, exige un turno de caja abierto antes de operar. */
@Injectable({ providedIn: 'root' })
export class CajaGateService {
  private exigida = false;
  private escuchando = false;

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
    private readonly caja: CajaSessionService,
  ) {}

  vigilar(): void {
    if (!this.escuchando) {
      this.escuchando = true;
      this.router.events.pipe(filter((evento): evento is NavigationEnd => evento instanceof NavigationEnd)).subscribe(() => this.exigir());
    }
    this.http.get<TurnoAbierto>(`${environment.apiUrl}/api/v1/turnos-caja/abierto`).subscribe({
      next: (respuesta) => {
        this.exigida = true;
        if (respuesta?.abierto && respuesta.turno?.id) this.caja.recordar(respuesta.turno);
        else this.caja.recordar(null);
        this.exigir();
      },
      error: () => { this.exigida = false; },
    });
  }

  private exigir(): void {
    if (!this.exigida) return;
    const turno = this.caja.turno();
    if (turno?.id && turno.estado !== 'CERRADO') return;
    const ruta = this.router.url.split('?')[0];
    if (ruta.startsWith('/modules/turnos-caja')) return;
    void this.router.navigateByUrl('/modules/turnos-caja');
  }
}
