import { CommonModule, DatePipe } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Routes } from '@angular/router';
import { environment } from '../../../environments/environment';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { ToastService } from '../../shared/services/toast.service';
import { problemText } from '../../shared/utils/feedback';

export interface MensajeWeb {
  id: string;
  nombre: string;
  correo: string;
  telefono?: string;
  empresa?: string;
  asunto: string;
  mensaje: string;
  estado: 'NUEVO' | 'GESTIONADO';
  notaGestion?: string;
  createdAt?: string;
  gestionadoAt?: string;
}

interface Pagina {
  content?: MensajeWeb[];
}

@Component({
  selector: 'app-mensajes-web-page',
  standalone: true,
  imports: [CommonModule, DatePipe, ReactiveFormsModule, PageHeaderComponent],
  templateUrl: './mensajes-web-page.component.html',
  styleUrls: ['./mensajes-web-page.component.css'],
})
export class MensajesWebPageComponent implements OnInit {
  mensajes: MensajeWeb[] = [];
  filtro: '' | 'NUEVO' | 'GESTIONADO' = 'NUEVO';
  cargando = true;
  abierto: MensajeWeb | null = null;
  guardando = false;
  private readonly fb = inject(FormBuilder);
  readonly nota = this.fb.nonNullable.control('');

  constructor(
    private readonly http: HttpClient,
    private readonly toast: ToastService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.cargando = true;
    let params = new HttpParams().set('size', '50');
    if (this.filtro) params = params.set('estado', this.filtro);
    this.http.get<Pagina>(`${environment.apiUrl}/api/v1/mensajes-web`, { params }).subscribe({
      next: (pagina) => {
        this.mensajes = pagina.content ?? [];
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (error: unknown) => {
        this.cargando = false;
        this.toast.error(problemText(error));
        this.cdr.detectChanges();
      },
    });
  }

  cambiar(filtro: '' | 'NUEVO' | 'GESTIONADO'): void {
    this.filtro = filtro;
    this.abierto = null;
    this.cargar();
  }

  ver(mensaje: MensajeWeb): void {
    this.abierto = mensaje;
    this.nota.setValue(mensaje.notaGestion ?? '');
  }

  gestionar(): void {
    if (!this.abierto) return;
    this.guardando = true;
    this.http.patch<MensajeWeb>(`${environment.apiUrl}/api/v1/mensajes-web/${this.abierto.id}/gestionar`, { nota: this.nota.value }).subscribe({
      next: () => {
        this.guardando = false;
        this.toast.success('Mensaje marcado como gestionado.');
        this.abierto = null;
        this.cargar();
      },
      error: (error: unknown) => {
        this.guardando = false;
        this.toast.error(problemText(error));
      },
    });
  }
}

export const MENSAJES_WEB_ROUTES: Routes = [
  { path: '', component: MensajesWebPageComponent, title: 'Mensajes web', data: { section: 'Ventas', breadcrumb: 'Mensajes web' } },
];
