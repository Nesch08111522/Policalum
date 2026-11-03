import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CarritoService } from '../core/carrito.service';
import { EmpresaStore } from '../core/empresa.store';
import { CotizacionCreada, FormatoCotizacion, LineaCarrito, VistaCotizacion } from '../core/models';
import { PortalApi, mensajeError, pen } from '../core/portal.api';

@Component({
  selector: 'app-cotizar',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './cotizar.component.html',
})
export class CotizarComponent implements OnInit {
  private readonly api = inject(PortalApi);
  readonly carrito = inject(CarritoService);
  readonly marca = inject(EmpresaStore);
  readonly pen = pen;
  readonly formato = signal<FormatoCotizacion | null>(null);
  readonly vista = signal<VistaCotizacion | null>(null);
  readonly enviando = signal(false);
  readonly calculando = signal(false);
  readonly error = signal('');
  readonly creada = signal<CotizacionCreada | null>(null);
  readonly form = inject(FormBuilder).nonNullable.group({
    ruc: ['', [Validators.required, Validators.pattern(/^\d{11}$/)]],
    razonSocial: ['', [Validators.required, Validators.maxLength(200)]],
    correo: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    telefono: ['', [Validators.required, Validators.pattern(/^[0-9+()\- ]{6,20}$/)]],
    distrito: ['', Validators.required],
    observaciones: ['', Validators.maxLength(2000)],
    website: [''],
  });

  ngOnInit(): void {
    this.api.formatoCotizacion().subscribe({
      next: (formato) => this.formato.set(formato),
      error: () => this.error.set('No se pudo cargar el formato de cotización del ERP.'),
    });
  }

  lineas(): LineaCarrito[] {
    return this.carrito.snapshot();
  }

  realizar(): void {
    this.error.set('');
    this.vista.set(null);
    if (!this.lineas().length) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Revisa el RUC, la razón social, el correo, el teléfono y el distrito.');
      return;
    }
    this.calculando.set(true);
    this.api.vistaCotizacion({
      distrito: this.form.controls.distrito.value,
      lineas: this.lineas().map((linea) => ({
        productoId: linea.productoId,
        fraccionId: linea.fraccionId || undefined,
        cantidad: linea.cantidad,
      })),
    }).subscribe({
      next: (vista) => {
        this.vista.set(vista);
        this.calculando.set(false);
      },
      error: (error: unknown) => {
        this.calculando.set(false);
        this.error.set(mensajeError(error));
      },
    });
  }

  enviar(): void {
    if (this.form.invalid || !this.lineas().length || !this.vista()) return;
    this.enviando.set(true);
    this.error.set('');
    const valor = this.form.getRawValue();
    this.api.cotizar({
      ruc: valor.ruc,
      razonSocial: valor.razonSocial,
      correo: valor.correo,
      telefono: valor.telefono,
      distrito: valor.distrito,
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

  whatsapp(creada: CotizacionCreada): string | null {
    const numero = this.marca.empresa().whatsapp;
    if (!numero) return null;
    const texto = `Hola POLICALUM, acabo de enviar la cotización de ${creada.clienteNombre ?? 'mi empresa'} por ${pen(creada.total)}. Quedo atento.`;
    return `https://wa.me/${numero}?text=${encodeURIComponent(texto)}`;
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
