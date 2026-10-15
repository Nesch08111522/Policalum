import { Injectable } from '@angular/core';
import { Observable, catchError, map, of, tap } from 'rxjs';
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

  loadSedes(force = false): Observable<Api.SedeResponseDTO[]> {
    if (!force && this.sedes.length) return of(this.sedes);
    return this.sedesApi.GetApiV1Sedes({ page: 0, size: 200, sort: 'nombre,asc' }).pipe(
      map((page) => page.content ?? []),
      tap((rows) => (this.sedes = rows)),
      catchError(() => of([])),
    );
  }

  loadAlmacenes(force = false): Observable<Api.AlmacenResponseDTO[]> {
    if (!force && this.almacenes.length) return of(this.almacenes);
    return this.almacenesApi.GetApiV1Almacenes({ page: 0, size: 200, sort: 'nombre,asc' }).pipe(
      map((page) => page.content ?? []),
      tap((rows) => (this.almacenes = rows)),
      catchError(() => of([])),
    );
  }

  loadCategorias(force = false): Observable<Api.CategoriaResponseDTO[]> {
    if (!force && this.categorias.length) return of(this.categorias);
    return this.categoriasApi.GetApiV1Categorias({ page: 0, size: 200, sort: 'nombre,asc' }).pipe(
      map((page) => page.content ?? []),
      tap((rows) => (this.categorias = rows)),
      catchError(() => of([])),
    );
  }

  loadProductos(force = false): Observable<Api.ProductoResponseDTO[]> {
    if (!force && this.productos.length) return of(this.productos);
    return this.productosApi.GetApiV1Productos({ page: 0, size: 200, sort: 'nombre,asc' }).pipe(
      map((page) => page.content ?? []),
      tap((rows) => (this.productos = rows)),
      catchError(() => of([])),
    );
  }

  loadProveedores(force = false): Observable<Api.ProveedorResponseDTO[]> {
    if (!force && this.proveedores.length) return of(this.proveedores);
    return this.proveedoresApi.GetApiV1Proveedores({ page: 0, size: 200, sort: 'nombreRazon,asc' }).pipe(
      map((page) => page.content ?? []),
      tap((rows) => (this.proveedores = rows)),
      catchError(() => of([])),
    );
  }

  loadClientes(force = false): Observable<Api.ClienteResponseDTO[]> {
    if (!force && this.clientes.length) return of(this.clientes);
    return this.clientesApi.GetApiV1Clientes({ page: 0, size: 200, sort: 'nombre,asc' }).pipe(
      map((page) => page.content ?? []),
      tap((rows) => (this.clientes = rows)),
      catchError(() => of([])),
    );
  }

  loadServicios(force = false): Observable<Api.ServicioResponseDTO[]> {
    if (!force && this.servicios.length) return of(this.servicios);
    return this.serviciosApi.GetApiV1Servicios({ page: 0, size: 200, sort: 'nombre,asc' }).pipe(
      map((page) => page.content ?? []),
      tap((rows) => (this.servicios = rows)),
      catchError(() => of([])),
    );
  }

  loadUsuarios(force = false): Observable<Api.UserResponseDTO[]> {
    if (!force && this.usuarios.length) return of(this.usuarios);
    return this.usersApi.GetApiV1Users().pipe(
      tap((rows) => (this.usuarios = rows ?? [])),
      catchError(() => of([])),
    );
  }

  loadRoles(force = false): Observable<Api.RolResponseDTO[]> {
    if (!force && this.roles.length) return of(this.roles);
    return this.rolesApi.GetApiV1Roles().pipe(
      tap((rows) => (this.roles = rows ?? [])),
      catchError(() => of([])),
    );
  }

  options(rows: Array<{ id?: string }>, label: (row: { id?: string }) => string): SelectOption[] {
    return rows.filter((row) => row.id).map((row) => ({ value: row.id ?? '', label: label(row) }));
  }
}
