import { CommonModule, DatePipe } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnDestroy, OnInit, effect, inject, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Routes } from '@angular/router';
import { environment } from '../../../environments/environment';
import { TokenService } from '../../services/token.service';
import { CanalWebAvisos } from '../../shared/services/canal-web-avisos.service';
import { ToastService } from '../../shared/services/toast.service';
import { problemText } from '../../shared/utils/feedback';

interface LineaChat {
  id: string;
  autor: 'CLIENTE' | 'EQUIPO';
  autorNombre?: string;
  texto: string;
  createdAt?: string;
}

interface HiloEquipo {
  id: string;
  nombre: string;
  correo: string;
  telefono?: string;
  empresa?: string;
  noLeidos: number;
  clienteEscribiendo: boolean;
  lineas: LineaChat[];
  estado?: string;
  clienteId?: string;
}

interface ResumenChat {
  id: string;
  nombre: string;
  correo: string;
  telefono?: string;
  empresa?: string;
  ultimoTexto?: string;
  ultimoMensajeAt?: string;
  noLeidos: number;
  clienteEscribiendo: boolean;
  estado?: string;
  clienteId?: string;
}

interface MensajeFormulario {
  id: string;
  nombre: string;
  correo: string;
  telefono?: string;
  empresa?: string;
  asunto: string;
  mensaje: string;
  estado: 'NUEVO' | 'GESTIONADO';
  createdAt?: string;
}

interface Pagina { content?: MensajeFormulario[]; }

interface ItemChat {
  id: string;
  vivo: boolean;
  nombre: string;
  correo: string;
  telefono?: string;
  empresa?: string;
  preview: string;
  cuando?: string;
  noLeidos: number;
  escribiendo: boolean;
  cuerpo?: string;
  estado?: string;
  clienteId?: string;
}

@Component({
  selector: 'app-mensajes-web-page',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule],
  templateUrl: './mensajes-web-page.component.html',
  styleUrls: ['./mensajes-web-page.component.css'],
})
export class MensajesWebPageComponent implements OnInit, OnDestroy {
  items: ItemChat[] = [];
  filtro: 'todos' | 'nuevos' | 'espera' | 'cerradas' = 'todos';
  busca = '';
  cargando = true;
  abierto: ItemChat | null = null;
  hilo: HiloEquipo | null = null;
  borrador = '';
  enviando = false;
  fichaAbierta = false;
  ficha = { nombre: '', tipoDocumento: 'DNI', numeroDocumento: '', celular: '', email: '', direccion: '' };
  private correoInicial = '';
  private readonly ruta = inject(ActivatedRoute);
  private reloj = 0;
  private ultimaPresencia = 0;
  private readonly tokens = inject(TokenService);
  private readonly canal = inject(CanalWebAvisos);
  private readonly alLlegarWeb = effect(() => {
    if (this.canal.revision() === 0) return;
    untracked(() => this.cargar());
  });

  constructor(
    private readonly http: HttpClient,
    private readonly toast: ToastService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.correoInicial = this.ruta.snapshot.queryParamMap.get('correo') ?? '';
    this.cargar();
    this.reloj = window.setInterval(() => this.cargar(false), 2500);
  }

  ngOnDestroy(): void {
    window.clearInterval(this.reloj);
  }

  get visibles(): ItemChat[] {
    const texto = this.busca.trim().toLowerCase();
    return this.items.filter((item) => {
      if (this.filtro === 'nuevos' && item.noLeidos < 1) return false;
      if (this.filtro === 'espera' && item.estado !== 'EN_ESPERA') return false;
      if (this.filtro === 'cerradas' && item.estado !== 'CERRADA') return false;
      if (!texto) return true;
      return `${item.nombre} ${item.correo} ${item.preview}`.toLowerCase().includes(texto);
    });
  }

