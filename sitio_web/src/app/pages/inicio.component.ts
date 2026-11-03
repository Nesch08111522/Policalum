import { AfterViewInit, Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FichaProductoComponent } from '../components/ficha-producto.component';
import { CmsStore } from '../core/cms.store';
import { LINEAS_OBRA } from '../core/obras';
import { PortalApi, media } from '../core/portal.api';
import { ProductoPublico, ProyectoPublico } from '../core/models';

@Component({
  selector: 'app-inicio',
  imports: [RouterLink, FichaProductoComponent],
  templateUrl: './inicio.component.html',
})
export class InicioComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly api = inject(PortalApi);
  private readonly cms = inject(CmsStore);
  private marco = 0;
  @ViewChild('cielo') cielo?: ElementRef<HTMLCanvasElement>;
  readonly destacados = signal<ProductoPublico[]>([]);
  readonly obras = signal<ProyectoPublico[]>(LINEAS_OBRA);
  readonly obrasReales = signal(false);
  readonly slides = [
    { titulo: 'Alta resistencia', texto: 'Planchas para techos que reciben sol, lluvia y granizo.' },
    { titulo: 'Luz sin calor de más', texto: 'Cubiertas que iluminan la nave y cuidan el interior.' },
    { titulo: 'Corte a la medida', texto: 'Completa, 3/4, 1/2 o 1/4, según la obra.' },
  ];

  ngOnInit(): void {
    this.cargarDestacados();
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

  private readonly alCms = effect(() => {
    if (this.cms.revision() === 0) return;
    untracked(() => this.cargarDestacados(true));
  });

  ngAfterViewInit(): void {
    const lienzo = this.cielo?.nativeElement;
    const ctx = lienzo?.getContext('2d');
    if (!lienzo || !ctx) return;
    const dibujar = (tiempo: number) => {
      const ancho = lienzo.clientWidth;
      const alto = lienzo.clientHeight;
      lienzo.width = ancho;
      lienzo.height = alto;
      ctx.clearRect(0, 0, ancho, alto);
      const foco = ctx.createRadialGradient(ancho * 0.72, alto * 0.35 + Math.sin(tiempo / 2800) * 16, 8, ancho * 0.5, alto * 0.5, ancho * 0.55);
      foco.addColorStop(0, 'rgba(31, 162, 214, 0.38)');
      foco.addColorStop(1, 'rgba(5, 5, 5, 0)');
      ctx.fillStyle = foco;
      ctx.fillRect(0, 0, ancho, alto);
      this.marco = requestAnimationFrame(dibujar);
    };
    this.marco = requestAnimationFrame(dibujar);
  }

  ngOnDestroy(): void {
    if (this.marco) cancelAnimationFrame(this.marco);
  }

  @HostListener('document:visibilitychange')
  alVolver(): void {
    if (document.visibilityState === 'visible') this.cargarDestacados(true);
  }

  private cargarDestacados(conservarSiFalla = false): void {
    this.api.destacados().subscribe({
      next: (lista) => this.destacados.set(lista),
      error: () => {
        if (!conservarSiFalla) this.destacados.set([]);
      },
    });
  }

  foto(obra: ProyectoPublico): string | null {
    return media(obra.imagenUrl);
  }
}
