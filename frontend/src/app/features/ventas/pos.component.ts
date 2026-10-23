import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, of, Subject } from 'rxjs';
import { catchError, takeUntil } from 'rxjs/operators';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { PenPipe } from '../../shared/pipes/format.pipes';
import { CajaSessionService } from '../../shared/services/caja-session.service';
import { InventarioSyncService } from '../../shared/services/inventario-sync.service';
import { LookupService } from '../../shared/services/lookup.service';
import { CotizacionesApiService } from '../cotizaciones/data-access/cotizaciones.api';
import { FacturacionElectronicaApiService } from '../facturacion-electronica/data-access/facturacion-electronica.api';
import { PreformasApiService } from '../preformas/data-access/preformas.api';
import { Almacen, Cliente, Fraccion, Producto, Sede, Servicio, Turno, VentasApiService } from './ventas-api.service';

type Catalogo = 'productos' | 'servicios' | 'favoritos';
type Comprobante = 'BOLETA' | 'FACTURA' | 'TICKET';
type Metodo = 'EFECTIVO' | 'TARJETA' | 'YAPE' | 'PLIN' | 'TRANSFERENCIA' | 'SIP_AGORA';

interface Linea {
  tipo: 'PRODUCTO' | 'SERVICIO';
  id: string;
  nombre: string;
  precio: number;
  cantidad: number;
  fraccion?: Fraccion;
}

const FAVORITOS = 'erp-pos-favoritos';

function mensajeError(error: unknown): string {
  const http = error as { error?: { message?: string; detail?: string }; message?: string };
  return http?.error?.message || http?.error?.detail || http?.message || 'No se pudo completar la operación.';
}

function redondear(valor: number, decimales = 2): number {
  const factor = 10 ** decimales;
  return Math.round((valor + Number.EPSILON) * factor) / factor;
}

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-pos',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent, PenPipe],
  templateUrl: './pos.component.html',
  styleUrl: './pos.component.css',
})
export class PosComponent implements OnInit, OnDestroy {
  @ViewChild('buscador') buscador?: ElementRef<HTMLInputElement>;

  productos: Producto[] = [];
  servicios: Servicio[] = [];
  clientes: Cliente[] = [];
  sedes: Sede[] = [];
  almacenes: Almacen[] = [];
  carrito: Linea[] = [];
  fracciones: Record<string, Fraccion[]> = {};
  stock: Record<string, number> = {};
  favoritos = new Set<string>();

  sedeId = '';
  almacenId = '';
  clienteId = '';
  turno?: Turno;
  montoInicial = 0;
  nombreTurno = '';
  busqueda = '';
  mensaje = '';
  error = '';
  catalogo: Catalogo = 'productos';
  comprobante: Comprobante = 'BOLETA';
  metodo: Metodo = 'EFECTIVO';
  referencia = '';
  guardando = false;
  rucEmpresa = '';
  rucEmpresa20 = false;

  readonly metodos: Array<{ id: Metodo; label: string; icon: string }> = [
    { id: 'EFECTIVO', label: 'Efectivo', icon: 'cash' },
    { id: 'TARJETA', label: 'Tarjeta', icon: 'card' },
    { id: 'YAPE', label: 'Yape', icon: 'cash' },
    { id: 'PLIN', label: 'Plin', icon: 'cash' },
    { id: 'TRANSFERENCIA', label: 'Transferencia', icon: 'building' },
    { id: 'SIP_AGORA', label: 'Agora', icon: 'cash' },
  ];

  private readonly destroy$ = new Subject<void>();
  private buscarTimer?: ReturnType<typeof setTimeout>;

  constructor(
    private readonly api: VentasApiService,
    private readonly caja: CajaSessionService,
    private readonly inventario: InventarioSyncService,
    private readonly lookup: LookupService,
    private readonly preformas: PreformasApiService,
    private readonly cotizaciones: CotizacionesApiService,
    private readonly sunat: FacturacionElectronicaApiService,
  ) {}

