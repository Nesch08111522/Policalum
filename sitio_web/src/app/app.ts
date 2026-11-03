import { AsyncPipe } from '@angular/common';
import { Component, ElementRef, OnDestroy, ViewChild, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Subscription, filter } from 'rxjs';
import { AvisoService } from './core/aviso.service';
import { CarritoService } from './core/carrito.service';
import { CmsStore } from './core/cms.store';
import { EmpresaStore } from './core/empresa.store';
import { CategoriaPublica, ProductoPublico, SedePublica } from './core/models';
import { PortalApi, media, pen } from './core/portal.api';
import { TemaService } from './core/tema.service';
import { AsistenteComponent } from './components/asistente.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AsyncPipe, AsistenteComponent],
  templateUrl: './app.html',
})
export class App implements OnDestroy {
  readonly carrito = inject(CarritoService);
  readonly marca = inject(EmpresaStore);
  readonly cms = inject(CmsStore);
  readonly avisos = inject(AvisoService);
  readonly tema = inject(TemaService);
  private readonly api = inject(PortalApi);
  private readonly router = inject(Router);
  private readonly navegacion: Subscription;

  readonly media = media;
  readonly pen = pen;
  readonly menu = signal(false);
  readonly preguntarCarrito = signal(false);
  readonly anio = new Date().getFullYear();
  readonly ruta = signal(this.router.url);
  readonly categorias = signal<CategoriaPublica[]>([]);
  readonly sedes = signal<SedePublica[]>([]);
  readonly vitrina = signal<ProductoPublico[]>([]);
  readonly sugerencias = signal<ProductoPublico[]>([]);
  private espera = 0;
  @ViewChild('caja') caja?: ElementRef<HTMLInputElement>;

  constructor() {
    this.navegacion = this.router.events.pipe(
      filter((evento): evento is NavigationEnd => evento instanceof NavigationEnd),
    ).subscribe((evento) => {
      this.ruta.set(evento.urlAfterRedirects);
      this.menu.set(false);
    });
    this.api.categorias().subscribe({ next: (lista) => this.categorias.set(lista), error: () => undefined });
    this.api.sedes().subscribe({ next: (lista) => this.sedes.set(lista), error: () => undefined });
    this.preguntarCarrito.set(this.carrito.deOtroDia());
    this.api.productos({ page: 0 }).subscribe({
      next: (pagina) => this.vitrina.set(pagina.content ?? []),
      error: () => undefined,
    });
  }

  ngOnDestroy(): void {
    this.navegacion.unsubscribe();
  }

  consulta(): URLSearchParams {
    return new URLSearchParams(this.ruta().split('?')[1] ?? '');
  }

  conservarCarrito(): void {
    this.carrito.confirmarDia();
    this.preguntarCarrito.set(false);
  }

  descartarCarrito(): void {
    this.carrito.vaciar();
    this.preguntarCarrito.set(false);
  }

  esCatalogo(): boolean {
    const path = this.ruta().split('?')[0];
    return path === '/' || path === '/catalogo';
  }

  categoriaActiva(): string {
    return this.consulta().get('categoria') ?? '';
  }

  vistaActiva(): string {
    return this.consulta().get('vista') ?? '';
  }

  sugerir(valor: string): void {
    window.clearTimeout(this.espera);
    const q = valor.trim();
    if (q.length < 2) {
      this.sugerencias.set([]);
      return;
    }
    this.espera = window.setTimeout(() => {
      this.api.productos({ q, page: 0, size: 6 }).subscribe({
        next: (pagina) => this.sugerencias.set(pagina.content ?? []),
        error: () => this.sugerencias.set([]),
      });
    }, 220);
  }

  elegirProducto(id: string): void {
    this.sugerencias.set([]);
    if (this.caja) this.caja.nativeElement.value = '';
    this.router.navigate(['/catalogo', id]);
  }

  irBusqueda(texto: string): void {
    const q = texto.trim();
    this.sugerencias.set([]);
    if (!q) {
      this.router.navigate(['/catalogo']);
      return;
    }
    this.api.productos({ q, page: 0, size: 8 }).subscribe({
      next: (pagina) => {
        const lista = pagina.content ?? [];
        const exacto = lista.find((producto) =>
          producto.nombre.toLowerCase() === q.toLowerCase() || (producto.codigoSku ?? '').toLowerCase() === q.toLowerCase());
        const destino = exacto ?? (lista.length === 1 ? lista[0] : null);
        if (destino) this.router.navigate(['/catalogo', destino.id]);
        else this.router.navigate(['/catalogo'], { queryParams: { q } });
      },
      error: () => this.router.navigate(['/catalogo'], { queryParams: { q } }),
    });
  }

  enfocarBusqueda(): void {
    this.caja?.nativeElement.focus();
  }

  sumar(producto: ProductoPublico, evento: Event): void {
    evento.preventDefault();
    evento.stopPropagation();
    this.carrito.agregar({
      productoId: producto.id,
      nombre: producto.nombre,
      precio: producto.precioPublico,
      cantidad: 1,
      imagenUrl: producto.imagenUrl,
      unidad: producto.unidad || 'unidad',
    });
  }

}
