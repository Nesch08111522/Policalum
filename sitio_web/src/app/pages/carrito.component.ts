import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CarritoService, enlaceWhatsapp } from '../core/carrito.service';
import { EmpresaStore } from '../core/empresa.store';
import { media, pen } from '../core/portal.api';

@Component({
  selector: 'app-carrito',
  imports: [AsyncPipe, RouterLink],
  templateUrl: './carrito.component.html',
})
export class CarritoComponent {
  readonly carrito = inject(CarritoService);
  readonly marca = inject(EmpresaStore);
  readonly pen = pen;
  readonly media = media;

  whatsapp(total: number): string | null {
    const numero = this.marca.empresa().whatsapp;
    if (!numero) return null;
    return enlaceWhatsapp(numero, this.carrito.snapshot(), total, pen);
  }
}
