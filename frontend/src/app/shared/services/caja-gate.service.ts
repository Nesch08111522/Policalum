import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import * as Api from '../../core/api/openapi.types';
import { HTTP_SILENCIOSO } from '../../core/interceptors/api-error.interceptor';
import { environment } from '../../../environments/environment';
import { CajaSessionService } from './caja-session.service';
import { SedeFiltroService } from './sede-filtro.service';

interface TurnoAbierto {
  abierto?: boolean;
  turno?: Api.TurnoCajaResponseDTO | null;
}

/** Exige un turno abierto de la sede elegida. Sin sede, basta cualquier turno del usuario. */
@Injectable({ providedIn: 'root' })
export class CajaGateService {
  private exigida = false;
  private escuchando = false;

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
    private readonly caja: CajaSessionService,
    private readonly sedeFiltro: SedeFiltroService,
  ) {}

  vigilar(): void {
    if (!this.escuchando) {
      this.escuchando = true;
      this.router.events.pipe(filter((evento): evento is NavigationEnd => evento instanceof NavigationEnd)).subscribe(() => this.exigir());
    }
    this.revisar();
  }

  revisar(): void {
    const sedeId = this.sedeFiltro.sedeId();
    let params = new HttpParams();
    if (sedeId) params = params.set('sedeId', sedeId);
    this.http.get<TurnoAbierto>(`${environment.apiUrl}/api/v1/turnos-caja/abierto`, { params, context: new HttpContext().set(HTTP_SILENCIOSO, true) }).subscribe({
      next: (respuesta) => {
        this.exigida = true;
        if (respuesta?.abierto && respuesta.turno?.id && this.coincide(respuesta.turno)) this.caja.recordar(respuesta.turno);
        else this.caja.recordar(null);
        this.exigir();
      },
      error: () => { this.exigida = false; },
    });
  }

  private exigir(): void {
    if (!this.exigida) return;
    const turno = this.caja.turno();
    if (turno?.id && turno.estado !== 'CERRADO' && this.coincide(turno)) return;
    const ruta = this.router.url.split('?')[0];
    if (ruta.startsWith('/modules/turnos-caja')) return;
    void this.router.navigateByUrl('/modules/turnos-caja');
  }

  private coincide(turno: Api.TurnoCajaResponseDTO): boolean {
    const sedeId = this.sedeFiltro.sedeId();
    if (!sedeId) return true;
    return turno.sede?.id === sedeId;
  }
}
