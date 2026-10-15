import { Injectable } from '@angular/core';
import { Observable, catchError, finalize, map, of, shareReplay, tap } from 'rxjs';
import * as Api from '../../core/api/openapi.types';
import { AlmacenesApiService } from '../../features/almacenes/data-access/almacenes.api';
import { CategoriasApiService } from '../../features/categorias/data-access/categorias.api';
import { ClientesApiService } from '../../features/clientes/data-access/clientes.api';
import { ProductosApiService } from '../../features/productos/data-access/productos.api';
import { ProveedoresApiService } from '../../features/proveedores/data-access/proveedores.api';
import { RolesApiService } from '../../features/roles/data-access/roles.api';
import { SedesApiService } from '../../features/sedes/data-access/sedes.api';
import { ServiciosApiService } from '../../features/servicios/data-access/servicios.api';
import { UsersApiService } from '../../features/users/data-access/users.api';
import { SelectOption } from '../models/ui.models';

export type LookupKey =
  | 'sedes'
  | 'almacenes'
  | 'categorias'
  | 'productos'
  | 'proveedores'
  | 'clientes'
  | 'servicios'
  | 'usuarios'
  | 'roles';

@Injectable({ providedIn: 'root' })
export class LookupService {
  sedes: Api.SedeResponseDTO[] = [];
  almacenes: Api.AlmacenResponseDTO[] = [];
  categorias: Api.CategoriaResponseDTO[] = [];
  productos: Api.ProductoResponseDTO[] = [];
  proveedores: Api.ProveedorResponseDTO[] = [];
  clientes: Api.ClienteResponseDTO[] = [];
  servicios: Api.ServicioResponseDTO[] = [];
  usuarios: Api.UserResponseDTO[] = [];
  roles: Api.RolResponseDTO[] = [];

  private readonly inflight = new Map<LookupKey, Observable<unknown[]>>();
  private readonly epoch: Record<LookupKey, number> = {
    sedes: 0,
    almacenes: 0,
    categorias: 0,
    productos: 0,
    proveedores: 0,
    clientes: 0,
    servicios: 0,
    usuarios: 0,
    roles: 0,
  };

  constructor(
    private readonly sedesApi: SedesApiService,
    private readonly almacenesApi: AlmacenesApiService,
    private readonly categoriasApi: CategoriasApiService,
    private readonly productosApi: ProductosApiService,
    private readonly proveedoresApi: ProveedoresApiService,
    private readonly clientesApi: ClientesApiService,
    private readonly serviciosApi: ServiciosApiService,
    private readonly usersApi: UsersApiService,
    private readonly rolesApi: RolesApiService,
  ) {}

  /** Descarta el catálogo en memoria para que el próximo selector consulte al backend. */
  invalidate(...keys: LookupKey[]): void {
    for (const key of keys) {
      this.epoch[key] += 1;
      this[key] = [];
      this.inflight.delete(key);
    }
  }

  loadSedes(force = false): Observable<Api.SedeResponseDTO[]> {
    return this.load('sedes', force, this.sedesApi.GetApiV1Sedes({ page: 0, size: 200, sort: 'nombre,asc' }).pipe(map((page) => page.content ?? [])));
  }

  loadAlmacenes(force = false): Observable<Api.AlmacenResponseDTO[]> {
    return this.load('almacenes', force, this.almacenesApi.GetApiV1Almacenes({ page: 0, size: 200, sort: 'nombre,asc' }).pipe(map((page) => page.content ?? [])));
  }

  loadCategorias(force = false): Observable<Api.CategoriaResponseDTO[]> {
    return this.load('categorias', force, this.categoriasApi.GetApiV1Categorias({ page: 0, size: 200, sort: 'nombre,asc' }).pipe(map((page) => page.content ?? [])));
  }

  loadProductos(force = false): Observable<Api.ProductoResponseDTO[]> {
    return this.load('productos', force, this.productosApi.GetApiV1Productos({ page: 0, size: 200, sort: 'nombre,asc' }).pipe(map((page) => page.content ?? [])));
  }

  loadProveedores(force = false): Observable<Api.ProveedorResponseDTO[]> {
    return this.load('proveedores', force, this.proveedoresApi.GetApiV1Proveedores({ page: 0, size: 200, sort: 'nombreRazon,asc' }).pipe(map((page) => page.content ?? [])));
  }

  loadClientes(force = false): Observable<Api.ClienteResponseDTO[]> {
    return this.load('clientes', force, this.clientesApi.GetApiV1Clientes({ page: 0, size: 200, sort: 'nombre,asc' }).pipe(map((page) => page.content ?? [])));
  }

  loadServicios(force = false): Observable<Api.ServicioResponseDTO[]> {
    return this.load('servicios', force, this.serviciosApi.GetApiV1Servicios({ page: 0, size: 200, sort: 'nombre,asc' }).pipe(map((page) => page.content ?? [])));
  }

  loadUsuarios(force = false): Observable<Api.UserResponseDTO[]> {
    return this.load('usuarios', force, this.usersApi.GetApiV1Users().pipe(map((rows) => rows ?? [])));
  }

  loadRoles(force = false): Observable<Api.RolResponseDTO[]> {
    return this.load('roles', force, this.rolesApi.GetApiV1Roles().pipe(map((rows) => rows ?? [])));
  }

  options(rows: Array<{ id?: string }>, label: (row: { id?: string }) => string): SelectOption[] {
    return rows.filter((row) => row.id).map((row) => ({ value: row.id ?? '', label: label(row) }));
  }

  private load<T>(key: LookupKey, force: boolean, request: Observable<T[]>): Observable<T[]> {
    const cached = this[key] as T[];
    if (!force && cached.length) return of(cached);
    if (force) this.invalidate(key);
    const pending = this.inflight.get(key) as Observable<T[]> | undefined;
    if (pending) return pending;
    const epoch = this.epoch[key];
    const shared = request.pipe(
      tap((rows) => {
        if (this.epoch[key] === epoch) (this[key] as T[]) = rows ?? [];
      }),
      catchError(() => of([] as T[])),
      finalize(() => {
        if (this.inflight.get(key) === shared) this.inflight.delete(key);
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    this.inflight.set(key, shared);
    return shared;
  }
}
