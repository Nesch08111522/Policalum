import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { EmpresaStore } from '../core/empresa.store';
import { PortalApi, mensajeError } from '../core/portal.api';

const IDENTIDAD = 'policalum-chat';

interface IdentidadChat {
  nombre: string;
  correo: string;
  telefono: string;
  empresa: string;
}

@Component({
  selector: 'app-asistente',
  imports: [ReactiveFormsModule],
  template: `
    <div class="flotantes">
      @if (abierto()) {
        <section class="chat-panel" role="dialog" aria-label="Chat con ventas">
          <header>
            <strong>Ventas POLICALUM</strong>
            <button type="button" (click)="abierto.set(false)" aria-label="Cerrar chat">×</button>
          </header>
          <div class="chat-cuerpo">
            <p class="burbuja ellos">Cuéntanos qué material necesitas. El mensaje llega a Mensajes web y a la campana del ERP.</p>
            @for (linea of enviados(); track $index) {
              <p class="burbuja yo">{{ linea }}</p>
            }
            @if (aviso()) { <p class="burbuja ellos">{{ aviso() }}</p> }
            @if (error()) { <p class="alert">{{ error() }}</p> }
          </div>
          @if (!identidad()) {
            <form class="chat-form" [formGroup]="datos" (ngSubmit)="guardarDatos()">
              <input class="input" formControlName="nombre" placeholder="Nombre" aria-label="Nombre" />
              <input class="input" type="email" formControlName="correo" placeholder="Correo" aria-label="Correo" />
              <input class="input" formControlName="telefono" placeholder="Teléfono" aria-label="Teléfono" />
              <button type="submit" class="btn btn-primary btn-sm">Continuar</button>
            </form>
          } @else {
            <form class="chat-form" [formGroup]="texto" (ngSubmit)="enviar()">
              <textarea class="textarea" formControlName="mensaje" rows="2" placeholder="Escribe tu consulta" aria-label="Mensaje"></textarea>
              <button type="submit" class="btn btn-primary btn-sm" [disabled]="enviando()">{{ enviando() ? 'Enviando...' : 'Enviar' }}</button>
            </form>
          }
        </section>
      }
      <button type="button" class="flotante chat" (click)="abierto.update((v) => !v)" [attr.aria-expanded]="abierto()" aria-label="Abrir chat de ventas">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 6.5h14v9H8l-3 2.5V6.5Z"/></svg>
      </button>
      @if (whatsapp(); as enlace) {
        <a class="flotante wa" [href]="enlace" target="_blank" rel="noopener noreferrer" aria-label="Escribir por WhatsApp">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 18.5 5.2 20.2A9 9 0 1 1 12 21a8.9 8.9 0 0 1-3.4-.7L7 18.5Z"/><path d="M9.2 10.2c.2 1.4 1.5 2.8 3 3.4l1-.9 1.8.4-.3 1.6a6.4 6.4 0 0 1-5.6-5.5l1.6-.4.5 1.9-1 1.1Z"/></svg>
        </a>
      }
    </div>
  `,
})
export class AsistenteComponent {
  private readonly api = inject(PortalApi);
  private readonly marca = inject(EmpresaStore);
  readonly abierto = signal(false);
  readonly enviando = signal(false);
  readonly aviso = signal('');
  readonly error = signal('');
  readonly enviados = signal<string[]>([]);
  readonly identidad = signal<IdentidadChat | null>(this.leer());
  readonly datos = inject(FormBuilder).nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    correo: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    telefono: ['', Validators.pattern(/^$|^[0-9+()\- ]{6,20}$/)],
    empresa: ['', Validators.maxLength(150)],
  });
  readonly texto = inject(FormBuilder).nonNullable.group({
    mensaje: ['', [Validators.required, Validators.maxLength(2000)]],
  });

  whatsapp(): string | null {
    const numero = this.marca.empresa().whatsapp;
    if (!numero) return null;
    return `https://wa.me/${numero}?text=${encodeURIComponent('Hola POLICALUM, quiero hablar con un asesor.')}`;
  }

  guardarDatos(): void {
    if (this.datos.invalid) {
      this.datos.markAllAsTouched();
      this.error.set('Indica tu nombre y un correo válido.');
      return;
    }
    this.error.set('');
    const valor = this.datos.getRawValue();
    const identidad = { nombre: valor.nombre.trim(), correo: valor.correo.trim(), telefono: valor.telefono.trim(), empresa: valor.empresa.trim() };
    sessionStorage.setItem(IDENTIDAD, JSON.stringify(identidad));
    this.identidad.set(identidad);
  }

  enviar(): void {
    const quien = this.identidad();
    if (!quien || this.texto.invalid) {
      this.texto.markAllAsTouched();
      return;
    }
    const mensaje = this.texto.controls.mensaje.value.trim();
    this.enviando.set(true);
    this.error.set('');
    this.api.contacto({
      nombre: quien.nombre,
      correo: quien.correo,
      telefono: quien.telefono || undefined,
      empresa: quien.empresa || undefined,
      asunto: 'Consulta desde el chat',
      mensaje,
      website: '',
    }).subscribe({
      next: () => {
        this.enviando.set(false);
        this.enviados.update((lista) => [...lista, mensaje]);
        this.aviso.set('Listo. Ventas ya tiene el aviso en el ERP.');
        this.texto.reset();
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        this.error.set(mensajeError(error));
      },
    });
  }

  private leer(): IdentidadChat | null {
    try {
      const crudo = sessionStorage.getItem(IDENTIDAD);
      return crudo ? JSON.parse(crudo) as IdentidadChat : null;
    } catch {
      return null;
    }
  }
}
