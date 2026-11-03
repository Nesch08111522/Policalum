import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ServicioPublico } from '../core/models';
import { PortalApi, pen } from '../core/portal.api';

@Component({
  selector: 'app-servicios',
  imports: [RouterLink],
  template: `
    <section class="section">
      <p class="kicker">Servicios</p>
      <h1>Instalación, corte y lo que vendas en el ERP</h1>
      <p class="lead">Cada ficha sale de la tabla de servicios. El precio es el publicado; la cotización formal la confirma ventas.</p>
    </section>
    @if (!servicios().length) {
      <p class="muted">No hay servicios activos. Cuando los registres en el ERP, aparecen aquí. <a routerLink="/contacto">Escríbenos</a> si necesitas uno a medida.</p>
    } @else {
      <div class="service-list">
        @for (servicio of servicios(); track servicio.id) {
          <article class="panel service">
            <div>
              <h2>{{ servicio.nombre }}</h2>
              <p class="muted">{{ servicio.descripcion || 'Sin descripción publicada.' }}</p>
            </div>
            <p class="price">{{ pen(servicio.precio) }}</p>
          </article>
        }
      </div>
      <p class="muted" style="margin-top:1rem"><a routerLink="/cotizar">Pedir este trabajo en una cotización</a></p>
    }
  `,
})
export class ServiciosComponent implements OnInit {
  private readonly api = inject(PortalApi);
  readonly pen = pen;
  readonly servicios = signal<ServicioPublico[]>([]);

  ngOnInit(): void {
    this.api.servicios().subscribe({ next: (lista) => this.servicios.set(lista), error: () => undefined });
  }
}
