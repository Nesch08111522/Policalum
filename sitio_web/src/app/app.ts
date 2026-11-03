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

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AsyncPipe],
  templateUrl: './app.html',
})
export class App implements OnDestroy {
  readonly carrito = inject(CarritoService);
  readonly marca = inject(EmpresaStore);
  readonly cms = inject(CmsStore);
  readonly avisos = inject(AvisoService);
  private readonly api = inject(PortalApi);
  private readonly router = inject(Router);
  private readonly navegacion: Subscription;

  readonly media = media;
  readonly pen = pen;
  readonly menu = signal(false);
  readonly ruta = signal(this.router.url);
  readonly categorias = signal<CategoriaPublica[]>([]);
  readonly sedes = signal<SedePublica[]>([]);
  readonly vitrina = signal<ProductoPublico[]>([]);
  readonly vitrinaVisible = signal(4);
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

  irBusqueda(texto: string): void {
    const q = texto.trim();
    this.router.navigate(['/catalogo'], { queryParams: q ? { q } : {} });
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

  masVitrina(): void {
    if (this.vitrinaVisible() >= this.vitrina().length) {
      this.router.navigate(['/catalogo']);
      return;
    }
    this.vitrinaVisible.update((cantidad) => cantidad + 4);
  }
}
