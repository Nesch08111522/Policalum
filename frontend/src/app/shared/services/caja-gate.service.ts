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

/**
 * Recuerda el turno abierto de la sede elegida para vender, convertir una cotización
 * o confirmar una preforma. El resto de las pantallas se usa con la caja cerrada.
 */
@Injectable({ providedIn: 'root' })
export class CajaGateService {
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
      this.router.events.pipe(filter((evento): evento is NavigationEnd => evento instanceof NavigationEnd)).subscribe(() => this.revisar());
    }
    this.revisar();
  }

  revisar(): void {
    const sedeId = this.sedeFiltro.sedeId();
    let params = new HttpParams();
    if (sedeId) params = params.set('sedeId', sedeId);
    this.http.get<TurnoAbierto>(`${environment.apiUrl}/api/v1/turnos-caja/abierto`, { params, context: new HttpContext().set(HTTP_SILENCIOSO, true) }).subscribe({
      next: (respuesta) => {
        if (respuesta?.abierto && respuesta.turno?.id && this.coincide(respuesta.turno)) this.caja.recordar(respuesta.turno);
        else this.caja.recordar(null);
      },
      error: () => undefined,
    });
  }

  private coincide(turno: Api.TurnoCajaResponseDTO): boolean {
    const sedeId = this.sedeFiltro.sedeId();
    if (!sedeId) return true;
    return turno.sede?.id === sedeId;
  }
}
