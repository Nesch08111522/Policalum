import { CommonModule, DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router, RouterLink, Routes } from '@angular/router';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { ToastService } from '../../shared/services/toast.service';
import { problemText } from '../../shared/utils/feedback';
import { Notificacion, NotificacionesApi } from './notificaciones.api';

@Component({
  selector: 'app-notificaciones-page',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterLink, PageHeaderComponent],
  templateUrl: './notificaciones-page.component.html',
  styleUrls: ['./notificaciones-page.component.css'],
})
export class NotificacionesPageComponent implements OnInit {
  avisos: Notificacion[] = [];
  filtro: 'todas' | 'nuevas' | 'pasadas' = 'todas';
  cargando = true;

  constructor(private readonly api: NotificacionesApi, private readonly router: Router, private readonly toast: ToastService, private readonly cdr: ChangeDetectorRef) {}

  ngOnInit(): void { this.cargar(); }

  get visibles(): Notificacion[] {
    if (this.filtro === 'nuevas') return this.avisos.filter((aviso) => !aviso.leida && !aviso.eliminada);
    if (this.filtro === 'pasadas') return this.avisos.filter((aviso) => aviso.leida || aviso.eliminada);
    return this.avisos;
  }

  cargar(): void {
    this.cargando = true;
    this.api.listar().subscribe({
      next: (avisos) => { this.avisos = avisos; this.cargando = false; this.cdr.detectChanges(); },
      error: (error: unknown) => { this.cargando = false; this.toast.error(problemText(error)); this.cdr.detectChanges(); },
    });
  }

  abrir(aviso: Notificacion): void {
    const ir = () => void this.router.navigateByUrl(aviso.ruta || '/modules/stock?tab=bajo');
    if (aviso.leida) { ir(); return; }
    this.api.leida(aviso.id).subscribe({ next: () => { aviso.leida = true; ir(); }, error: () => ir() });
  }

  leida(aviso: Notificacion): void {
    this.api.leida(aviso.id).subscribe({
      next: () => { aviso.leida = true; this.toast.success('Notificación marcada como leída.'); this.cdr.detectChanges(); },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  leerTodas(): void {
    this.api.leerTodas().subscribe({
      next: () => { this.avisos.forEach((aviso) => { if (!aviso.eliminada) aviso.leida = true; }); this.toast.success('Notificaciones marcadas como leídas.'); this.cdr.detectChanges(); },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  archivar(aviso: Notificacion): void {
    this.api.archivar(aviso.id).subscribe({
      next: () => { aviso.eliminada = true; aviso.leida = true; this.toast.success('La notificación queda en el historial 30 días.'); this.cdr.detectChanges(); },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }
}

export const NOTIFICACIONES_ROUTES: Routes = [
  { path: '', component: NotificacionesPageComponent, title: 'Notificaciones', data: { section: 'Inicio', breadcrumb: 'Notificaciones' } },
];
