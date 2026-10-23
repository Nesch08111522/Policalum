import { CommonModule } from '@angular/common';
import { Component, HostListener, OnDestroy, OnInit, effect, inject, untracked, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink, Routes } from '@angular/router';
import { Subject, catchError, debounceTime, distinctUntilChanged, forkJoin, of, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { SelectComponent } from '../../../shared/components/select/select.component';
import { SelectOption } from '../../../shared/models/ui.models';
import { PenPipe } from '../../../shared/pipes/format.pipes';
import { InventarioSyncService } from '../../../shared/services/inventario-sync.service';
import { LookupService } from '../../../shared/services/lookup.service';
import { SedeFiltroService } from '../../../shared/services/sede-filtro.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { PageState } from '../../../shared/utils/page-state';
import { KardexApiService } from '../../kardex/data-access/kardex.api';
import { StockApiService } from '../../stock/data-access/stock.api';
import { ProductosApiService } from '../data-access/productos.api';

interface CorteVista {
  nombre: string;
  precio: number;
  factor: number;
  equivalentes: number;
  ancho: number;
}

interface FilaCatalogo {
  producto: Api.ProductoResponseDTO;
  cantidad: number;
  minimo: number;
  bajo: boolean;
  unidad: string;
  almacen: string;
  sede: string;
  otrosAlmacenes: number;
  ancho: number;
  cortes: CorteVista[];
  estado: 'optimo' | 'minimo' | 'agotado' | 'inactivo';
  estadoLabel: string;
}

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-productos-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PageHeaderComponent, ModalComponent, ConfirmDialogComponent, FormFieldComponent, SelectComponent, PaginationComponent, IconComponent, PenPipe],
  templateUrl: './productos-page.component.html',
  styleUrls: ['./productos-page.component.css'],
})
export class ProductosPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly inventario = inject(InventarioSyncService);
  private readonly sedeFiltro = inject(SedeFiltroService);
  readonly state = new PageState<Api.ProductoResponseDTO>();
  readonly filtros = this.fb.nonNullable.group({ searchTerm: [''], categoriaId: [''], sedeId: [''], tipo: [''], activo: [''], esFraccionable: [''] });
  readonly form = this.fb.nonNullable.group({
    id: [''],
    nombre: ['', [Validators.required, Validators.maxLength(200)]],
    codigoSku: [''],
    descripcion: [''],
    categoriaId: [''],
    proveedorId: [''],
    tipo: ['FISICO' as Api.ProductoRequestDTO['tipo'], Validators.required],
    unidad: ['UNIDAD'],
    precioCompra: [0, [Validators.required, Validators.min(0)]],
    precioVentaBase: [0, [Validators.required, Validators.min(0)]],
    activo: [true],
    esFraccionable: [false],
    incluyeIgv: [true],
  });
  categorias: SelectOption[] = [];
  proveedores: SelectOption[] = [];
  sedes: SelectOption[] = [];
  stockFiltro: '' | 'CON' | 'BAJO' | 'SIN' = '';
  dashboard: Api.StockDashboardDTO | null = null;
  alertas: Api.SaldoKardexDTO[] = [];
  mermas: Api.ReporteMermasDTO | null = null;
  stockTruncado = false;
  modal = false;
  editando = false;
  guardando = false;
  errorFormulario: string | null = null;
  confirmar = false;
  eliminando = false;
  accion: 'eliminar' | 'desactivar' = 'eliminar';
  seleccionado: Api.ProductoResponseDTO | null = null;
  private saldos = new Map<string, Api.SaldoKardexDTO[]>();
  private carga = 0;
  private fraccionesActuales: Api.CatalogoFraccionResponseDTO[] = [];
  private readonly buscar$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly api: ProductosApiService,
    private readonly stockApi: StockApiService,
    private readonly kardex: KardexApiService,
    private readonly lookup: LookupService,
    private readonly toast: ToastService,
    private readonly router: Router,
  ) {
    this.state.sort = 'nombre,asc';
    effect(() => {
      this.lookup.revision();
      untracked(() => this.cargarCombos());
    });
    effect(() => {
      const sede = this.sedeFiltro.sedeId();
      untracked(() => {
        if (this.filtros.controls.sedeId.value === sede) return;
        this.filtros.controls.sedeId.setValue(sede);
      });
    });
    effect(() => {
      const revision = this.inventario.revision();
      untracked(() => { if (revision) this.cargar(); });
    });
  }

  ngOnInit(): void {
    this.cargar();
    this.buscar$.pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroy$)).subscribe((valor) => {
      this.filtros.controls.searchTerm.setValue(valor);
      this.filtrar();
    });
    this.filtros.controls.categoriaId.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => this.filtrar());
    this.filtros.controls.sedeId.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => this.filtrar());
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  get filas(): FilaCatalogo[] {
    const filas = this.state.rows.map((producto) => this.fila(producto));
    if (this.stockFiltro === 'CON') return filas.filter((fila) => fila.cantidad > 0);
    if (this.stockFiltro === 'BAJO') return filas.filter((fila) => fila.bajo);
    if (this.stockFiltro === 'SIN') return filas.filter((fila) => fila.cantidad <= 0);
    return filas;
  }

  cargarCombos(): void {
    this.lookup.loadCategorias().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.categorias = this.lookup.options(rows, (row) => (row as Api.CategoriaResponseDTO).nombre ?? 'Categoría'); });
    this.lookup.loadProveedores().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.proveedores = this.lookup.options(rows, (row) => (row as Api.ProveedorResponseDTO).nombreRazon ?? 'Proveedor'); });
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede'); });
  }

  cargar(): void {
    const extra = this.filtros.getRawValue();
    const query = this.state.query({
      searchTerm: extra.searchTerm,
      categoriaId: extra.categoriaId,
      tipo: extra.tipo,
      activo: extra.activo,
      esFraccionable: extra.esFraccionable,
    });
    if (this.stockFiltro) {
      query['page'] = 0;
      query['size'] = 100;
    }
    const token = ++this.carga;
    this.state.loading = true;
    this.state.error = null;
    forkJoin({
      page: this.api.GetApiV1Productos(query),
      saldos: this.kardex.GetApiV1KardexSaldos({ sedeId: extra.sedeId, page: 0, size: 200 }).pipe(catchError(() => of(null))),
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: ({ page, saldos }) => {
        if (token !== this.carga) return;
        this.state.apply(page);
        this.saldos = new Map();
        for (const row of saldos?.content ?? []) {
          const id = row.productoId ?? '';
          if (!id) continue;
          const lista = this.saldos.get(id) ?? [];
          lista.push(row);
          this.saldos.set(id, lista);
        }
        this.stockTruncado = Boolean(this.stockFiltro) && (page.totalElements ?? 0) > (page.content?.length ?? 0);
      },
      error: (error: unknown) => { if (token !== this.carga) return; this.state.fail(problemText(error), problemStatus(error)); },
    });
    this.cargarPanel(extra.sedeId);
  }

  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void {
    this.stockFiltro = '';
    this.filtros.reset({ searchTerm: '', categoriaId: '', sedeId: this.sedeFiltro.sedeId(), tipo: '', activo: '', esFraccionable: '' });
    this.filtrar();
  }
  onBuscar(event: Event): void { this.buscar$.next((event.target as HTMLInputElement).value); }
  elegirStock(valor: 'CON' | 'BAJO' | 'SIN'): void {
    this.stockFiltro = this.stockFiltro === valor ? '' : valor;
    this.filtrar();
  }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }

  abrirCrear(): void { this.cargarCombos(); this.editando = false; this.fraccionesActuales = []; this.errorFormulario = null; this.form.reset({ id: '', nombre: '', codigoSku: '', descripcion: '', categoriaId: '', proveedorId: '', tipo: 'FISICO', unidad: 'UNIDAD', precioCompra: 0, precioVentaBase: 0, activo: true, esFraccionable: false, incluyeIgv: true }); this.modal = true; }
  abrirEditar(item: Api.ProductoResponseDTO): void {
    this.cargarCombos();
    this.editando = true;
    this.fraccionesActuales = item.fracciones ?? [];
    this.errorFormulario = null;
    this.form.reset({
      id: item.id ?? '', nombre: item.nombre ?? '', codigoSku: item.codigoSku ?? '', descripcion: item.descripcion ?? '',
      categoriaId: item.categoria?.id ?? '', proveedorId: item.proveedor?.id ?? '', tipo: item.tipo ?? 'FISICO', unidad: item.unidad ?? 'UNIDAD',
      precioCompra: item.precioCompra ?? 0, precioVentaBase: item.precioVentaBase ?? 0, activo: item.activo ?? true, esFraccionable: item.esFraccionable ?? false, incluyeIgv: item.incluyeIgv ?? true,
    });
    this.modal = true;
  }
  guardar(): void {
    if (this.guardando) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const body = this.cuerpo(value, value.id, this.fraccionesActuales);
    this.guardando = true;
    const request = this.editando && value.id ? this.api.PutApiV1ProductosId({ id: value.id }, body) : this.api.PostApiV1Productos({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = false; this.lookup.invalidate('productos'); this.inventario.marcarCambio(); this.toast.success(this.editando ? 'Producto actualizado.' : 'Producto creado.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }
  pedirEliminar(item: Api.ProductoResponseDTO): void { this.seleccionado = item; this.accion = 'eliminar'; this.confirmar = true; }
  pedirDesactivar(item: Api.ProductoResponseDTO): void { this.seleccionado = item; this.accion = 'desactivar'; this.confirmar = true; }
  confirmarAccion(): void {
    if (this.accion === 'desactivar') this.desactivar();
    else this.eliminar();
  }
  eliminar(): void {
    if (!this.seleccionado?.id) return;
    this.eliminando = true;
    this.api.DeleteApiV1ProductosId({ id: this.seleccionado.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.eliminando = false; this.confirmar = false; this.lookup.invalidate('productos'); this.inventario.marcarCambio(); this.toast.success('Producto eliminado.'); this.cargar(); },
      error: (error: unknown) => { this.eliminando = false; this.confirmar = false; this.toast.error(problemText(error)); },
    });
  }
  verKardex(item: Api.ProductoResponseDTO): void {
    void this.router.navigate(['/modules/kardex'], { queryParams: { productoId: item.id ?? null } });
  }
  exportar(): void {
    const filas = this.filas;
    const encabezado = ['SKU', 'Producto', 'Categoría', 'Precio base', 'Saldo', 'Unidad', 'Almacén', 'Sede', 'Estado'];
    const lineas = [encabezado, ...filas.map((fila) => [
      fila.producto.codigoSku ?? '',
      fila.producto.nombre ?? '',
      fila.producto.categoria?.nombre ?? '',
      String(fila.producto.precioVentaBase ?? ''),
      String(fila.cantidad),
      fila.unidad,
      fila.almacen,
      fila.sede,
      fila.estadoLabel,
    ])];
    const csv = lineas.map((columnas) => columnas.map((valor) => `"${String(valor).replaceAll('"', '""')}"`).join(',')).join('\n');
    const enlace = document.createElement('a');
    enlace.href = URL.createObjectURL(new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' }));
    enlace.download = 'productos.csv';
    enlace.click();
    URL.revokeObjectURL(enlace.href);
    this.toast.success(filas.length ? 'Catálogo exportado en CSV.' : 'El archivo se generó sin filas.');
  }
  iniciales(nombre: string | undefined): string {
    const partes = (nombre ?? 'P').trim().split(/\s+/).slice(0, 2);
    return partes.map((parte) => parte.charAt(0).toUpperCase()).join('') || 'P';
  }
  errorDe(nombre: 'nombre' | 'precioCompra' | 'precioVentaBase'): string | null { return fieldError(this.form.controls[nombre]); }

  @HostListener('document:keydown', ['$event'])
  atajo(event: KeyboardEvent): void {
    if (event.key !== 'F2' || this.modal) return;
    event.preventDefault();
    document.getElementById('prod-buscar')?.focus();
  }

  private cargarPanel(sedeId: string): void {
    this.stockApi.GetApiV1StockDashboard({ sedeId }).pipe(takeUntil(this.destroy$)).subscribe({ next: (data) => { this.dashboard = data; }, error: () => { this.dashboard = null; } });
    this.stockApi.GetApiV1StockBajoStock({ sedeId, page: 0, size: 4 }).pipe(takeUntil(this.destroy$)).subscribe({ next: (page) => { this.alertas = page.content ?? []; }, error: () => { this.alertas = []; } });
    this.kardex.GetApiV1KardexMermas({ sedeId, page: 0, size: 3 }).pipe(takeUntil(this.destroy$)).subscribe({ next: (data) => { this.mermas = data; }, error: () => { this.mermas = null; } });
  }

  private desactivar(): void {
    const item = this.seleccionado;
    if (!item?.id) return;
    this.eliminando = true;
    this.api.PutApiV1ProductosId({ id: item.id }, this.cuerpoDesde(item, false)).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.eliminando = false; this.confirmar = false; this.lookup.invalidate('productos'); this.toast.success('Producto desactivado.'); this.cargar(); },
      error: (error: unknown) => { this.eliminando = false; this.confirmar = false; this.toast.error(problemText(error)); },
    });
  }

  private cuerpo(value: ReturnType<ProductosPageComponent['form']['getRawValue']>, productoId: string, fracciones: Api.CatalogoFraccionResponseDTO[]): Api.ProductoRequestDTO {
    return {
      nombre: value.nombre.trim(), codigoSku: value.codigoSku.trim() || undefined, descripcion: value.descripcion.trim() || undefined,
      categoriaId: value.categoriaId || undefined, proveedorId: value.proveedorId || undefined, tipo: value.tipo, unidad: value.unidad.trim() || undefined,
      precioCompra: Number(value.precioCompra), precioVentaBase: Number(value.precioVentaBase), activo: value.activo, esFraccionable: value.esFraccionable, incluyeIgv: value.incluyeIgv,
      fracciones: value.esFraccionable ? this.fraccionesDe(productoId, fracciones) : undefined,
    };
  }

  private cuerpoDesde(item: Api.ProductoResponseDTO, activo: boolean): Api.ProductoRequestDTO {
    return {
      nombre: item.nombre ?? '',
      codigoSku: item.codigoSku,
      descripcion: item.descripcion,
      categoriaId: item.categoria?.id,
      proveedorId: item.proveedor?.id,
      tipo: item.tipo ?? 'FISICO',
      unidad: item.unidad,
      precioCompra: Number(item.precioCompra ?? 0),
      precioVentaBase: Number(item.precioVentaBase ?? 0),
      activo,
      esFraccionable: item.esFraccionable ?? false,
      incluyeIgv: item.incluyeIgv ?? true,
      fracciones: item.esFraccionable ? this.fraccionesDe(item.id ?? '', item.fracciones ?? []) : undefined,
    };
  }

  private fraccionesDe(productoId: string, fracciones: Api.CatalogoFraccionResponseDTO[]): Api.CatalogoFraccionRequestDTO[] | undefined {
    const lista = fracciones.filter((fraccion) => fraccion.nombreFraccion && fraccion.factorDescuentoKardex && productoId);
    if (!lista.length) return undefined;
    return lista.map((fraccion) => ({
      productoId,
      nombreFraccion: fraccion.nombreFraccion ?? '',
      factorDescuentoKardex: Number(fraccion.factorDescuentoKardex),
      precioVenta: fraccion.precioVenta,
      activo: fraccion.activo ?? true,
    }));
  }

  private fila(producto: Api.ProductoResponseDTO): FilaCatalogo {
    const registros = this.saldos.get(producto.id ?? '') ?? [];
    const cantidad = registros.reduce((total, row) => total + Number(row.cantidad ?? 0), 0);
    const minimo = registros.reduce((total, row) => total + Number(row.stockMinimo ?? 0), 0);
    const bajo = registros.some((row) => row.bajoMinimo) || (minimo > 0 && cantidad <= minimo);
    const principal = [...registros].sort((a, b) => Number(b.cantidad ?? 0) - Number(a.cantidad ?? 0))[0];
    const unidad = principal?.unidad || producto.unidad || 'UNID';
    const cortes = this.cortes(producto, cantidad);
    const referencia = Math.max(minimo * 2, cantidad, 1);
    let estado: FilaCatalogo['estado'] = 'optimo';
    let estadoLabel = 'Óptimo';
    if (producto.activo === false) { estado = 'inactivo'; estadoLabel = 'Inactivo'; }
    else if (cantidad <= 0) { estado = 'agotado'; estadoLabel = 'Sin stock'; }
    else if (bajo) { estado = 'minimo'; estadoLabel = 'Stock mínimo'; }
    return {
      producto,
      cantidad,
      minimo,
      bajo,
      unidad,
      almacen: principal?.almacenNombre || 'Sin almacén',
      sede: principal?.sedeNombre || 'Sin sede',
      otrosAlmacenes: Math.max(registros.length - 1, 0),
      ancho: Math.max(0, Math.min(100, (cantidad / referencia) * 100)),
      cortes,
      estado,
      estadoLabel,
    };
  }

  private cortes(producto: Api.ProductoResponseDTO, cantidad: number): CorteVista[] {
    const fracciones = (producto.fracciones ?? []).filter((fraccion) => fraccion.activo !== false);
    const base = fracciones.length
      ? fracciones
      : [{ nombreFraccion: producto.unidad || 'Unidad', precioVenta: producto.precioVentaBase, factorDescuentoKardex: 1 }];
    const vistos = base
      .map((fraccion) => {
        const factor = Number(fraccion.factorDescuentoKardex ?? 0);
        const equivalentes = factor > 0 ? cantidad / factor : 0;
        return {
          nombre: fraccion.nombreFraccion || 'Corte',
          precio: Number(fraccion.precioVenta ?? 0),
          factor,
          equivalentes,
          ancho: 0,
        };
      })
      .sort((a, b) => b.factor - a.factor);
    const mayor = Math.max(...vistos.map((corte) => corte.equivalentes), 1);
    return vistos.map((corte) => ({ ...corte, ancho: Math.max(0, Math.min(100, (corte.equivalentes / mayor) * 100)) }));
  }
}

export const PRODUCTOS_ROUTES: Routes = [
  { path: '', component: ProductosPageComponent, title: 'Productos', data: { section: 'Inventario', breadcrumb: 'Productos' } },
];
