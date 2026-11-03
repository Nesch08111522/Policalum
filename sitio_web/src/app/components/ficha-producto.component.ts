import { Component, Input, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CarritoService } from '../core/carrito.service';
import { ProductoPublico } from '../core/models';
import { media, pen } from '../core/portal.api';
import { VisorProductoComponent } from './visor-producto.component';

@Component({
  selector: 'app-ficha',
  imports: [RouterLink, VisorProductoComponent],
  template: `
    <article class="tile" [class.elegida]="enCarrito">
      <div class="tile-photo" [class.gira]="girando()">
        <a [routerLink]="['/catalogo', producto.id]" [attr.aria-label]="producto.nombre">
          <app-visor [src]="foto" [alt]="producto.nombre" [nombre]="producto.nombre" />
        </a>
        @if (producto.oferta) {
          <span class="tile-badge">Oferta</span>
        } @else if (producto.destacado) {
          <span class="tile-badge">Destacado</span>
        }
        <button type="button" class="giro" (click)="girar()" aria-label="Ver en 3D">3D</button>
      </div>
      <p class="eyebrow">{{ producto.categoriaNombre || 'Catálogo' }}</p>
      <h3><a [routerLink]="['/catalogo', producto.id]">{{ producto.nombre }}</a></h3>
      <p class="tile-meta">
        <span [class.on]="producto.disponible" [class.off]="!producto.disponible">{{ existencia }}</span>
        @if (producto.codigoSku) { <span>{{ producto.codigoSku }}</span> }
      </p>
      <div class="tile-row">
        <p class="tile-price">{{ precio }}</p>
        <button type="button" class="btn btn-sm" [class.btn-primary]="!enCarrito" [class.btn-ok]="enCarrito" [disabled]="!producto.disponible" (click)="sumar()">
          {{ enCarrito ? 'En el carrito' : 'Agregar' }}
        </button>
      </div>
    </article>
  `,
})
export class FichaProductoComponent {
  private readonly carrito = inject(CarritoService);
  readonly girando = signal(false);
  @Input({ required: true }) producto!: ProductoPublico;

  get enCarrito(): boolean {
    return this.carrito.tiene(this.producto.id);
  }

  girar(): void {
    this.girando.set(true);
    window.setTimeout(() => this.girando.set(false), 1100);
  }

  sumar(): void {
    if (!this.producto.disponible) return;
    const fraccion = this.producto.esFraccionable ? this.producto.fracciones?.[0] : undefined;
    this.carrito.agregar({
      productoId: this.producto.id,
      nombre: this.producto.nombre,
      fraccionId: fraccion?.id,
      fraccionNombre: fraccion?.nombre,
      precio: fraccion?.precio ?? this.producto.precioPublico,
      cantidad: 1,
      imagenUrl: this.producto.imagenUrl,
      unidad: this.producto.unidad || 'UND',
    });
  }

  get foto(): string | null {
    return media(this.producto.imagenUrl);
  }

  get inicial(): string {
    return (this.producto.nombre || 'P').charAt(0).toUpperCase();
  }

  get precio(): string {
    return pen(this.producto.precioPublico);
  }

  get existencia(): string {
    const stock = Number(this.producto.stock ?? 0);
    if (!this.producto.disponible || stock <= 0) return 'Agotado';
    return `Stock ${stock}`;
  }
}
