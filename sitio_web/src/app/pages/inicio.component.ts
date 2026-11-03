import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FichaProductoComponent } from '../components/ficha-producto.component';
import { LINEAS_OBRA } from '../core/obras';
import { EmpresaStore } from '../core/empresa.store';
import { PortalApi, media, pen } from '../core/portal.api';
import { ProductoPublico, ProyectoPublico, ServicioPublico } from '../core/models';

@Component({
  selector: 'app-inicio',
  imports: [RouterLink, FichaProductoComponent],
  templateUrl: './inicio.component.html',
})
export class InicioComponent implements OnInit {
  private readonly api = inject(PortalApi);
  readonly marca = inject(EmpresaStore);
  readonly pen = pen;
  readonly ofertas = signal<ProductoPublico[]>([]);
  readonly catalogo = signal<ProductoPublico[]>([]);
  readonly servicios = signal<ServicioPublico[]>([]);
  readonly obras = signal<ProyectoPublico[]>(LINEAS_OBRA);
  readonly obrasReales = signal(false);

  ngOnInit(): void {
    this.api.ofertas().subscribe({ next: (lista) => this.ofertas.set(lista), error: () => undefined });
    this.api.productos({ page: 0 }).subscribe({
      next: (pagina) => this.catalogo.set((pagina.content ?? []).slice(0, 6)),
      error: () => undefined,
    });
    this.api.servicios().subscribe({ next: (lista) => this.servicios.set(lista), error: () => undefined });
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