  cargar(conAviso = true): void {
    if (conAviso) this.cargando = true;
    const params = new HttpParams().set('size', '50');
    this.http.get<ResumenChat[]>(`${environment.apiUrl}/api/v1/chats`).subscribe({
      next: (chats) => {
        this.http.get<Pagina>(`${environment.apiUrl}/api/v1/mensajes-web`, { params }).subscribe({
          next: (pagina) => {
            this.items = this.unir(chats ?? [], pagina.content ?? []);
            this.cargando = false;
            if (this.correoInicial) {
              const hallado = this.items.find((item) => item.vivo && item.correo === this.correoInicial);
              if (hallado) this.abrir(hallado);
              this.correoInicial = '';
            } else if (this.abierto?.vivo) this.leerHilo(this.abierto.id, false);
            this.cdr.detectChanges();
          },
          error: (error: unknown) => this.fallo(error, conAviso),
        });
      },
      error: (error: unknown) => this.fallo(error, conAviso),
    });
  }

  abrir(item: ItemChat): void {
    this.abierto = item;
    this.hilo = null;
    this.borrador = '';
    if (!item.vivo) return;
    this.http.post(`${environment.apiUrl}/api/v1/chats/${item.id}/leido`, {}, { responseType: 'text' }).subscribe({ error: () => undefined });
    this.leerHilo(item.id, true);
  }

  volver(): void {
    this.abierto = null;
    this.hilo = null;
  }

  enviar(): void {
    if (!this.abierto?.vivo || this.enviando) return;
    const texto = this.borrador.trim();
    if (!texto) return;
    this.enviando = true;
    const autorNombre = this.tokens.session()?.nombre || 'POLICALUM';
    this.http.post<HiloEquipo>(`${environment.apiUrl}/api/v1/chats/${this.abierto.id}/mensajes`, { texto, autorNombre }).subscribe({
      next: (hilo) => {
        this.enviando = false;
        this.hilo = hilo;
        this.borrador = '';
        this.bajar();
        this.cargar(false);
      },
      error: (error: unknown) => {
        this.enviando = false;
        this.toast.error(problemText(error));
        this.cdr.detectChanges();
      },
    });
  }

  presencia(): void {
    if (!this.abierto?.vivo) return;
    const ahora = Date.now();
    if (ahora - this.ultimaPresencia < 2000) return;
    this.ultimaPresencia = ahora;
    const activo = this.borrador.trim().length > 0;
    this.http.post(`${environment.apiUrl}/api/v1/chats/${this.abierto.id}/escribiendo`, { activo }, { responseType: 'text' }).subscribe({ error: () => undefined });
  }

