import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, effect, inject, untracked, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { FilterBarComponent } from '../../../shared/components/filter-bar/filter-bar.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SelectComponent } from '../../../shared/components/select/select.component';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { TabsComponent } from '../../../shared/components/tabs/tabs.component';
import { ColumnDef, SelectOption, TabItem } from '../../../shared/models/ui.models';
import { PenPipe } from '../../../shared/pipes/format.pipes';
import { InventarioSyncService } from '../../../shared/services/inventario-sync.service';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { ignoreStale, PageState } from '../../../shared/utils/page-state';
import { StockApiService } from '../data-access/stock.api';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-stock-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, FormFieldComponent, SelectComponent, StatCardComponent, TabsComponent, PenPipe],
  templateUrl: './stock-page.component.html',
  styleUrls: ['./stock-page.component.css'],
})
export class StockPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly tabs: TabItem[] = [
    { id: 'consolidado', label: 'Consolidado' },
    { id: 'bajo', label: 'Bajo mínimo' },
    { id: 'ajustes', label: 'Ajustes' },
    { id: 'transferencias', label: 'Transferencias' },
  ];
  tab = 'consolidado';
  readonly state = new PageState<Record<string, unknown>>();
  dashboard: Api.StockDashboardDTO | null = null;
  productos: SelectOption[] = [];
  almacenes: SelectOption[] = [];
  sedes: SelectOption[] = [];
  readonly filtros = this.fb.nonNullable.group({ busqueda: [''], sedeId: [''], almacenId: [''], productoId: [''] });
  readonly ajuste = this.fb.nonNullable.group({
    productoId: ['', Validators.required], almacenId: ['', Validators.required],
    cantidad: [0, [Validators.required]], motivo: ['', [Validators.required, Validators.maxLength(255)]], justificacion: ['', Validators.maxLength(2000)],
  });
  readonly transferencia = this.fb.nonNullable.group({
    productoId: ['', Validators.required], almacenOrigenId: ['', Validators.required], almacenDestinoId: ['', Validators.required],
    cantidad: [1, [Validators.required, Validators.min(0.0001)]], motivo: ['', Validators.maxLength(255)], referenciaDocumento: ['', Validators.maxLength(100)],
  });
  modal: 'ajuste' | 'transferencia' | null = null;
  guardando = false;
  errorFormulario: string | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: StockApiService, private readonly lookup: LookupService, private readonly inventario: InventarioSyncService, private readonly toast: ToastService) {
    effect(() => {
      const revision = this.inventario.revision();
      untracked(() => {
        if (!revision) return;
        this.cargar();
        this.api.GetApiV1StockDashboard().pipe(takeUntil(this.destroy$)).subscribe({ next: (data) => { this.dashboard = data; }, error: () => { this.dashboard = null; } });
      });
    });
  }
  ngOnInit(): void {
    this.cargar();
    this.lookup.loadProductos().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.productos = this.lookup.options(rows, (row) => (row as Api.ProductoResponseDTO).nombre ?? 'Producto'); });
    this.lookup.loadAlmacenes().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.almacenes = this.lookup.options(rows, (row) => (row as Api.AlmacenResponseDTO).nombre ?? 'Almacén'); });
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede'); });
    this.api.GetApiV1StockDashboard().pipe(takeUntil(this.destroy$)).subscribe({ next: (data) => { this.dashboard = data; }, error: () => { this.dashboard = null; } });
  }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  get columns(): ColumnDef[] {
    if (this.tab === 'bajo') return [
      { key: 'productoNombre', label: 'Producto' }, { key: 'codigoSku', label: 'SKU' }, { key: 'almacenNombre', label: 'Almacén' },
      { key: 'cantidad', label: 'Cantidad', kind: 'number', align: 'end' }, { key: 'stockMinimo', label: 'Mínimo', kind: 'number', align: 'end' },
    ];
    if (this.tab === 'ajustes') return [
      { key: 'createdAt', label: 'Fecha', kind: 'date' }, { key: 'productoNombre', label: 'Producto' }, { key: 'almacenNombre', label: 'Almacén' },
      { key: 'cantidad', label: 'Cantidad', kind: 'number', align: 'end' }, { key: 'motivo', label: 'Motivo' }, { key: 'tipoMovimiento', label: 'Tipo', kind: 'badge', badge: { ENTRADA: 'success', SALIDA: 'danger' } },
    ];
    if (this.tab === 'transferencias') return [
      { key: 'createdAt', label: 'Fecha', kind: 'date' }, { key: 'productoNombre', label: 'Producto' }, { key: 'origen.almacenNombre', label: 'Origen' },
      { key: 'destino.almacenNombre', label: 'Destino' }, { key: 'cantidad', label: 'Cantidad', kind: 'number', align: 'end' },
    ];
    return [
      { key: 'productoNombre', label: 'Producto' }, { key: 'codigoSku', label: 'SKU' }, { key: 'stockTotal', label: 'Stock', kind: 'number', align: 'end' },
      { key: 'stockMinimoTotal', label: 'Mínimo', kind: 'number', align: 'end' }, { key: 'almacenesBajoMinimo', label: 'Alertas', kind: 'number', align: 'end' }, { key: 'unidad', label: 'Unidad' },
    ];
  }

  cargar(): void {
    const filtros = this.filtros.getRawValue();
    const done = { next: (page: { content?: unknown[]; totalElements?: number; totalPages?: number; number?: number }) => this.state.apply(page as { content?: Record<string, unknown>[] }), error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)) };
    if (this.tab === 'bajo') this.api.GetApiV1StockBajoStock(this.state.query(filtros)).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe(done);
    else if (this.tab === 'ajustes') this.api.GetApiV1StockAjustes(this.state.query(filtros)).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe(done);
    else if (this.tab === 'transferencias') this.api.GetApiV1StockTransferencias(this.state.query(filtros)).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe(done);
    else this.api.GetApiV1StockConsolidado(this.state.query({ sedeId: filtros.sedeId, busqueda: filtros.busqueda })).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe(done);
  }
  cambiarTab(id: string): void { this.tab = id; this.state.page = 0; this.cargar(); }
  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset(); this.filtrar(); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }
  abrirAjuste(): void { this.errorFormulario = null; this.ajuste.reset({ productoId: '', almacenId: '', cantidad: 0, motivo: '', justificacion: '' }); this.modal = 'ajuste'; }
  abrirTransferencia(): void { this.errorFormulario = null; this.transferencia.reset({ productoId: '', almacenOrigenId: '', almacenDestinoId: '', cantidad: 1, motivo: '', referenciaDocumento: '' }); this.modal = 'transferencia'; }
  guardarAjuste(): void {
    this.ajuste.markAllAsTouched();
    if (this.ajuste.invalid) return;
    const value = this.ajuste.getRawValue();
    this.guardando = true;
    this.api.PostApiV1StockAjuste({}, { productoId: value.productoId, almacenId: value.almacenId, cantidad: Number(value.cantidad), motivo: value.motivo.trim(), justificacion: value.justificacion.trim() || undefined }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = null; this.inventario.marcarCambio(); this.toast.success('Ajuste registrado.'); this.tab = 'ajustes'; this.cargar(); },
      error: (error: unknown) => { this.guardando = false; this.errorFormulario = problemText(error); },
    });
  }
  guardarTransferencia(): void {
    this.transferencia.markAllAsTouched();
    if (this.transferencia.invalid) return;
    const value = this.transferencia.getRawValue();
    if (value.almacenOrigenId === value.almacenDestinoId) { this.errorFormulario = 'El origen y el destino deben ser distintos.'; return; }
    this.guardando = true;
    this.api.PostApiV1StockTransferencia({}, { productoId: value.productoId, almacenOrigenId: value.almacenOrigenId, almacenDestinoId: value.almacenDestinoId, cantidad: Number(value.cantidad), motivo: value.motivo.trim() || undefined, referenciaDocumento: value.referenciaDocumento.trim() || undefined }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = null; this.inventario.marcarCambio(); this.toast.success('Transferencia registrada.'); this.tab = 'transferencias'; this.cargar(); },
      error: (error: unknown) => { this.guardando = false; this.errorFormulario = problemText(error); },
    });
  }
  errorAjuste(nombre: 'productoId' | 'almacenId' | 'cantidad' | 'motivo'): string | null { return fieldError(this.ajuste.controls[nombre]); }
}

export const STOCK_ROUTES: Routes = [
  { path: '', component: StockPageComponent, title: 'Stock', data: { section: 'Inventario', breadcrumb: 'Stock' } },
];
