import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { FichaProductoComponent } from '../components/ficha-producto.component';
import { PortalApi, mensajeError } from '../core/portal.api';
import { CategoriaPublica, ProductoPublico } from '../core/models';

@Component({
  selector: 'app-catalogo',
  imports: [ReactiveFormsModule, FichaProductoComponent],
  templateUrl: './catalogo.component.html',
})
export class CatalogoComponent implements OnInit {
  private readonly api = inject(PortalApi);
  private readonly fb = inject(FormBuilder);
  readonly categorias = signal<CategoriaPublica[]>([]);
  readonly productos = signal<ProductoPublico[]>([]);
  readonly pagina = signal(0);
  readonly paginas = signal(0);
  readonly total = signal(0);
  readonly categoriaId = signal('');
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly busqueda = this.fb.nonNullable.control('');

  ngOnInit(): void {
    this.api.categorias().subscribe({ next: (lista) => this.categorias.set(lista), error: () => undefined });
    this.cargar();
  }

  filtrarCategoria(id: string): void {
    this.categoriaId.set(id);
    this.pagina.set(0);
    this.cargar();
  }

  buscar(): void {
    this.pagina.set(0);
    this.cargar();
  }

  mover(delta: number): void {
    const siguiente = this.pagina() + delta;
    if (siguiente < 0 || siguiente >= this.paginas()) return;
    this.pagina.set(siguiente);
    this.cargar();
  }

  private cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.api.productos({
      categoriaId: this.categoriaId() || undefined,
      q: this.busqueda.value.trim() || undefined,
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
        this.error.set(mensajeError(error));
      },
    });
  }
}
