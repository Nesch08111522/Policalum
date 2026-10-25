import { AfterViewInit, Component, ElementRef, Input, OnChanges, OnDestroy, ViewChild } from '@angular/core';
import * as L from 'leaflet';

export interface PuntoSede {
  nombre?: string;
  ciudad?: string;
  direccion?: string;
  latitud?: number | null;
  longitud?: number | null;
}

@Component({
  selector: 'app-peru-mapa',
  standalone: true,
  template: `<div #lienzo class="peru-mapa" role="region" aria-label="Mapa de sedes en el Perú"></div>`,
  styles: [':host{display:block;min-width:0}.peru-mapa{height:420px;width:100%;border-radius:16px}'],
})
export class PeruMapaComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() puntos: PuntoSede[] = [];
  @ViewChild('lienzo') lienzo?: ElementRef<HTMLDivElement>;
  private mapa?: L.Map;
  private readonly capa = L.layerGroup();

  ngAfterViewInit(): void {
    const elemento = this.lienzo?.nativeElement;
    if (!elemento) return;
    this.mapa = L.map(elemento, { scrollWheelZoom: true }).setView([-9.19, -75.01], 5);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap',
      maxZoom: 18,
    }).addTo(this.mapa);
    this.capa.addTo(this.mapa);
    this.pintar();
    setTimeout(() => this.mapa?.invalidateSize(), 250);
  }

  ngOnChanges(): void { this.pintar(); }
  ngOnDestroy(): void { this.mapa?.remove(); }

  private pintar(): void {
    if (!this.mapa) return;
    this.capa.clearLayers();
    const validos = this.puntos.filter((punto) => punto.latitud != null && punto.longitud != null);
    for (const punto of validos) {
      L.circleMarker([punto.latitud as number, punto.longitud as number], {
        radius: 9,
        color: '#075591',
        fillColor: '#3db5e6',
        fillOpacity: 0.92,
        weight: 2,
      }).bindPopup(`<strong>${escapar(punto.nombre || 'Sede')}</strong><br>${escapar(punto.ciudad || '')}<br>${escapar(punto.direccion || '')}`)
        .addTo(this.capa);
    }
    if (validos.length === 1) {
      this.mapa.setView([-9.19, -75.01], 5);
    } else if (validos.length > 1) {
      this.mapa.fitBounds(L.latLngBounds(validos.map((punto) => [punto.latitud as number, punto.longitud as number])), { padding: [32, 32], maxZoom: 6 });
    }
  }
}

function escapar(valor: string): string {
  return valor.replace(/[&<>"']/g, (caracter) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[caracter] ?? caracter));
}
