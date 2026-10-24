import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { QueryMap } from '../../../core/api/backend-api.service';
import { ProductoRequestDTO, ProductoResponseDTO } from '../../../core/api/openapi.types';

export const COLUMNAS_PRODUCTO = [
  'codigoSku',
  'nombre',
  'descripcion',
  'categoria',
  'tipo',
  'precioCompra',
  'precioVentaBase',
  'unidad',
  'esFraccionable',
  'activo',
  'incluyeIgv',
  'tipoAfectacion',
  'imagen',
  'stockInicial',
  'almacen',
] as const;

export interface ProductoImportacionFila {
  codigoSku?: string;
  nombre?: string;
  descripcion?: string;
  categoria?: string;
  tipo?: string;
  precioCompra?: number;
  precioVentaBase?: number;
  unidad?: string;
  esFraccionable?: boolean;
  activo?: boolean;
  incluyeIgv?: boolean;
  tipoAfectacion?: string;
  imagen?: string;
  stockInicial?: number;
  almacen?: string;
}

export interface ProductoImportacionResultado {
  creados: number;
  actualizados: number;
  filas: number;
}

@Injectable({ providedIn: 'root' })
export class ProductosMasivoApiService {
  private readonly base = environment.apiUrl.replace(/\/+$/, '');

  constructor(private readonly http: HttpClient) {}

  exportar(params: QueryMap = {}): Observable<ProductoImportacionFila[]> {
    return this.http.get<ProductoImportacionFila[]>(`${this.base}/api/v1/productos/exportacion`, {
      params: this.limpiar(params),
    });
  }

  importar(filas: ProductoImportacionFila[]): Observable<ProductoImportacionResultado> {
    return this.http.post<ProductoImportacionResultado>(`${this.base}/api/v1/productos/importacion-masiva`, { filas });
  }

  subirImagen(archivo: File): Observable<{ ruta: string }> {
    const cuerpo = new FormData();
    cuerpo.append('archivo', archivo);
    return this.http.post<{ ruta: string }>(`${this.base}/api/v1/productos/imagenes`, cuerpo);
  }

  actualizarConImagen(id: string, producto: ProductoRequestDTO, archivo: File): Observable<ProductoResponseDTO> {
    const cuerpo = new FormData();
    cuerpo.append('producto', new Blob([JSON.stringify(producto)], { type: 'application/json' }), 'producto.json');
    cuerpo.append('archivo', archivo);
    return this.http.put<ProductoResponseDTO>(`${this.base}/api/v1/productos/${id}`, cuerpo);
  }

  verImagen(ruta: string): Observable<Blob> {
    return this.http.get(`${this.base}/api/v1/productos/imagenes`, { params: { ruta }, responseType: 'blob' });
  }

  private limpiar(params: QueryMap): Record<string, string> {
    const salida: Record<string, string> = {};
    for (const [clave, valor] of Object.entries(params)) {
      if (valor !== undefined && valor !== null && valor !== '') salida[clave] = String(valor);
    }
    return salida;
  }
}
