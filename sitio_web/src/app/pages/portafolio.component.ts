import { Component, OnInit, inject, signal } from '@angular/core';
import { LINEAS_OBRA } from '../core/obras';
import { ProyectoPublico } from '../core/models';
import { PortalApi, media } from '../core/portal.api';

@Component({
  selector: 'app-portafolio',
  templateUrl: './portafolio.component.html',
})
export class PortafolioComponent implements OnInit {
  private readonly api = inject(PortalApi);
  readonly obras = signal<ProyectoPublico[]>(LINEAS_OBRA);
  readonly reales = signal(false);

  ngOnInit(): void {
    this.api.portafolio().subscribe({
      next: (lista) => {
        if (!lista.length) return;
        this.obras.set(lista);
        this.reales.set(true);
      },
      error: () => undefined,
    });
  }

  foto(obra: ProyectoPublico): string | null {
    return media(obra.imagenUrl);
  }
}
