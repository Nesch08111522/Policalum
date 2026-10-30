import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FichaProductoComponent } from '../components/ficha-producto.component';
import { LINEAS_OBRA } from '../core/obras';
import { PortalApi, media } from '../core/portal.api';
import { ProductoPublico, ProyectoPublico } from '../core/models';

@Component({
  selector: 'app-inicio',
  imports: [RouterLink, FichaProductoComponent],
  templateUrl: './inicio.component.html',
})
export class InicioComponent implements OnInit {
  private readonly api = inject(PortalApi);
  readonly destacados = signal<ProductoPublico[]>([]);
  readonly obras = signal<ProyectoPublico[]>(LINEAS_OBRA);
  readonly obrasReales = signal(false);
  readonly slides = [
    { titulo: 'Alta resistencia', texto: 'Planchas para techos que reciben sol, lluvia y granizo.' },
    { titulo: 'Luz sin calor de más', texto: 'Cubiertas que iluminan la nave y cuidan el interior.' },
    { titulo: 'Corte a la medida', texto: 'Completa, 3/4, 1/2 o 1/4, según la obra.' },
  ];

  ngOnInit(): void {
    this.api.destacados().subscribe({ next: (lista) => this.destacados.set(lista), error: () => this.destacados.set([]) });
    this.api.portafolio().subscribe({
      next: (lista) => {
        if (lista.length) {
          this.obras.set(lista);
          this.obrasReales.set(true);
        }
      },
      error: () => undefined,
    });
  }

  foto(obra: ProyectoPublico): string | null {
    return media(obra.imagenUrl);
  }
}
