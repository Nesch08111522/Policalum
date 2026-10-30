import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CarritoService } from '../core/carrito.service';
import { EmpresaStore } from '../core/empresa.store';
import { CotizacionCreada, LineaCarrito } from '../core/models';
import { PortalApi, mensajeError, pen } from '../core/portal.api';

@Component({
  selector: 'app-cotizar',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './cotizar.component.html',
})
export class CotizarComponent {
  private readonly api = inject(PortalApi);
  readonly carrito = inject(CarritoService);
  readonly marca = inject(EmpresaStore);
  readonly pen = pen;
  readonly vista = signal(false);
  readonly enviando = signal(false);
  readonly error = signal('');
  readonly creada = signal<CotizacionCreada | null>(null);
  readonly form = inject(FormBuilder).nonNullable.group({
    ruc: ['', [Validators.required, Validators.pattern(/^\d{11}$/)]],
    razonSocial: ['', [Validators.required, Validators.maxLength(200)]],
    correo: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    telefono: ['', [Validators.required, Validators.pattern(/^[0-9+()\- ]{6,20}$/)]],
    observaciones: ['', Validators.maxLength(2000)],
    website: [''],
  });

  lineas(): LineaCarrito[] {
    return this.carrito.snapshot();
  }

  totalLocal(): number {
    return this.lineas().reduce((suma, linea) => suma + linea.precio * linea.cantidad, 0);
  }

  ver(): void {
    this.error.set('');
    if (!this.lineas().length) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Revisa el RUC, la razón social, el correo y el teléfono.');
      return;
    }
    this.vista.set(true);
  }

  enviar(): void {
    if (this.form.invalid || !this.lineas().length) return;
    this.enviando.set(true);
    this.error.set('');
    const valor = this.form.getRawValue();
    this.api.cotizar({
      ruc: valor.ruc,
      razonSocial: valor.razonSocial,
      correo: valor.correo,
      telefono: valor.telefono,
      observaciones: valor.observaciones || undefined,
      website: valor.website,
      lineas: this.lineas().map((linea) => ({
        productoId: linea.productoId,
        fraccionId: linea.fraccionId || undefined,
        cantidad: linea.cantidad,
      })),
    }).subscribe({
      next: (creada) => {
        this.creada.set(creada);
        this.enviando.set(false);
        this.carrito.vaciar();
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        this.error.set(mensajeError(error));
      },
    });
  }

  imprimir(): void {
    window.print();
  }

  pdf(): void {
    const id = this.creada()?.id;
    if (!id) return;
    window.open(this.api.pdfUrl(id), '_blank', 'noopener');
  }
}
