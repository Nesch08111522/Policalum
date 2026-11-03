import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProductoPublico } from '../core/models';
import { media, pen } from '../core/portal.api';

@Component({
  selector: 'app-ficha',
  imports: [RouterLink],
  template: `
    <article class="tile">
      <a class="tile-photo" [routerLink]="['/catalogo', producto.id]" [attr.aria-label]="producto.nombre">
        @if (foto) {
          <img [src]="foto" [alt]="producto.nombre" />
        } @else {
          <span class="swatch">{{ inicial }}</span>
        }
        @if (producto.oferta) {
          <span class="tile-badge">Oferta</span>
        } @else if (producto.destacado) {
          <span class="tile-badge">Destacado</span>
        }
      </a>
      <div class="tile-row">
        <p class="eyebrow">{{ producto.categoriaNombre || 'Catálogo' }}</p>
        <p class="tile-price">{{ precio }}</p>
      </div>
      <h3><a [routerLink]="['/catalogo', producto.id]">{{ producto.nombre }}</a></h3>
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
