import { Injectable } from '@angular/core';
import { BehaviorSubject, map } from 'rxjs';
import { LineaCarrito } from './models';

const CLAVE = 'policalum-carrito';

@Injectable({ providedIn: 'root' })
export class CarritoService {
  private readonly lineas$ = new BehaviorSubject<LineaCarrito[]>(this.leer());
  readonly lineas = this.lineas$.asObservable();
  readonly piezas = this.lineas.pipe(map((lista) => lista.reduce((total, linea) => total + linea.cantidad, 0)));
  readonly total = this.lineas.pipe(map((lista) => lista.reduce((total, linea) => total + linea.precio * linea.cantidad, 0)));

  snapshot(): LineaCarrito[] {
    return this.lineas$.value;
  }

  agregar(linea: LineaCarrito): void {
    const actual = [...this.lineas$.value];
    const indice = actual.findIndex((item) => item.productoId === linea.productoId && (item.fraccionId ?? '') === (linea.fraccionId ?? ''));
    if (indice >= 0) {
      actual[indice] = { ...actual[indice], cantidad: redondear(actual[indice].cantidad + linea.cantidad) };
    } else {
      actual.push({ ...linea, cantidad: redondear(linea.cantidad) });
    }
    this.publicar(actual);
  }

  cambiarCantidad(indice: number, cantidad: number): void {
    const actual = [...this.lineas$.value];
    if (!actual[indice]) return;
    actual[indice] = { ...actual[indice], cantidad: redondear(Math.max(cantidad, 0.0001)) };
    this.publicar(actual);
  }

  quitar(indice: number): void {
    this.publicar(this.lineas$.value.filter((_, i) => i !== indice));
  }

  vaciar(): void {
    this.publicar([]);
  }

  private publicar(lista: LineaCarrito[]): void {
    this.lineas$.next(lista);
    localStorage.setItem(CLAVE, JSON.stringify(lista));
  }

  private leer(): LineaCarrito[] {
    try {
      const crudo = localStorage.getItem(CLAVE);
      const lista = crudo ? JSON.parse(crudo) as LineaCarrito[] : [];
      return Array.isArray(lista) ? lista : [];
    } catch {
      return [];
    }
  }
}

function redondear(valor: number): number {
  return Math.round(valor * 10000) / 10000;
}

export function enlaceWhatsapp(numero: string, lineas: LineaCarrito[], total: number, formato: (valor: number) => string): string {
  const detalle = lineas.map((linea) => {
    const corte = linea.fraccionNombre ? ` (${linea.fraccionNombre})` : '';
    return `• ${linea.nombre}${corte} x ${linea.cantidad} = ${formato(linea.precio * linea.cantidad)}`;
  }).join('\n');
  const texto = `Hola POLICALUM, quiero consultar este pedido:\n${detalle}\nTotal aproximado: ${formato(total)}`;
  return `https://wa.me/${numero}?text=${encodeURIComponent(texto)}`;
}
