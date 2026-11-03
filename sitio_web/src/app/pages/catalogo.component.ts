import { Component, DestroyRef, HostListener, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { FichaProductoComponent } from '../components/ficha-producto.component';
import { CategoriaPublica, ProductoPublico } from '../core/models';
import { PortalApi, mensajeError } from '../core/portal.api';

@Component({
  selector: 'app-catalogo',
  imports: [FichaProductoComponent],
  templateUrl: './catalogo.component.html',
})
export class CatalogoComponent implements OnInit {
  private readonly api = inject(PortalApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly categorias = signal<CategoriaPublica[]>([]);
  readonly productos = signal<ProductoPublico[]>([]);
  readonly pagina = signal(0);
  readonly paginas = signal(0);
  readonly total = signal(0);
  readonly categoriaId = signal('');
  readonly vista = signal('');
  readonly q = signal('');
  readonly orden = signal<'reciente' | 'precio' | 'nombre'>('reciente');
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly titulo = computed(() => {
    if (this.q()) return 'Resultados';
    if (this.vista() === 'destacados') return 'Destacados';
    const id = this.categoriaId();
    if (!id) return 'Todos los productos';
    return this.categorias().find((categoria) => categoria.id === id)?.nombre ?? 'Colección';
  });
  readonly visibles = computed(() => {
    const lista = [...this.productos()];
    if (this.orden() === 'precio') lista.sort((a, b) => a.precioPublico - b.precioPublico);
    if (this.orden() === 'nombre') lista.sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
    return lista;
  });

  ngOnInit(): void {
    this.api.categorias().subscribe({ next: (lista) => this.categorias.set(lista), error: () => undefined });
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.categoriaId.set(params.get('categoria') ?? '');
      this.vista.set(params.get('vista') ?? '');
      this.q.set(params.get('q') ?? '');
      this.pagina.set(Number(params.get('page') ?? '0') || 0);
      this.cargar();
    });
  }

  ordenar(valor: string): void {
    if (valor === 'precio' || valor === 'nombre' || valor === 'reciente') this.orden.set(valor);
  }

  @HostListener('document:visibilitychange')
  alVolver(): void {
    if (document.visibilityState === 'visible') this.cargar(true);
  }

  mover(delta: number): void {
    const siguiente = this.pagina() + delta;
    if (siguiente < 0 || siguiente >= this.paginas()) return;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page: siguiente || null },
      queryParamsHandling: 'merge',
    });
  }

  private cargar(silencioso = false): void {
    if (!silencioso) {
      this.cargando.set(true);
      this.error.set('');
    }
    if (this.vista() === 'destacados' && !this.q() && !this.categoriaId()) {
      this.api.destacados().subscribe({
        next: (lista) => {
          this.productos.set(lista);
          this.paginas.set(1);
          this.total.set(lista.length);
          this.cargando.set(false);
        },
        error: (error: unknown) => {
          this.cargando.set(false);
          if (!silencioso || !this.productos().length) this.error.set(mensajeError(error));
        },
      });
      return;
    }
    this.api.productos({
      categoriaId: this.categoriaId() || undefined,
      q: this.q().trim() || undefined,
      page: this.pagina(),
    }).subscribe({
      next: (pagina) => {
        this.productos.set(pagina.content ?? []);
        this.paginas.set(pagina.totalPages ?? 0);
        this.total.set(pagina.totalElements ?? 0);
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.cargando.set(false);
        if (!silencioso || !this.productos().length) this.error.set(mensajeError(error));
      },
    });
  }
}
