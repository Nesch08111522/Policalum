import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CarritoService } from '../core/carrito.service';
import { PortalApi, media, mensajeError, pen } from '../core/portal.api';
import { ProductoPublico } from '../core/models';

@Component({
  selector: 'app-producto',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './producto.component.html',
})
export class ProductoComponent implements OnInit {
  private readonly api = inject(PortalApi);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  readonly carrito = inject(CarritoService);
  readonly producto = signal<ProductoPublico | null>(null);
  readonly error = signal('');
  readonly aviso = signal('');
  readonly form = this.fb.nonNullable.group({
    fraccionId: [''],
    cantidad: [1, [Validators.required, Validators.min(0.0001)]],
  });

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (!id) return;
      this.aviso.set('');
      this.api.producto(id).subscribe({
        next: (producto) => {
          this.producto.set(producto);
          const primera = producto.fracciones[0]?.id ?? '';
          this.form.patchValue({ fraccionId: producto.esFraccionable ? primera : '', cantidad: 1 });
        },
        error: (error: unknown) => this.error.set(mensajeError(error)),
      });
    });
  }

  foto(producto: ProductoPublico): string | null {
    return media(producto.imagenUrl);
  }

  precioVisible(producto: ProductoPublico): string {
    const fraccion = producto.fracciones.find((item) => item.id === this.form.controls.fraccionId.value);
    return pen(fraccion ? fraccion.precio : producto.precioPublico);
  }

  agregar(producto: ProductoPublico): void {
    if (this.form.invalid) return;
    const fraccion = producto.fracciones.find((item) => item.id === this.form.controls.fraccionId.value);
    if (producto.esFraccionable && !fraccion) {
      this.aviso.set('Elige el corte de la plancha.');
      return;
    }
    this.carrito.agregar({
      productoId: producto.id,
      nombre: producto.nombre,
      fraccionId: fraccion?.id,
      fraccionNombre: fraccion?.nombre,
      precio: Number(fraccion ? fraccion.precio : producto.precioPublico),
      cantidad: Number(this.form.controls.cantidad.value),
      imagenUrl: producto.imagenUrl,
      unidad: producto.unidad || 'UNID',
    });
    this.aviso.set('Agregado al carrito.');
  }
}
