import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { media, pen } from '../core/portal.api';
import { ProductoPublico } from '../core/models';

@Component({
  selector: 'app-ficha',
  imports: [RouterLink],
  template: `
    <article class="card">
      <a class="card-media" [routerLink]="['/catalogo', producto.id]" [attr.aria-label]="producto.nombre">
        @if (foto) {
          <img [src]="foto" [alt]="producto.nombre" />
        } @else {
          <span class="swatch">{{ inicial }}</span>
        }
      </a>
      <div class="card-body">
        <p class="muted">{{ producto.categoriaNombre || 'Catálogo' }}</p>
        <h3><a [routerLink]="['/catalogo', producto.id]">{{ producto.nombre }}</a></h3>
        <p class="price">{{ precio }}</p>
        <p class="stock" [class.on]="producto.disponible" [class.off]="!producto.disponible">
          {{ producto.disponible ? 'Disponible' : 'Agotado' }}
        </p>
        <a class="btn btn-line btn-sm" [routerLink]="['/catalogo', producto.id]">Ver opciones</a>
      </div>
    </article>
  `,
})
export class FichaProductoComponent {
  @Input({ required: true }) producto!: ProductoPublico;

  get foto(): string | null {
    return media(this.producto.imagenUrl);
  }

  get inicial(): string {
    return (this.producto.nombre || 'P').charAt(0).toUpperCase();
  }

  get precio(): string {
    return pen(this.producto.precioPublico);
  }
}
