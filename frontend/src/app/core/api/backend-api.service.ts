import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EndpointDefinition } from './api-endpoints.generated';

export type QueryValue = string | number | boolean | null | undefined;
export type QueryMap = Record<string, QueryValue>;

@Injectable({ providedIn: 'root' })
export class BackendApiService {
  private readonly baseUrl = environment.apiUrl.replace(/\/+$/, '');

  constructor(private readonly http: HttpClient) {}

  execute<T>(
    endpoint: EndpointDefinition,
    routeValues: QueryMap = {},
    queryValues: QueryMap = {},
    body?: unknown,
  ): Observable<T> {
    let path = endpoint.path;

    // Sustituir parámetros de ruta
    for (const parameter of endpoint.parameters) {
      const value = routeValues[parameter];
      if (path.includes(`{${parameter}}`)) {
        if (value === undefined || value === null || value === '') {
          // Importante: return dentro de un Observable, no throw
          return throwError(() => new Error(`Falta el parámetro de ruta: ${parameter}`));
        }
        path = path.replace(`{${parameter}}`, encodeURIComponent(String(value)));
      }
    }

    // Armar query params (excluye los que ya van en la ruta)
    let params = new HttpParams();
    for (const [key, value] of Object.entries(queryValues)) {
      if (endpoint.path.includes(`{${key}}`)) continue;
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }

    const url = `${this.baseUrl}${path}`;

    if (endpoint.method === 'GET') return this.http.get<T>(url, { params });
    if (endpoint.method === 'POST') return this.http.post<T>(url, body ?? {}, { params });
    if (endpoint.method === 'PUT') return this.http.put<T>(url, body ?? {}, { params });
    if (endpoint.method === 'PATCH') return this.http.patch<T>(url, body ?? {}, { params });
    return this.http.delete<T>(url, { params });
  }
}