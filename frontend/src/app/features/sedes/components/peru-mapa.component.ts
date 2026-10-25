import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, ViewChild } from '@angular/core';
import * as L from 'leaflet';

export interface PuntoSede {
  id?: string;
  nombre?: string;
  ciudad?: string;
  direccion?: string;
  latitud?: number | null;
  longitud?: number | null;
  foto?: string;
}

export interface PuntoElegido {
  latitud: number;
  longitud: number;
}

@Component({
  selector: 'app-peru-mapa',
  standalone: true,
  template: `<div #lienzo class="peru-mapa" role="region" [attr.aria-label]="elegir ? 'Mapa para ubicar la sede' : 'Mapa de sedes en el Perú'"></div>`,
  styles: [`
    :host { display: block; min-width: 0; }
    .peru-mapa { position: relative; z-index: 0; isolation: isolate; width: 100%; height: min(26rem, 58vh); min-height: 16rem; border-radius: 14px; border: 1px solid var(--color-border); background: var(--color-bg-subtle); overflow: hidden; }
    :host(.compacto) .peru-mapa { height: 16rem; min-height: 14rem; }
    @media (max-width: 40rem) { .peru-mapa, :host(.compacto) .peru-mapa { height: 15rem; } }
    :host ::ng-deep .leaflet-div-icon.sede-pin { background: transparent; border: 0; }
    :host ::ng-deep .sede-pin img { width: 2.25rem; height: 2.25rem; object-fit: contain; background: #fff; border: 2px solid #075591; border-radius: 0.7rem; box-shadow: 0 6px 16px rgba(7, 20, 40, 0.28); display: block; }
    :host ::ng-deep .sede-pin.mover { cursor: grab; }
    :host ::ng-deep .leaflet-popup-content-wrapper, :host ::ng-deep .leaflet-popup-tip { background: var(--color-surface); color: var(--color-text); box-shadow: var(--shadow-md); }
    :host ::ng-deep .sede-popup { display: grid; gap: 0.2rem; min-width: 9rem; }
    :host ::ng-deep .sede-popup img { width: 100%; height: 7rem; object-fit: cover; border-radius: 0.6rem; }
    :host ::ng-deep .sede-popup strong { font-size: 0.9rem; }
    :host ::ng-deep .sede-popup span { color: var(--color-text-muted); font-size: 0.78rem; }
  `],
})
export class PeruMapaComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() puntos: PuntoSede[] = [];
  @Input() elegir = false;
  @Input() marca: PuntoElegido | null = null;
  @Input() logo = '/POLICALUM.png';
  @Output() marcaChange = new EventEmitter<PuntoElegido>();
  @ViewChild('lienzo') lienzo?: ElementRef<HTMLDivElement>;

  private mapa?: L.Map;
  private readonly capa = L.layerGroup();
  private pin?: L.Marker;
  private firma = '';

  ngAfterViewInit(): void {
    const elemento = this.lienzo?.nativeElement;
    if (!elemento) return;
    this.mapa = L.map(elemento, { scrollWheelZoom: !this.elegir }).setView([-9.19, -75.01], 5);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap',
      maxZoom: 18,
    }).addTo(this.mapa);
    this.capa.addTo(this.mapa);
    if (this.elegir) {
      this.mapa.on('click', (evento: L.LeafletMouseEvent) => this.soltar(evento.latlng.lat, evento.latlng.lng, false));
    }
    this.pintar();
    setTimeout(() => {
      if (!this.mapa) return;
      this.mapa.invalidateSize();
      this.firma = '';
      this.pin?.remove();
      this.pin = undefined;
      this.pintar();
    }, 250);
  }

  ngOnChanges(): void { this.pintar(); }
  ngOnDestroy(): void { this.mapa?.remove(); }

  private pintar(): void {
    if (!this.mapa) return;
    this.capa.clearLayers();
    const validos = this.puntos.filter((punto) => punto.latitud != null && punto.longitud != null);
    for (const punto of validos) {
      const marca = L.marker([punto.latitud as number, punto.longitud as number], { icon: this.icono(false) });
      marca.bindPopup(popup(punto));
      marca.addTo(this.capa);
    }
    if (this.elegir && this.marca) this.soltar(this.marca.latitud, this.marca.longitud, true);
    const firma = validos.map((punto) => `${punto.latitud},${punto.longitud}`).join('|');
    if (firma === this.firma || this.elegir) return;
    this.firma = firma;
    if (validos.length === 1) {
      this.mapa.setView([validos[0].latitud as number, validos[0].longitud as number], 13);
    } else if (validos.length > 1) {
      this.mapa.fitBounds(L.latLngBounds(validos.map((punto) => [punto.latitud as number, punto.longitud as number])), { padding: [28, 28], maxZoom: 12 });
    }
  }

  private soltar(latitud: number, longitud: number, desdeDato: boolean): void {
    if (!this.mapa) return;
    const actual = this.pin?.getLatLng();
    const distinto = !actual || Math.abs(actual.lat - latitud) > 0.00001 || Math.abs(actual.lng - longitud) > 0.00001;
    if (!this.pin) {
      this.pin = L.marker([latitud, longitud], { icon: this.icono(true), draggable: true, autoPan: true, zIndexOffset: 600 });
      this.pin.on('dragend', () => {
        const punto = this.pin?.getLatLng();
        if (!punto) return;
        this.marcaChange.emit({ latitud: redondear(punto.lat), longitud: redondear(punto.lng) });
      });
      this.pin.addTo(this.mapa);
    } else if (distinto) {
      this.pin.setLatLng([latitud, longitud]);
    }
    if (distinto) this.mapa.setView([latitud, longitud], Math.max(this.mapa.getZoom(), 14));
    if (!desdeDato && distinto) this.marcaChange.emit({ latitud: redondear(latitud), longitud: redondear(longitud) });
  }

  private icono(mover: boolean): L.DivIcon {
    return L.divIcon({
      className: mover ? 'sede-pin mover' : 'sede-pin',
      html: `<img src="${escapar(this.logo)}" alt="" />`,
      iconSize: [36, 36],
      iconAnchor: [18, 18],
      popupAnchor: [0, -18],
    });
  }
}

function redondear(valor: number): number {
  return Math.round(valor * 1e6) / 1e6;
}

function popup(punto: PuntoSede): string {
  const foto = urlSegura(punto.foto);
  const imagen = foto ? `<img src="${escapar(foto)}" alt="" />` : '';
  return `<div class="sede-popup">${imagen}<strong>${escapar(punto.nombre || 'Sede')}</strong><span>${escapar(punto.direccion || punto.ciudad || '')}</span><span>${escapar(punto.ciudad || '')}</span></div>`;
}

function urlSegura(url?: string): string {
  if (!url) return '';
  if (url.startsWith('blob:') || url.startsWith('data:image/') || url.startsWith('/') || url.startsWith('http://') || url.startsWith('https://')) return url;
  return '';
}

function escapar(valor: string): string {
  return valor.replace(/[&<>"']/g, (caracter) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[caracter] ?? caracter));
}