  ngOnInit(): void {
    const abierto = this.caja.turno();
    if (abierto?.id && abierto.estado !== 'CERRADO') {
      this.turno = {
        id: abierto.id,
        montoInicial: abierto.montoInicial ?? 0,
        estado: 'ABIERTO',
        fechaApertura: abierto.fechaApertura ?? '',
        observaciones: abierto.observaciones,
        sede: abierto.sede?.id ? { id: abierto.sede.id } : undefined,
      };
    }
    this.favoritos = new Set(this.leerFavoritos());
    this.api.sedes().pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => {
        this.sedes = page.content.filter((sede) => sede.activa);
        if (this.sedes[0]) this.seleccionarSede(this.sedes[0].id);
      },
      error: () => { this.error = 'No se pudieron cargar las sedes.'; },
    });
    this.cargarCatalogos();
    this.api.clientes().pipe(takeUntil(this.destroy$)).subscribe((page) => { this.clientes = page.content; });
    this.sunat.panel().pipe(takeUntil(this.destroy$), catchError(() => of(null))).subscribe((panel) => {
      this.rucEmpresa = panel?.ruc?.trim() ?? '';
      this.rucEmpresa20 = panel?.rucEmpresa20 === true;
    });
  }

  ngOnDestroy(): void {
    clearTimeout(this.buscarTimer);
    this.destroy$.next();
    this.destroy$.complete();
  }

  @HostListener('window:keydown', ['$event'])
  atajo(event: KeyboardEvent): void {
    const escribe = event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement || event.target instanceof HTMLSelectElement;
    if (event.key === 'F2') {
      event.preventDefault();
      this.buscador?.nativeElement.focus();
    }
    if (event.key === 'F10' && !escribe) {
      event.preventDefault();
      this.cobrar();
    }
  }

  get cajaTexto(): string {
    const nombre = this.turno?.observaciones?.trim();
    return nombre ? `Caja abierta: ${nombre}` : 'Caja abierta';
  }

  get clienteSeleccionado(): Cliente | undefined {
    return this.clientes.find((cliente) => cliente.id === this.clienteId);
  }

  get productosVisibles(): Producto[] {
    if (this.catalogo !== 'favoritos') return this.productos;
    return this.productos.filter((producto) => this.favoritos.has(producto.id));
  }

  get serviciosVisibles(): Servicio[] {
    if (this.catalogo === 'productos') return [];
    if (this.catalogo === 'favoritos') return this.servicios.filter((servicio) => this.favoritos.has(servicio.id));
    return this.servicios;
  }

  get subtotal(): number {
    return redondear(this.carrito.reduce((suma, linea) => suma + this.importe(linea).neto, 0));
  }

  get igv(): number {
    return redondear(this.carrito.reduce((suma, linea) => suma + this.importe(linea).igv, 0));
  }

  get total(): number {
    return redondear(this.carrito.reduce((suma, linea) => suma + this.importe(linea).total, 0));
  }

  buscar(): void {
    clearTimeout(this.buscarTimer);
    this.buscarTimer = setTimeout(() => this.cargarCatalogos(), 250);
  }

  cargarCatalogos(): void {
    this.api.productos(this.busqueda).pipe(takeUntil(this.destroy$)).subscribe((page) => {
      this.productos = page.content;
      this.cargarFracciones(page.content.filter((producto) => producto.esFraccionable));
    });
    this.api.servicios(this.busqueda).pipe(takeUntil(this.destroy$)).subscribe((page) => { this.servicios = page.content; });
  }

  seleccionarSede(id: string): void {
    this.sedeId = id;
    this.almacenId = '';
    this.stock = {};
    if (!id) {
      this.almacenes = [];
      return;
    }
    this.api.almacenes(id).pipe(takeUntil(this.destroy$)).subscribe((rows) => {
      this.almacenes = rows.filter((almacen) => almacen.activo);
      if (this.almacenes[0]) this.seleccionarAlmacen(this.almacenes[0].id);
    });
  }

  seleccionarAlmacen(id: string): void {
    this.almacenId = id;
    this.stock = {};
    if (!id) return;
    this.api.stockAlmacen(id).pipe(takeUntil(this.destroy$), catchError(() => of(null))).subscribe((page) => {
      const siguiente: Record<string, number> = {};
      for (const fila of page?.content ?? []) {
        const productoId = fila.producto?.id;
        if (productoId) siguiente[productoId] = Number(fila.cantidad ?? 0);
      }
      this.stock = siguiente;
    });
  }

  fraccionesDe(producto: Producto): Fraccion[] {
    return this.fracciones[producto.id] ?? [];
  }

  stockDe(producto: Producto): string {
    if (!this.almacenId || this.stock[producto.id] === undefined) return '—';
    return `${this.stock[producto.id]} ${producto.unidad || 'und'}`;
  }

  mermaDe(producto: Producto): string {
    const fracciones = this.fraccionesDe(producto);
    if (!fracciones.length) return '';
    const menor = [...fracciones].sort((a, b) => Number(a.factorDescuentoKardex ?? 1) - Number(b.factorDescuentoKardex ?? 1))[0];
    return menor.nombreFraccion || '';
  }

  precioDe(producto: Producto): number {
    const fraccion = this.fraccionesDe(producto)[0];
    return producto.esFraccionable && fraccion ? fraccion.precioVenta : producto.precioVentaBase;
  }

  inicial(nombre: string): string {
    return (nombre || 'P').trim().slice(0, 1).toUpperCase();
  }

  esFavorito(id: string): boolean {
    return this.favoritos.has(id);
  }

  alternarFavorito(id: string): void {
    const siguiente = new Set(this.favoritos);
    if (siguiente.has(id)) siguiente.delete(id);
    else siguiente.add(id);
    this.favoritos = siguiente;
    localStorage.setItem(FAVORITOS, JSON.stringify([...siguiente]));
  }

  agregarProducto(producto: Producto, fraccion?: Fraccion): void {
    if (producto.esFraccionable && !fraccion) {
      const disponibles = this.fraccionesDe(producto);
      if (!disponibles.length) {
        this.error = 'El producto fraccionable no tiene fracciones activas.';
        return;
      }
      fraccion = disponibles[0];
    }
    this.agregar({
      tipo: 'PRODUCTO',
      id: producto.id,
      nombre: fraccion ? `${producto.nombre} (${fraccion.nombreFraccion})` : producto.nombre,
      precio: fraccion ? fraccion.precioVenta : producto.precioVentaBase,
      cantidad: 1,
      fraccion,
    });
  }

  agregarServicio(servicio: Servicio): void {
    this.agregar({ tipo: 'SERVICIO', id: servicio.id, nombre: servicio.nombre, precio: servicio.precio, cantidad: 1 });
  }

  ajustar(index: number, delta: number): void {
    const actual = this.carrito[index];
    if (!actual) return;
    this.fijarCantidad(index, redondear(actual.cantidad + delta, 4));
  }

  fijarCantidad(index: number, valor: number): void {
    const cantidad = Number(valor);
    if (!Number.isFinite(cantidad) || cantidad <= 0) return;
    const next = [...this.carrito];
    next[index] = { ...next[index], cantidad };
    this.carrito = next;
  }

  quitar(index: number): void {
    this.carrito = this.carrito.filter((_, i) => i !== index);
  }

  importe(linea: Linea): { neto: number; igv: number; total: number } {
    const total = redondear(linea.precio * linea.cantidad);
    const neto = total / 1.18;
    return { neto, igv: total - neto, total };
  }

  abrirCaja(): void {
    this.error = '';
    if (!this.sedeId || this.montoInicial < 0) {
      this.error = 'Elige una sede y un monto inicial válido.';
      return;
    }
    this.api.abrirCaja({ sedeId: this.sedeId, montoInicial: this.montoInicial, observaciones: this.nombreTurno.trim() || undefined }).subscribe({
      next: (turno) => {
        this.turno = { ...turno, sede: turno.sede ?? { id: this.sedeId } };
        this.caja.recordar({
          id: turno.id,
          montoInicial: turno.montoInicial,
          estado: turno.estado,
          fechaApertura: turno.fechaApertura,
          observaciones: turno.observaciones,
          sede: { id: this.sedeId },
        });
        this.mensaje = 'Caja abierta correctamente.';
      },
      error: (error: unknown) => { this.error = mensajeError(error); },
    });
  }

  cobrar(): void {
    this.error = '';
    this.mensaje = '';
    if (this.guardando) return;
    if (!this.turno || !this.almacenId || !this.carrito.length || !this.sedeId) {
      this.error = 'Abre la caja, elige el almacén y agrega al menos una línea.';
      return;
    }
    if (this.turno.sede?.id && this.turno.sede.id !== this.sedeId) {
      this.error = 'La caja abierta pertenece a otra sede.';
      return;
    }
    const total = this.total;
    const data = {
      sedeId: this.sedeId,
      almacenId: this.almacenId,
      clienteId: this.clienteId || null,
      turnoCajaId: this.turno.id,
      tipoComprobante: this.comprobante,
      canalVenta: 'LOCAL',
      lineas: this.lineasVenta(),
      pagos: [{ metodoPago: this.metodo, monto: total, referencia: this.referencia.trim() || undefined }],
    };
    this.guardando = true;
    this.api.registrarVenta(data).subscribe({
      next: (venta) => {
        this.carrito = [];
        this.referencia = '';
        this.inventario.marcarCambio();
        this.caja.tocar();
        this.lookup.invalidate('productos');
        this.cargarCatalogos();
        this.seleccionarAlmacen(this.almacenId);
        this.mensaje = `Venta ${venta.serieCorrelativo} registrada. El comprobante queda pendiente de SUNAT.`;
        this.guardando = false;
      },
      error: (error: unknown) => {
        this.error = mensajeError(error);
        this.guardando = false;
      },
    });
  }

  guardarPreforma(): void {
    if (!this.puedeGuardar()) return;
    this.guardando = true;
    this.preformas.PostApiV1Preformas({}, {
      sedeId: this.sedeId,
      clienteId: this.clienteId || undefined,
      lineas: this.lineasVenta(),
      minutosVigencia: 60,
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (documento) => {
        this.mensaje = `Preforma ${documento.codigo ?? ''} guardada. El stock no se mueve hasta confirmarla.`;
        this.guardando = false;
      },
      error: (error: unknown) => this.falloGuardado(error),
    });
  }

  guardarCotizacion(): void {
    if (!this.puedeGuardar()) return;
    this.guardando = true;
    this.cotizaciones.PostApiV1Cotizaciones({}, {
      sedeId: this.sedeId,
      clienteId: this.clienteId || undefined,
      lineas: this.lineasVenta(),
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (documento) => {
        this.mensaje = `Cotización ${documento.id ?? ''} guardada.`;
        this.guardando = false;
      },
      error: (error: unknown) => this.falloGuardado(error),
    });
  }

  private puedeGuardar(): boolean {
    this.error = '';
    this.mensaje = '';
    if (!this.sedeId || !this.carrito.length || this.guardando) {
      this.error = 'Elige la sede y agrega al menos una línea.';
      return false;
    }
    return true;
  }

  private falloGuardado(error: unknown): void {
    this.error = mensajeError(error);
    this.guardando = false;
  }

  private lineasVenta(): Array<{ productoId?: string; fraccionId?: string; servicioId?: string; cantidad: number }> {
    return this.carrito.map((linea) => linea.tipo === 'PRODUCTO'
      ? { productoId: linea.id, fraccionId: linea.fraccion?.id, cantidad: linea.cantidad }
      : { servicioId: linea.id, cantidad: linea.cantidad });
  }

  private agregar(linea: Linea): void {
    this.error = '';
    const index = this.carrito.findIndex((item) => item.tipo === linea.tipo && item.id === linea.id && item.fraccion?.id === linea.fraccion?.id);
    if (index >= 0) {
      this.ajustar(index, 1);
      return;
    }
    this.carrito = [...this.carrito, linea];
  }

  private cargarFracciones(productos: Producto[]): void {
    const pendientes = productos.filter((producto) => !this.fracciones[producto.id]);
    if (!pendientes.length) return;
    forkJoin(pendientes.map((producto) => this.api.fracciones(producto.id).pipe(catchError(() => of([] as Fraccion[])))))
      .pipe(takeUntil(this.destroy$))
      .subscribe((listas) => {
        const siguiente = { ...this.fracciones };
        pendientes.forEach((producto, index) => { siguiente[producto.id] = listas[index].filter((fraccion) => fraccion.activo !== false); });
        this.fracciones = siguiente;
      });
  }

  private leerFavoritos(): string[] {
    try {
      const raw = localStorage.getItem(FAVORITOS);
      const parsed = raw ? JSON.parse(raw) as unknown : [];
      return Array.isArray(parsed) ? parsed.filter((id): id is string => typeof id === 'string') : [];
    } catch {
      return [];
    }
  }
}
