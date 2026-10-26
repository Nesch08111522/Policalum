import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, effect, inject, untracked, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Routes } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { FilterBarComponent } from '../../../shared/components/filter-bar/filter-bar.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { ExportButtonsComponent } from '../../../shared/components/export-buttons/export-buttons.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SelectComponent } from '../../../shared/components/select/select.component';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { TabsComponent } from '../../../shared/components/tabs/tabs.component';
import { ColumnDef, SelectOption, TabItem } from '../../../shared/models/ui.models';
import { PenPipe } from '../../../shared/pipes/format.pipes';
import { TokenService } from '../../../services/token.service';
import { InventarioSyncService } from '../../../shared/services/inventario-sync.service';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { problemStatus, problemText } from '../../../shared/utils/feedback';
import { ignoreStale, PageState } from '../../../shared/utils/page-state';
import { KardexApiService } from '../data-access/kardex.api';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-kardex-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, ExportButtonsComponent, FilterBarComponent, DataTableComponent, ModalComponent, FormFieldComponent, SelectComponent, TabsComponent, StatCardComponent, PenPipe],
  templateUrl: './kardex-page.component.html',
  styleUrls: ['./kardex-page.component.css'],
})
export class KardexPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly tabs: TabItem[] = [
    { id: 'movimientos', label: 'Movimientos' },
    { id: 'saldos', label: 'Saldos' },
    { id: 'valorizacion', label: 'Valorización' },
    { id: 'mermas', label: 'Mermas' },
  ];
  tab = 'movimientos';
  readonly state = new PageState<Record<string, unknown>>();
  readonly filtros = this.fb.nonNullable.group({ productoId: [''], almacenId: [''], sedeId: [''], tipoReferencia: [''], desde: [''], hasta: [''], busqueda: [''] });
  readonly form = this.fb.nonNullable.group({
    productoId: ['', Validators.required], almacenId: ['', Validators.required],
    tipoMovimiento: ['ENTRADA' as 'ENTRADA' | 'SALIDA', Validators.required],
    tipoReferencia: ['AJUSTE' as Api.KardexMovimientoRequestDTO['tipoReferencia'], Validators.required],
    cantidadMovida: [1, [Validators.required, Validators.min(0.0001)]], observacion: [''],
  });
  productos: SelectOption[] = [];
  almacenes: SelectOption[] = [];
  sedes: SelectOption[] = [];
  valorizacion: Api.ValorizacionKardexDTO | null = null;
  mermas: Api.ReporteMermasDTO | null = null;
  modal = false;
  guardando = false;
  errorFormulario: string | null = null;
  private omitirRevision = 0;
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: KardexApiService, private readonly lookup: LookupService, private readonly inventario: InventarioSyncService, private readonly toast: ToastService, private readonly route: ActivatedRoute, private readonly tokens: TokenService) {
    effect(() => {
      const revision = this.inventario.revision();
      untracked(() => {
        if (!revision || revision === this.omitirRevision) return;
        this.cargar();
      });
    });
  }
  ngOnInit(): void {
    const productoId = this.route.snapshot.queryParamMap.get('productoId') ?? '';
    const tab = this.route.snapshot.queryParamMap.get('tab') ?? '';
    if (productoId) this.filtros.controls.productoId.setValue(productoId);
    if (tab === 'mermas' || tab === 'saldos' || tab === 'valorizacion' || tab === 'movimientos') this.tab = tab;
    if (!this.esAdmin && (this.tab === 'mermas' || this.tab === 'valorizacion')) this.tab = 'movimientos';
    this.cargar();
    this.lookup.loadProductos().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.productos = this.lookup.options(rows, (row) => (row as Api.ProductoResponseDTO).nombre ?? 'Producto'); });
    this.lookup.loadAlmacenes().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.almacenes = this.lookup.options(rows, (row) => (row as Api.AlmacenResponseDTO).nombre ?? 'Almacén'); });
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede'); });
  }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  get esAdmin(): boolean {
    return this.tokens.session()?.rol === 'ROLE_ADMIN';
  }

  get tabsVisibles(): TabItem[] {
    return this.esAdmin ? this.tabs : this.tabs.filter((item) => item.id === 'movimientos' || item.id === 'saldos');
  }

  get textoFiltros(): string {
    const filtros = this.filtros.getRawValue();
    const partes = [filtros.desde ? `desde ${filtros.desde}` : '', filtros.hasta ? `hasta ${filtros.hasta}` : ''].filter(Boolean);
    return partes.join(' ');
  }

  get columns(): ColumnDef[] {
    if (this.tab === 'saldos') return [
      { key: 'productoNombre', label: 'Producto' }, { key: 'codigoSku', label: 'SKU' }, { key: 'almacenNombre', label: 'Almacén' },
      { key: 'cantidad', label: 'Cantidad', kind: 'number', align: 'end' }, { key: 'bajoMinimo', label: 'Bajo mínimo', kind: 'bool' },
    ];
    if (this.tab === 'valorizacion') return [
      { key: 'productoNombre', label: 'Producto' }, { key: 'cantidad', label: 'Cantidad', kind: 'number', align: 'end' },
      { key: 'costoUnitario', label: 'Precio compra', kind: 'money', align: 'end' },
      { key: 'precioVenta', label: 'Precio venta', kind: 'money', align: 'end' },
      { key: 'gananciaEstimada', label: 'Ganancia', kind: 'money', align: 'end' },
      { key: 'valorTotal', label: 'Valor', kind: 'money', align: 'end' },
    ];
    if (this.tab === 'mermas') return [
      { key: 'fecha', label: 'Fecha', kind: 'date' }, { key: 'productoNombre', label: 'Producto' }, { key: 'almacenNombre', label: 'Almacén' },
      { key: 'cantidad', label: 'Cantidad', kind: 'number', align: 'end' }, { key: 'costoUnitario', label: 'Costo', kind: 'money', align: 'end' },
    ];
    return [
      { key: 'createdAt', label: 'Fecha', kind: 'date' }, { key: 'productoNombre', label: 'Producto' }, { key: 'almacenNombre', label: 'Almacén' },
      { key: 'tipoMovimiento', label: 'Movimiento', kind: 'badge', badge: { ENTRADA: 'success', SALIDA: 'danger' } },
      { key: 'tipoReferencia', label: 'Origen', kind: 'badge' }, { key: 'cantidadMovida', label: 'Cantidad', kind: 'number', align: 'end' },
      { key: 'costoUnitario', label: 'Precio compra', kind: 'money', align: 'end' },
      { key: 'precioVenta', label: 'Precio venta', kind: 'money', align: 'end' },
      { key: 'margenUnitario', label: 'Margen', kind: 'money', align: 'end' },
      { key: 'stockResultante', label: 'Saldo', kind: 'number', align: 'end' },
    ];
  }

  cargar(): void {
    const filtros = this.filtros.getRawValue();
    if (this.tab === 'saldos') {
      this.api.GetApiV1KardexSaldos(this.state.query({ sedeId: filtros.sedeId, busqueda: filtros.busqueda })).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
        next: (page) => this.state.apply(page as { content?: Record<string, unknown>[] }),
        error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
      });
      return;
    }
    if (this.tab === 'valorizacion') {
      this.api.GetApiV1KardexValorizacion({ sedeId: filtros.sedeId }).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
        next: (data) => { this.valorizacion = data; this.state.loading = false; this.state.rows = (data.detalle ?? []) as Record<string, unknown>[]; this.state.totalElements = this.state.rows.length; this.state.totalPages = this.state.rows.length ? 1 : 0; this.state.error = null; },
        error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
      });
      return;
    }
    if (this.tab === 'mermas') {
      this.api.GetApiV1KardexMermas(this.state.query(filtros)).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
        next: (data) => { this.mermas = data; this.state.loading = false; this.state.rows = (data.detalle ?? []) as unknown as Record<string, unknown>[]; this.state.totalElements = data.totalElementos ?? this.state.rows.length; this.state.totalPages = data.totalPaginas ?? 1; this.state.error = null; },
        error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
      });
      return;
    }
    this.api.GetApiV1KardexMovimientos(this.state.query(filtros)).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
      next: (page) => this.state.apply(page as { content?: Record<string, unknown>[] }),
      error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
    });
  }
  cambiarTab(id: string): void {
    if (!this.esAdmin && (id === 'mermas' || id === 'valorizacion')) return;
    this.tab = id;
    this.state.page = 0;
    this.cargar();
  }
  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset(); this.filtrar(); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }
  abrir(): void { this.errorFormulario = null; this.form.reset({ productoId: '', almacenId: '', tipoMovimiento: 'ENTRADA', tipoReferencia: 'AJUSTE', cantidadMovida: 1, observacion: '' }); this.modal = true; }
  guardar(): void {
    if (this.guardando) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const body: Api.KardexMovimientoRequestDTO = {
      producto: { id: value.productoId }, almacen: { id: value.almacenId }, tipoMovimiento: value.tipoMovimiento,
      tipoReferencia: value.tipoReferencia, cantidadMovida: Number(value.cantidadMovida), observacion: value.observacion.trim() || undefined,
    };
    this.guardando = true;
    this.api.PostApiV1KardexMovimientos({}, body).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.guardando = false;
        this.modal = false;
        this.form.reset({ productoId: '', almacenId: '', tipoMovimiento: 'ENTRADA', tipoReferencia: 'AJUSTE', cantidadMovida: 1, observacion: '' });
        this.tab = 'movimientos';
        this.state.page = 0;
        this.omitirRevision = this.inventario.revision() + 1;
        this.inventario.marcarCambio();
        this.toast.success('Movimiento registrado.');
        this.cargar();
      },
      error: (error: unknown) => { this.guardando = false; this.errorFormulario = problemText(error); this.toast.error(problemText(error)); },
    });
  }
}

export const KARDEX_ROUTES: Routes = [
  { path: '', component: KardexPageComponent, title: 'Kardex', data: { section: 'Inventario', breadcrumb: 'Kardex' } },
];
