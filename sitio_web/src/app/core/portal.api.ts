import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { CategoriaPublica, ContactoCreado, CotizacionCreada, EmpresaPublica, FormatoCotizacion, PaginaPublica, ProductoPublico, ProyectoPublico, SedePublica, ServicioPublico, VistaCotizacion } from './models';

@Injectable({ providedIn: 'root' })
export class PortalApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/v1/public`;
  private readonly categoriasGuardadas = signal<CategoriaPublica[] | null>(null);

  empresa(): Observable<EmpresaPublica> {
    return this.http.get<EmpresaPublica>(`${this.base}/empresa`);
  }

  sedes(): Observable<SedePublica[]> {
    return this.http.get<SedePublica[]>(`${this.base}/sedes`);
  }

  categorias(): Observable<CategoriaPublica[]> {
    const guardadas = this.categoriasGuardadas();
    if (guardadas) return of(guardadas);
    return this.http.get<CategoriaPublica[]>(`${this.base}/catalogo/categorias`).pipe(
      tap((lista) => this.categoriasGuardadas.set(lista)),
    );
  }

  ofertas(): Observable<ProductoPublico[]> {
    return this.http.get<ProductoPublico[]>(`${this.base}/catalogo/ofertas`);
  }

  servicios(): Observable<ServicioPublico[]> {
    return this.http.get<ServicioPublico[]>(`${this.base}/servicios`);
  }

  destacados(): Observable<ProductoPublico[]> {
    return this.http.get<ProductoPublico[]>(`${this.base}/catalogo/destacados`);
  }

  productos(filtro: { categoriaId?: string; q?: string; page?: number; size?: number; sort?: string }): Observable<PaginaPublica<ProductoPublico>> {
    let params = new HttpParams().set('size', String(filtro.size ?? 12)).set('page', String(filtro.page ?? 0));
    if (filtro.categoriaId) params = params.set('categoriaId', filtro.categoriaId);
    if (filtro.q) params = params.set('q', filtro.q);
    if (filtro.sort) params = params.set('sort', filtro.sort);
    const guardable = !filtro.q && !filtro.categoriaId && (filtro.page ?? 0) === 0;
    return this.http.get<PaginaPublica<ProductoPublico>>(`${this.base}/catalogo/productos`, { params }).pipe(
      tap((pagina) => {
        if (guardable) localStorage.setItem('policalum-catalogo', JSON.stringify(pagina));
      }),
      catchError((error: unknown) => {
        if (!guardable) return throwError(() => error);
        try {
          const crudo = localStorage.getItem('policalum-catalogo');
          if (!crudo) return throwError(() => error);
          const pagina = JSON.parse(crudo) as PaginaPublica<ProductoPublico>;
          return of({ ...pagina, enCache: true });
        } catch {
          return throwError(() => error);
        }
      }),
    );
  }

  producto(id: string): Observable<ProductoPublico> {
    return this.http.get<ProductoPublico>(`${this.base}/catalogo/productos/${id}`);
  }

  portafolio(): Observable<ProyectoPublico[]> {
    return this.http.get<ProyectoPublico[]>(`${this.base}/portafolio`);
  }

  formatoCotizacion(): Observable<FormatoCotizacion> {
    return this.http.get<FormatoCotizacion>(`${this.base}/cotizaciones/formato`);
  }

  vistaCotizacion(cuerpo: unknown): Observable<VistaCotizacion> {
    return this.http.post<VistaCotizacion>(`${this.base}/cotizaciones/vista`, cuerpo);
  }

  cotizar(cuerpo: unknown): Observable<CotizacionCreada> {
    return this.http.post<CotizacionCreada>(`${this.base}/cotizaciones`, cuerpo);
  }

  pdfUrl(id: string): string {
    return `${this.base}/cotizaciones/${id}/pdf`;
  }

  contacto(cuerpo: unknown): Observable<ContactoCreado> {
    return this.http.post<ContactoCreado>(`${this.base}/contacto`, cuerpo);
  }
}

export function media(ruta?: string | null): string | null {
  if (!ruta) return null;
  if (ruta.startsWith('http://') || ruta.startsWith('https://')) return ruta;
  return `${environment.apiUrl}${ruta}`;
}

const dinero = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' });

export function pen(valor: number | null | undefined): string {
  return dinero.format(Number(valor ?? 0));
}

export function mensajeError(error: unknown): string {
  const fallo = error as { status?: number; error?: { message?: string } };
  if (fallo?.status === 0) return 'No hay conexión con el ERP. El catálogo sale de la base ERPpoli.';
  return fallo?.error?.message || 'No se pudo completar la solicitud. Intenta de nuevo.';
}
