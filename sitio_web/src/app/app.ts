import { AsyncPipe } from '@angular/common';
import { Component, HostListener, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CarritoService } from './core/carrito.service';
import { AvisoService } from './core/aviso.service';
import { CmsStore } from './core/cms.store';
import { EmpresaStore } from './core/empresa.store';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AsyncPipe],
  templateUrl: './app.html',
})
export class App {
  readonly carrito = inject(CarritoService);
  readonly marca = inject(EmpresaStore);
  readonly cms = inject(CmsStore);
  readonly avisos = inject(AvisoService);
  readonly menu = signal(false);
  readonly scrolled = signal(false);

  @HostListener('window:scroll')
  alScroll(): void {
    this.scrolled.set(window.scrollY > 8);
  }

  cerrar(): void {
    this.menu.set(false);
  }
}
