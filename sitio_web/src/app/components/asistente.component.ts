import { Component, OnDestroy, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { EmpresaStore } from '../core/empresa.store';
import { LineaChat, PortalApi, mensajeError } from '../core/portal.api';

const FICHA = 'policalum-chat';

interface FichaChat {
  clave?: string;
  nombre: string;
  correo: string;
  telefono: string;
  empresa: string;
}

interface NotaLocal {
  texto: string;
  lado: 'guia' | 'cliente';
}

type Paso = 'menu' | 'pedido' | 'cotizar' | 'comprar' | 'stock' | 'escribir' | 'confirmar';

@Component({
  selector: 'app-asistente',
  imports: [ReactiveFormsModule],
  template: `
    <div class="flotantes">
      @if (abierto()) {
        <section class="chat-panel" role="dialog" aria-label="Chat con ventas">
          <header>
            <div>
              <strong>Ventas POLICALUM</strong>
              <p>{{ equipoEscribiendo() ? 'Escribiendo…' : 'Mensajes' }}</p>
            </div>
            <button type="button" (click)="cerrar()" aria-label="Cerrar chat">×</button>
          </header>
          <div class="chat-cuerpo">
            @for (nota of locales(); track $index) {
              <p class="burbuja" [class.yo]="nota.lado === 'cliente'" [class.ellos]="nota.lado === 'guia'">{{ nota.texto }}</p>
            }
            @for (linea of lineas(); track linea.id) {
              <p class="burbuja" [class.yo]="linea.autor === 'CLIENTE'" [class.ellos]="linea.autor === 'EQUIPO'">
                @if (linea.autor === 'EQUIPO') { <small>{{ linea.autorNombre || 'POLICALUM' }}</small> }
                {{ linea.texto }}
                @if (hora(linea.createdAt); as marca) { <time>{{ marca }}</time> }
              </p>
            }
            @if (equipoEscribiendo()) { <p class="escribiendo">El equipo está escribiendo…</p> }
            @if (error()) { <p class="alert">{{ error() }}</p> }
          </div>
          @if (paso() !== 'escribir' && paso() !== 'confirmar') {
            <div class="chat-opciones">
              @for (opcion of opciones(); track opcion.id) {
                <button type="button" (click)="elegir(opcion.id)">{{ opcion.texto }}</button>
              }
            </div>
          } @else if (paso() === 'confirmar' && anterior()) {
            <div class="chat-opciones">
              <p class="burbuja ellos">Ya hemos tenido una conversación anterior con {{ anterior()?.nombre }}. ¿Eras tú?</p>
              <button type="button" (click)="confirmar(true)">Sí, soy yo</button>
              <button type="button" (click)="confirmar(false)">No, soy otra persona</button>
            </div>
          } @else if (!identidad()) {
            <form class="chat-form" [formGroup]="datos" (ngSubmit)="guardarDatos()">
              <input class="input" formControlName="nombre" placeholder="Nombre" aria-label="Nombre" />
              <input class="input" type="email" formControlName="correo" placeholder="Correo" aria-label="Correo" />
              <input class="input" formControlName="telefono" placeholder="Teléfono" aria-label="Teléfono" />
              <button type="submit" class="btn btn-primary btn-sm">Continuar</button>
            </form>
          } @else {
            <form class="chat-form" [formGroup]="texto" (ngSubmit)="enviar()">
              <textarea class="textarea" formControlName="mensaje" rows="2" placeholder="Escribe un mensaje" aria-label="Mensaje" (input)="presencia()" (keydown)="tecla($event)"></textarea>
              <button type="submit" class="btn btn-primary btn-sm" [disabled]="enviando()">{{ enviando() ? 'Enviando...' : 'Enviar' }}</button>
            </form>
          }
        </section>
      }
      <button type="button" class="flotante chat" (click)="alternar()" [attr.aria-expanded]="abierto()" aria-label="Abrir chat de ventas">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 6.5h14v9H8l-3 2.5V6.5Z"/></svg>
        @if (noLeidos() > 0) { <span class="chat-badge">{{ noLeidos() > 9 ? '9+' : noLeidos() }}</span> }
      </button>
      @if (whatsapp(); as enlace) {
        <a class="flotante wa" [href]="enlace" target="_blank" rel="noopener noreferrer" aria-label="Escribir por WhatsApp">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 18.5 5.2 20.2A9 9 0 1 1 12 21a8.9 8.9 0 0 1-3.4-.7L7 18.5Z"/><path d="M9.2 10.2c.2 1.4 1.5 2.8 3 3.4l1-.9 1.8.4-.3 1.6a6.4 6.4 0 0 1-5.6-5.5l1.6-.4.5 1.9-1 1.1Z"/></svg>
        </a>
      }
    </div>
  `,
})
export class AsistenteComponent implements OnDestroy {
  private readonly api = inject(PortalApi);
  private readonly marca = inject(EmpresaStore);
  private reloj = 0;
  private ultimaPresencia = 0;
  readonly abierto = signal(false);
  readonly enviando = signal(false);
  readonly error = signal('');
  readonly paso = signal<Paso>('menu');
  readonly locales = signal<NotaLocal[]>([{ lado: 'guia', texto: 'Hola. Elige cómo quieres continuar.' }]);
  readonly lineas = signal<LineaChat[]>([]);
  readonly noLeidos = signal(0);
  readonly equipoEscribiendo = signal(false);
  readonly clave = signal('');
  readonly anterior = signal<{ nombre: string; referencia: string } | null>(null);
  readonly identidad = signal<FichaChat | null>(null);
  readonly datos = inject(FormBuilder).nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    correo: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    telefono: ['', Validators.pattern(/^$|^[0-9+()\- ]{6,20}$/)],
    empresa: ['', Validators.maxLength(150)],
  });
  readonly texto = inject(FormBuilder).nonNullable.group({
    mensaje: ['', [Validators.required, Validators.maxLength(2000)]],
  });

  constructor() {
    const ficha = this.leer();
    if (ficha) {
      this.identidad.set(ficha);
      this.clave.set(ficha.clave ?? '');
      if (ficha.clave) this.paso.set('escribir');
    }
    this.refrescar();
    this.reloj = window.setInterval(() => this.refrescar(), 2500);
  }

  ngOnDestroy(): void {
    window.clearInterval(this.reloj);
  }

  whatsapp(): string | null {
    const numero = this.marca.empresa().whatsapp;
    if (!numero) return null;
    return `https://wa.me/${numero}?text=${encodeURIComponent('Hola POLICALUM, quiero hablar con un asesor.')}`;
  }

  opciones(): { id: string; texto: string }[] {
    if (this.paso() === 'menu') {
      return [
        { id: 'pedido', texto: 'a) Cómo realizo un pedido' },
        { id: 'stock', texto: 'b) Los productos están actualizados' },
        { id: 'encargado', texto: 'c) Contactar al encargado' },
      ];
    }
    if (this.paso() === 'pedido') {
      return [
        { id: 'cotizar', texto: 'Primero una cotización' },
        { id: 'comprar', texto: 'Comprar productos' },
        { id: 'menu', texto: 'Volver al inicio' },
      ];
    }
    if (this.paso() === 'cotizar' || this.paso() === 'comprar' || this.paso() === 'stock') {
      return [
        { id: 'cotizar', texto: 'Cómo cotizo' },
        { id: 'comprar', texto: 'Cómo compro los productos' },
        { id: 'encargado', texto: 'Contactar al encargado' },
        { id: 'menu', texto: 'Volver al inicio' },
      ];
    }
    return [];
  }

  elegir(id: string): void {
    this.error.set('');
    if (id === 'menu') {
      this.paso.set('menu');
      this.locales.update((lista) => [...lista, { lado: 'guia', texto: 'Elige cómo quieres continuar.' }]);
      return;
    }
    if (id === 'pedido') {
      this.anotar('a) Cómo realizo un pedido', this.textoPedido());
      this.paso.set('pedido');
      return;
    }
    if (id === 'cotizar') {
      this.anotar('Primero una cotización', this.textoCotizar());
      this.paso.set('cotizar');
      return;
    }
    if (id === 'comprar') {
      this.anotar('Comprar productos', this.textoComprar());
      this.paso.set('comprar');
      return;
    }
    if (id === 'stock') {
      this.anotar('b) Los productos están actualizados', 'Sí. Los productos se trabajan con la información real del ERP. El precio, el stock y los datos que ves en el sitio salen de ese registro. Cuando el equipo actualiza un producto, el sitio muestra ese cambio.');
      this.paso.set('stock');
      return;
    }
    const numero = this.telefonoEmpresa();
    const aviso = numero
      ? `Puedes comunicarte por este medio. Déjanos tu mensaje y el equipo te responde aquí.\nSi en 24 horas no recibes respuesta, llama al ${numero}.`
      : 'Puedes comunicarte por este medio. Déjanos tu mensaje y el equipo te responde aquí.\nSi en 24 horas no recibes respuesta, vuelve a escribir por este chat.';
    this.anotar('c) Contactar al encargado', aviso);
    this.paso.set('escribir');
  }

  guardarDatos(): void {
    if (this.datos.invalid) {
      this.datos.markAllAsTouched();
      this.error.set('Indica tu nombre y un correo válido.');
      return;
    }
    const valor = this.datos.getRawValue();
    const ficha: FichaChat = {
      clave: this.clave() || undefined,
      nombre: valor.nombre.trim(),
      correo: valor.correo.trim(),
      telefono: valor.telefono.trim(),
      empresa: valor.empresa.trim(),
    };
    this.error.set('');
    this.api.coincidencias(ficha.correo, ficha.telefono, dispositivo()).subscribe({
      next: (coincidencia) => {
        if (!coincidencia.encontrada || !coincidencia.referencia) {
          this.guardar(ficha);
          this.identidad.set(ficha);
          return;
        }
        if (coincidencia.mismoDispositivo) {
          this.retomar(coincidencia.referencia, ficha);
          return;
        }
        this.anterior.set({ nombre: coincidencia.nombre || 'un cliente', referencia: coincidencia.referencia });
        this.paso.set('confirmar');
      },
      error: () => {
        this.guardar(ficha);
        this.identidad.set(ficha);
      },
    });
  }

  confirmar(soyYo: boolean): void {
    const valor = this.datos.getRawValue();
    const ficha: FichaChat = {
      nombre: valor.nombre.trim(),
      correo: valor.correo.trim(),
      telefono: valor.telefono.trim(),
      empresa: valor.empresa.trim(),
    };
    const referencia = this.anterior()?.referencia;
    this.anterior.set(null);
    if (!soyYo || !referencia) {
      this.clave.set('');
      this.guardar(ficha);
      this.identidad.set(ficha);
      this.paso.set('escribir');
      return;
    }
    this.retomar(referencia, ficha);
  }

  private retomar(referencia: string, ficha: FichaChat): void {
    this.api.continuarChat({ referencia, correo: ficha.correo, telefono: ficha.telefono, dispositivo: dispositivo() }).subscribe({
      next: (respuesta) => {
        ficha.clave = respuesta.clave;
        this.clave.set(respuesta.clave);
        this.guardar(ficha);
        this.identidad.set(ficha);
        this.paso.set('escribir');
        this.refrescar();
      },
      error: (error: unknown) => this.error.set('No se pudo retomar la conversación.'),
    });
  }

  enviar(): void {
    const quien = this.identidad();
    if (!quien || this.texto.invalid || this.enviando()) {
      this.texto.markAllAsTouched();
      return;
    }
    const mensaje = this.texto.controls.mensaje.value.trim();
    if (!mensaje) return;
    this.enviando.set(true);
    this.error.set('');
    this.api.enviarChat({
      clave: this.clave() || undefined,
      nombre: quien.nombre,
      correo: quien.correo,
      telefono: quien.telefono || undefined,
      empresa: quien.empresa || undefined,
      texto: mensaje,
      dispositivo: dispositivo(),
    }).subscribe({
      next: (hilo) => {
        this.enviando.set(false);
        this.clave.set(hilo.clave);
        this.lineas.set(hilo.lineas ?? []);
        this.guardar({ ...quien, clave: hilo.clave });
        this.texto.reset();
        this.bajar();
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        this.error.set(mensajeError(error));
      },
    });
  }

  presencia(): void {
    const clave = this.clave();
    const activo = this.texto.controls.mensaje.value.trim().length > 0;
    const ahora = Date.now();
    if (!clave || ahora - this.ultimaPresencia < 2000) return;
    this.ultimaPresencia = ahora;
    this.api.presenciaChat(clave, activo).subscribe({ error: () => undefined });
  }

  tecla(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.enviar();
    }
  }

  alternar(): void {
    this.abierto.update((valor) => !valor);
    if (this.abierto()) this.marcarLeido();
  }

  cerrar(): void {
    this.abierto.set(false);
  }

  hora(valor?: string): string {
    if (!valor) return '';
    const fecha = new Date(valor);
    if (Number.isNaN(fecha.getTime())) return '';
    return fecha.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
  }

  private refrescar(): void {
    const clave = this.clave();
    if (!clave) return;
    this.api.leerChat(clave).subscribe({
      next: (hilo) => {
        this.lineas.set(hilo.lineas ?? []);
        this.equipoEscribiendo.set(hilo.equipoEscribiendo);
        if (this.abierto()) {
          this.noLeidos.set(0);
          if (hilo.noLeidos > 0) this.api.leerChatCliente(clave).subscribe({ error: () => undefined });
        } else {
          this.noLeidos.set(hilo.noLeidos ?? 0);
        }
        if (this.abierto()) this.bajar();
      },
      error: () => {
        localStorage.removeItem(FICHA);
        this.clave.set('');
        this.identidad.set(null);
        this.lineas.set([]);
        this.paso.set('menu');
      },
    });
  }

  private marcarLeido(): void {
    this.noLeidos.set(0);
    const clave = this.clave();
    if (clave) this.api.leerChatCliente(clave).subscribe({ error: () => undefined });
    this.bajar();
  }

  private anotar(pregunta: string, respuesta: string): void {
    this.locales.update((lista) => [...lista, { lado: 'cliente', texto: pregunta }, { lado: 'guia', texto: respuesta }]);
    this.bajar();
  }

  private textoPedido(): string {
    return 'Para comprar en POLICALUM puedes seguir estos pasos:\n1. Entra a Catálogo y elige el producto.\n2. Revisa medida, color y stock.\n3. Pulsa Agregar. El producto queda en tu carrito.\n\nSi todavía estás definiendo cantidades o el traslado, conviene cotizar primero. Si ya sabes lo que necesitas, puedes seguir con la compra.\n\n¿Qué quieres hacer primero?';
  }

  private textoCotizar(): string {
    return 'Para cotizar:\n1. Arma la lista en el catálogo y agrégala al carrito.\n2. Entra a Cotizar y deja tu nombre, documento, teléfono y el distrito de entrega.\n3. El sistema calcula material, movilidad y los otros cargos con los datos del ERP.\n4. Ventas revisa la solicitud y te confirma el monto. Si ajusta un cargo, la cotización indica qué cambió.\n5. Cuando aceptas, se coordina el pago y la entrega.\n\nAsí el pedido queda claro antes de pagar.';
  }

  private textoComprar(): string {
    return 'Para comprar los productos:\n1. Abre el producto en el catálogo y confirma medida, color y disponibilidad.\n2. Pulsa Agregar. Si ya está en el carrito, el botón lo indica.\n3. Entra a Carrito y revisa las cantidades.\n4. Si el dato está completo, sigue con la solicitud. Si falta medida, traslado o un cargo, pide la cotización.\n\nEl precio que ves es el que está publicado en el ERP.';
  }

  private telefonoEmpresa(): string {
    return (this.marca.empresa().telefono ?? '').trim();
  }

  private guardar(ficha: FichaChat): void {
    localStorage.setItem(FICHA, JSON.stringify(ficha));
    this.identidad.set(ficha);
  }

  private leer(): FichaChat | null {
    try {
      const crudo = localStorage.getItem(FICHA);
      if (!crudo) return null;
      const ficha = JSON.parse(crudo) as FichaChat;
      return ficha?.nombre && ficha?.correo ? ficha : null;
    } catch {
      return null;
    }
  }

  private bajar(): void {
    window.setTimeout(() => {
      const caja = document.querySelector('.chat-cuerpo');
      if (caja) caja.scrollTop = caja.scrollHeight;
    }, 0);
  }
}

function dispositivo(): string {
  const clave = 'policalum-dispositivo';
  const actual = localStorage.getItem(clave);
  if (actual) return actual;
  const nuevo = crypto.randomUUID();
  localStorage.setItem(clave, nuevo);
  return nuevo;
}
