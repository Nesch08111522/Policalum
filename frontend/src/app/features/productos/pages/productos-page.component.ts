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
import { applyServerErrors, fieldError, isProblem, problemStatus, problemText } from '../../../shared/utils/feedback';
import { PageState } from '../../../shared/utils/page-state';
import { KardexApiService } from '../../kardex/data-access/kardex.api';
import { StockApiService } from '../../stock/data-access/stock.api';
import { COLUMNAS_PRODUCTO, ProductoImportacionFila, ProductosMasivoApiService } from '../data-access/productos-masivo.api';
import { ProductosApiService } from '../data-access/productos.api';
import * as XLSX from 'xlsx';

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
    imagen: [''],
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
  menuExportar = false;
  modalImportar = false;
  importando = false;
  sobreArchivo = false;
  archivoNombre = '';
  importErrores: { fila?: number; motivo?: string }[] = [];
  importExito: string | null = null;
  modoImagen: 'archivo' | 'url' = 'archivo';
  archivoPendiente: File | null = null;
  vistaPrevia: string | null = null;
  private readonly vistas = new Map<string, string>();
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
    private readonly masivo: ProductosMasivoApiService,
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

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    for (const url of this.vistas.values()) {
      if (url.startsWith('blob:')) URL.revokeObjectURL(url);
    }
  }

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
        this.prepararVistas();
      },
      error: (error: unknown) => {
        if (token !== this.carga) return;
        const mensaje = problemText(error);
        this.state.fail(mensaje, problemStatus(error));
        this.toast.error(mensaje);
      },
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

  abrirCrear(): void {
    this.cargarCombos();
    this.editando = false;
    this.fraccionesActuales = [];
    this.errorFormulario = null;
    this.modoImagen = 'archivo';
    this.archivoPendiente = null;
    this.vistaPrevia = null;
    this.form.reset({ id: '', nombre: '', codigoSku: '', descripcion: '', categoriaId: '', proveedorId: '', tipo: 'FISICO', unidad: 'UNIDAD', precioCompra: 0, precioVentaBase: 0, activo: true, esFraccionable: false, incluyeIgv: true, imagen: '' });
    this.modal = true;
  }
  abrirEditar(item: Api.ProductoResponseDTO): void {
    this.cargarCombos();
    this.editando = true;
    this.fraccionesActuales = item.fracciones ?? [];
    this.errorFormulario = null;
    this.form.reset({
      id: item.id ?? '', nombre: item.nombre ?? '', codigoSku: item.codigoSku ?? '', descripcion: item.descripcion ?? '',
      categoriaId: item.categoria?.id ?? '', proveedorId: item.proveedor?.id ?? '', tipo: item.tipo ?? 'FISICO', unidad: item.unidad ?? 'UNIDAD',
      precioCompra: item.precioCompra ?? 0, precioVentaBase: item.precioVentaBase ?? 0, activo: item.activo ?? true, esFraccionable: item.esFraccionable ?? false, incluyeIgv: item.incluyeIgv ?? true,
      imagen: item.imagen ?? '',
    });
    this.archivoPendiente = null;
    this.modoImagen = item.imagen?.startsWith('http://') || item.imagen?.startsWith('https://') ? 'url' : 'archivo';
    this.vistaPrevia = this.modoImagen === 'url' ? item.imagen ?? null : null;
    if (this.modoImagen === 'archivo' && item.imagen) this.cargarVistaPrevia(item.imagen);
    this.modal = true;
  }
  guardar(): void {
    if (this.guardando) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    if (this.modoImagen === 'url') {
      const url = value.imagen.trim();
      if (url && !/^https?:\/\//i.test(url)) {
        this.errorFormulario = 'La URL de la imagen debe empezar por http:// o https://.';
        return;
      }
    }
    this.guardando = true;
    if (this.modoImagen === 'archivo' && this.archivoPendiente && !(this.editando && value.id)) {
      this.masivo.subirImagen(this.archivoPendiente).pipe(takeUntil(this.destroy$)).subscribe({
        next: (respuesta) => {
          this.form.controls.imagen.setValue(respuesta.ruta);
          this.archivoPendiente = null;
          this.persistir();
        },
        error: (error: unknown) => {
          this.guardando = false;
          this.errorFormulario = problemText(error);
          this.toast.error(this.errorFormulario);
        },
      });
      return;
    }
    this.persistir();
  }

  private persistir(): void {
    const value = this.form.getRawValue();
    const body = this.cuerpo(value, value.id, this.fraccionesActuales);
    const archivo = this.modoImagen === 'archivo' ? this.archivoPendiente : null;
    const request = this.editando && value.id && archivo
      ? this.masivo.actualizarConImagen(value.id, body, archivo)
      : this.editando && value.id
        ? this.api.PutApiV1ProductosId({ id: value.id }, body)
        : this.api.PostApiV1Productos({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.guardando = false;
        this.archivoPendiente = null;
        this.modal = false;
        this.lookup.invalidate('productos');
        this.inventario.marcarCambio();
        this.toast.success(this.editando ? 'Producto actualizado.' : 'Producto creado.');
        this.cargar();
      },
      error: (error: unknown) => {
        this.guardando = false;
        applyServerErrors(this.form, error);
        this.errorFormulario = problemText(error);
        this.toast.error(this.errorFormulario);
      },
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
  exportar(formato: 'xlsx' | 'csv'): void {
    this.menuExportar = false;
    const extra = this.filtros.getRawValue();
    this.masivo.exportar({
      searchTerm: extra.searchTerm,
      categoriaId: extra.categoriaId,
      tipo: extra.tipo,
      activo: extra.activo,
      esFraccionable: extra.esFraccionable,
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (filas) => {
        this.escribirArchivo(filas, formato, formato === 'xlsx' ? 'productos.xlsx' : 'productos.csv');
        this.toast.success(filas.length ? 'Catálogo exportado.' : 'El archivo se generó sin filas.');
      },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  abrirImportar(): void {
    this.menuExportar = false;
    this.importando = false;
    this.sobreArchivo = false;
    this.archivoNombre = '';
    this.importErrores = [];
    this.importExito = null;
    this.modalImportar = true;
  }

  descargarPlantilla(): void {
    this.escribirArchivo([], 'xlsx', 'plantilla-productos.xlsx');
  }

  soltarArchivo(event: DragEvent): void {
    event.preventDefault();
    this.sobreArchivo = false;
    const archivo = event.dataTransfer?.files?.[0];
    if (archivo) this.leerArchivo(archivo);
  }

  elegirArchivo(event: Event): void {
    const archivo = (event.target as HTMLInputElement).files?.[0];
    if (archivo) this.leerArchivo(archivo);
  }

  elegirImagen(event: Event): void {
    const archivo = (event.target as HTMLInputElement).files?.[0];
    if (!archivo) return;
    if (archivo.size > 2 * 1024 * 1024) {
      this.errorFormulario = 'La imagen no puede superar 2 MB.';
      return;
    }
    this.errorFormulario = null;
    this.archivoPendiente = archivo;
    const lector = new FileReader();
    lector.onload = () => { this.vistaPrevia = String(lector.result ?? ''); };
    lector.readAsDataURL(archivo);
  }

  usarModoImagen(modo: 'archivo' | 'url'): void {
    this.modoImagen = modo;
    this.archivoPendiente = null;
    if (modo === 'url') this.vistaPrevia = this.form.controls.imagen.value.trim() || null;
  }

  actualizarVistaUrl(): void {
    const url = this.form.controls.imagen.value.trim();
    this.vistaPrevia = /^https?:\/\//i.test(url) ? url : null;
  }

  vistaDe(producto: Api.ProductoResponseDTO): string | null {
    return producto.id ? this.vistas.get(producto.id) ?? null : null;
  }

  ocultarVista(producto: Api.ProductoResponseDTO): void {
    if (producto.id) this.vistas.delete(producto.id);
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

  private leerArchivo(archivo: File): void {
    const nombre = archivo.name.toLowerCase();
    if (!nombre.endsWith('.xlsx') && !nombre.endsWith('.csv')) {
      this.importErrores = [{ motivo: 'Solo se aceptan archivos .xlsx o .csv.' }];
      return;
    }
    if (archivo.size > 5 * 1024 * 1024) {
      this.importErrores = [{ motivo: 'El archivo no puede superar 5 MB.' }];
      return;
    }
    this.archivoNombre = archivo.name;
    this.importErrores = [];
    this.importExito = null;
    this.importando = true;
    archivo.arrayBuffer().then((buffer) => {
      const libro = XLSX.read(buffer, { type: 'array' });
      const hoja = libro.Sheets[libro.SheetNames[0]];
      const crudas = XLSX.utils.sheet_to_json<Record<string, unknown>>(hoja, { defval: '' });
      const filas: ProductoImportacionFila[] = [];
      const errores: { fila?: number; motivo?: string }[] = [];
      crudas.forEach((cruda, indice) => {
        const fila = this.filaImportada(cruda);
        if (!fila) return;
        const error = this.validarFilaCliente(fila);
        if (error) errores.push({ fila: indice + 1, motivo: error });
        else filas.push(fila);
      });
      if (!filas.length && !errores.length) {
        this.importando = false;
        this.importErrores = [{ motivo: 'No hay filas para importar.' }];
        return;
      }
      if (errores.length) {
        this.importando = false;
        this.importErrores = errores;
        return;
      }
      this.masivo.importar(filas).pipe(takeUntil(this.destroy$)).subscribe({
        next: (resultado) => {
          this.importando = false;
          this.importExito = `Se crearon ${resultado.creados} y se actualizaron ${resultado.actualizados}. Ninguna fila quedó a medias.`;
          this.lookup.invalidate('productos');
          this.inventario.marcarCambio();
          this.cargar();
        },
        error: (error: unknown) => {
          this.importando = false;
          this.importErrores = isProblem(error) && error.errores?.length
            ? error.errores
            : [{ motivo: problemText(error) }];
        },
      });
    }).catch(() => {
      this.importando = false;
      this.importErrores = [{ motivo: 'No se pudo leer el archivo.' }];
    });
  }

  private filaImportada(cruda: Record<string, unknown>): ProductoImportacionFila | null {
    const normalizada: Record<string, unknown> = {};
    for (const [clave, valor] of Object.entries(cruda)) {
      normalizada[this.aliasColumna(clave)] = valor;
    }
    const sku = this.texto(normalizada['codigoSku']);
    const nombre = this.texto(normalizada['nombre']);
    if (!sku && !nombre) return null;
    return {
      codigoSku: sku || undefined,
      nombre: nombre || undefined,
      descripcion: this.texto(normalizada['descripcion']) || undefined,
      categoria: this.texto(normalizada['categoria']) || undefined,
      tipo: this.texto(normalizada['tipo']).toUpperCase() || undefined,
      precioCompra: this.numero(normalizada['precioCompra']),
      precioVentaBase: this.numero(normalizada['precioVentaBase']),
      unidad: this.texto(normalizada['unidad']) || undefined,
      esFraccionable: this.bandera(normalizada['esFraccionable']),
      activo: this.bandera(normalizada['activo']),
      incluyeIgv: this.bandera(normalizada['incluyeIgv']),
      tipoAfectacion: this.texto(normalizada['tipoAfectacion']) || undefined,
      imagen: this.texto(normalizada['imagen']) || undefined,
      stockInicial: this.numero(normalizada['stockInicial']),
      almacen: this.texto(normalizada['almacen']) || undefined,
    };
  }

  private validarFilaCliente(fila: ProductoImportacionFila): string | null {
    if (!fila.codigoSku) return 'El SKU es obligatorio.';
    if (fila.tipo && !['FISICO', 'SERVICIO'].includes(fila.tipo.toUpperCase())) return 'El tipo debe ser FISICO o SERVICIO.';
    if (fila.imagen && !/^https?:\/\//i.test(fila.imagen) && !fila.imagen.startsWith('productos/')) {
      return 'La imagen debe ser una URL http(s) o una ruta productos/.';
    }
    return null;
  }

  private aliasColumna(clave: string): string {
    const limpia = clave.trim().toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    const alias: Record<string, string> = {
      sku: 'codigoSku', codigosku: 'codigoSku', codigo_sku: 'codigoSku',
      preciocompra: 'precioCompra', precio_compra: 'precioCompra',
      precioventabase: 'precioVentaBase', precio_venta_base: 'precioVentaBase', precioventa: 'precioVentaBase',
      esfraccionable: 'esFraccionable', incluyeigv: 'incluyeIgv', tipoafectacion: 'tipoAfectacion',
      stockinicial: 'stockInicial', stock_inicial: 'stockInicial',
    };
    return alias[limpia] ?? clave.trim();
  }

  private texto(valor: unknown): string {
    if (valor === null || valor === undefined) return '';
    return String(valor).trim();
  }

  private numero(valor: unknown): number | undefined {
    if (valor === null || valor === undefined || valor === '') return undefined;
    const numero = typeof valor === 'number' ? valor : Number(String(valor).replace(',', '.'));
    return Number.isFinite(numero) ? numero : undefined;
  }

  private bandera(valor: unknown): boolean | undefined {
    if (valor === null || valor === undefined || valor === '') return undefined;
    if (typeof valor === 'boolean') return valor;
    const texto = String(valor).trim().toLowerCase();
    if (['true', '1', 'si', 'sí', 's', 'yes'].includes(texto)) return true;
    if (['false', '0', 'no', 'n'].includes(texto)) return false;
    return undefined;
  }

  private escribirArchivo(filas: ProductoImportacionFila[], formato: 'xlsx' | 'csv', nombre: string): void {
    const hoja = filas.length
      ? XLSX.utils.json_to_sheet(filas, { header: [...COLUMNAS_PRODUCTO] })
      : XLSX.utils.aoa_to_sheet([[...COLUMNAS_PRODUCTO]]);
    if (formato === 'csv') {
      const csv = XLSX.utils.sheet_to_csv(hoja);
      const enlace = document.createElement('a');
      enlace.href = URL.createObjectURL(new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' }));
      enlace.download = nombre;
      enlace.click();
      URL.revokeObjectURL(enlace.href);
      return;
    }
    const libro = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(libro, hoja, 'Productos');
    XLSX.writeFile(libro, nombre);
  }

  private prepararVistas(): void {
    for (const producto of this.state.rows) {
      const imagen = producto.imagen;
      const id = producto.id;
      if (!imagen || !id || this.vistas.has(id)) continue;
      if (imagen.startsWith('http://') || imagen.startsWith('https://')) {
        this.vistas.set(id, imagen);
      } else {
        this.masivo.verImagen(imagen).pipe(takeUntil(this.destroy$)).subscribe({
          next: (blob) => this.vistas.set(id, URL.createObjectURL(blob)),
          error: () => this.vistas.delete(id),
        });
      }
    }
  }

  private cargarVistaPrevia(ruta: string): void {
    this.masivo.verImagen(ruta).pipe(takeUntil(this.destroy$)).subscribe({
      next: (blob) => { this.vistaPrevia = URL.createObjectURL(blob); },
      error: () => { this.vistaPrevia = null; },
    });
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
      imagen: value.imagen.trim() || undefined,
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
      imagen: item.imagen,
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