  tecla(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.enviar();
    }
  }

  marcarEstado(estado: 'ABIERTA' | 'EN_ESPERA' | 'CERRADA'): void {
    if (!this.abierto?.vivo) return;
    this.http.post(`${environment.apiUrl}/api/v1/chats/${this.abierto.id}/estado`, { estado }, { responseType: 'text' }).subscribe({
      next: () => {
        if (this.abierto) this.abierto.estado = estado;
        this.toast.success(estado === 'CERRADA' ? 'Conversación cerrada.' : estado === 'EN_ESPERA' ? 'Conversación en espera.' : 'Conversación abierta.');
        this.cargar(false);
      },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  borrar(): void {
    if (!this.abierto?.vivo) return;
    this.http.delete(`${environment.apiUrl}/api/v1/chats/${this.abierto.id}`, { responseType: 'text' }).subscribe({
      next: () => {
        this.abierto = null;
        this.hilo = null;
        this.toast.success('Conversación eliminada.');
        this.cargar(false);
      },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  abrirFicha(): void {
    if (!this.abierto) return;
    this.ficha = {
      nombre: this.abierto.nombre,
      tipoDocumento: 'DNI',
      numeroDocumento: '',
      celular: this.abierto.telefono ?? '',
      email: this.abierto.correo,
      direccion: '',
    };
    this.fichaAbierta = true;
  }

  guardarNombre(): void {
    if (!this.abierto?.vivo || !this.ficha.nombre.trim() || !this.ficha.email.trim()) {
      this.toast.error('El nombre y el correo son necesarios.');
      return;
    }
    this.http.put<HiloEquipo>(`${environment.apiUrl}/api/v1/chats/${this.abierto.id}`, {
      nombre: this.ficha.nombre.trim(),
      correo: this.ficha.email.trim(),
      telefono: this.ficha.celular.trim(),
    }).subscribe({
      next: (hilo) => {
        this.hilo = hilo;
        if (this.abierto) {
          this.abierto.nombre = hilo.nombre;
          this.abierto.correo = hilo.correo;
          this.abierto.telefono = hilo.telefono;
        }
        this.toast.success('Nombre actualizado.');
        this.cargar(false);
      },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  guardarCliente(): void {
    if (!this.abierto?.vivo) return;
    const documento = this.ficha.numeroDocumento.trim();
    if (this.ficha.tipoDocumento === 'DNI' && !/^\d{8}$/.test(documento)) {
      this.toast.error('El DNI debe tener 8 dígitos.');
      return;
    }
    if (this.ficha.tipoDocumento === 'RUC' && !/^\d{11}$/.test(documento)) {
      this.toast.error('El RUC debe tener 11 dígitos.');
      return;
    }
    if (!this.ficha.nombre.trim()) {
      this.toast.error('Indica el nombre del cliente.');
      return;
    }
    this.http.post<HiloEquipo>(`${environment.apiUrl}/api/v1/chats/${this.abierto.id}/cliente`, {
      nombre: this.ficha.nombre.trim(),
      tipoDocumento: this.ficha.tipoDocumento,
      numeroDocumento: documento,
      celular: this.ficha.celular.trim() || undefined,
      email: this.ficha.email.trim() || undefined,
      direccion: this.ficha.direccion.trim() || undefined,
    }).subscribe({
      next: (hilo) => {
        this.hilo = hilo;
        this.abierto = { ...this.abierto!, nombre: hilo.nombre, correo: hilo.correo, telefono: hilo.telefono, clienteId: hilo.clienteId };
        this.fichaAbierta = false;
        this.toast.success('El cliente quedó guardado en Clientes.');
        this.cargar(false);
      },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  inicial(nombre: string): string {
    return (nombre.trim().charAt(0) || 'C').toUpperCase();
  }

  private leerHilo(id: string, bajar: boolean): void {
    this.http.get<HiloEquipo>(`${environment.apiUrl}/api/v1/chats/${id}`).subscribe({
      next: (hilo) => {
        if (this.abierto?.id !== id) return;
        this.hilo = hilo;
        if (bajar) this.bajar();
        this.cdr.detectChanges();
      },
      error: (error: unknown) => {
        this.toast.error(problemText(error));
        this.cdr.detectChanges();
      },
    });
  }

  private unir(chats: ResumenChat[], formularios: MensajeFormulario[]): ItemChat[] {
    const vivos: ItemChat[] = chats.map((chat) => ({
      id: chat.id,
      vivo: true,
      nombre: chat.nombre,
      correo: chat.correo,
      telefono: chat.telefono,
      empresa: chat.empresa,
      preview: chat.clienteEscribiendo ? 'Escribiendo…' : (chat.ultimoTexto || 'Conversación'),
      cuando: chat.ultimoMensajeAt,
      noLeidos: chat.noLeidos ?? 0,
      escribiendo: chat.clienteEscribiendo,
      estado: chat.estado || 'ABIERTA',
      clienteId: chat.clienteId,
    }));
    const antiguos: ItemChat[] = formularios.map((mensaje) => ({
      id: mensaje.id,
      vivo: false,
      nombre: mensaje.nombre,
      correo: mensaje.correo,
      telefono: mensaje.telefono,
      empresa: mensaje.empresa,
      preview: mensaje.asunto || mensaje.mensaje,
      cuando: mensaje.createdAt,
      noLeidos: mensaje.estado === 'NUEVO' ? 1 : 0,
      escribiendo: false,
      cuerpo: mensaje.mensaje,
    }));
    return [...vivos, ...antiguos].sort((a, b) => (b.cuando ?? '').localeCompare(a.cuando ?? ''));
  }

  private fallo(error: unknown, conAviso: boolean): void {
    this.cargando = false;
    if (conAviso) this.toast.error(problemText(error));
    this.cdr.detectChanges();
  }

  private bajar(): void {
    window.setTimeout(() => {
      const caja = document.querySelector('.wa-cuerpo');
      if (caja) caja.scrollTop = caja.scrollHeight;
    }, 0);
  }
}

export const MENSAJES_WEB_ROUTES: Routes = [
  { path: '', component: MensajesWebPageComponent, title: 'Mensajes web', data: { section: 'Ventas', breadcrumb: 'Mensajes web' } },
];
