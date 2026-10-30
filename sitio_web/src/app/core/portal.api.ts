import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, of } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { CategoriaPublica, ContactoCreado, CotizacionCreada, EmpresaPublica, PaginaPublica, ProductoPublico, ProyectoPublico } from './models';

@Injectable({ providedIn: 'root' })
export class PortalApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/v1/public`;
  private readonly categoriasGuardadas = signal<CategoriaPublica[] | null>(null);

  empresa(): Observable<EmpresaPublica> {
    return this.http.get<EmpresaPublica>(`${this.base}/empresa`);
  }

  categorias(): Observable<CategoriaPublica[]> {
    const guardadas = this.categoriasGuardadas();
    if (guardadas) return of(guardadas);
    return this.http.get<CategoriaPublica[]>(`${this.base}/catalogo/categorias`).pipe(
      tap((lista) => this.categoriasGuardadas.set(lista)),
    );
  }

  destacados(): Observable<ProductoPublico[]> {
    return this.http.get<ProductoPublico[]>(`${this.base}/catalogo/destacados`);
  }

  productos(filtro: { categoriaId?: string; q?: string; page?: number }): Observable<PaginaPublica<ProductoPublico>> {
    let params = new HttpParams().set('size', '12').set('page', String(filtro.page ?? 0));
    if (filtro.categoriaId) params = params.set('categoriaId', filtro.categoriaId);
    if (filtro.q) params = params.set('q', filtro.q);
    return this.http.get<PaginaPublica<ProductoPublico>>(`${this.base}/catalogo/productos`, { params });
  }

  producto(id: string): Observable<ProductoPublico> {
    return this.http.get<ProductoPublico>(`${this.base}/catalogo/productos/${id}`);
  }

  portafolio(): Observable<ProyectoPublico[]> {
    return this.http.get<ProyectoPublico[]>(`${this.base}/portafolio`);
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
  const cuerpo = (error as { error?: { message?: string } })?.error;
  return cuerpo?.message || 'No se pudo completar la solicitud. Intenta de nuevo.';
}
