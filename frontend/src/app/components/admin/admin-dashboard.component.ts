// src/app/components/admin/admin-dashboard.component.ts
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DashboardAsistenciaDTO, RrhhAsistenciaResponseDTO } from '../../models/asistencia.model';
import { AsistenciaService } from '../../services/asistencia.service';
import { TokenService } from '../../services/token.service';
// ❌ ELIMINA QrPublicoComponent si no lo usas
// import { QrPublicoComponent } from '../qr-publico/qr-publico.component';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule], // ✅ Solo CommonModule
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css'], // ✅ CORREGIDO
})
export class AdminDashboardComponent implements OnInit {
  adminName: string = '';
  adminRole: string = 'Administrador';

  totalEmpleados: number = 0;
  presentesHoy: number = 0;
  tardanzasHoy: number = 0;
  ausentesHoy: number = 0;

  usuariosSistema: number = 0;
  reportesDiarios: number = 0;
  incidentes: number = 0;
  empleadosRiesgoAlto: number = 0;
  faltasHoy: number = 0;
  puntualidad: number = 0;
  ranking: any[] = [];
  feedbackPendientes: number = 0;

  asistenciasRecientes: RrhhAsistenciaResponseDTO[] = [];

  constructor(
    private tokenService: TokenService,
    private router: Router,
    private asistenciaService: AsistenciaService,
  ) {
    const user = this.tokenService.getUser();
    this.adminName = user?.nombre || 'Administrador';
  }

  ngOnInit(): void {
    this.cargarDashboard();
  }

  cargarDatos(): void {
    this.cargarDashboard();
  }

  cargarDashboard(): void {
    this.asistenciaService.obtenerDashboard().subscribe({
      next: (data: DashboardAsistenciaDTO) => {
        console.log('📊 Datos del dashboard:', data);
        
        this.totalEmpleados = data.totalEmpleados;
        this.presentesHoy = data.presentesHoy;
        this.tardanzasHoy = data.tardanzasHoy;
        this.ausentesHoy = data.ausentesHoy;
        this.asistenciasRecientes = data.asistenciasRecientes || [];

        this.usuariosSistema = data.totalEmpleados;
        this.faltasHoy = data.ausentesHoy;
        
        this.puntualidad = data.totalEmpleados > 0 
          ? Math.round((data.presentesHoy / data.totalEmpleados) * 100) 
          : 0;

        this.reportesDiarios = Math.round(data.totalEmpleados * 0.3);
        this.incidentes = Math.round(data.totalEmpleados * 0.05);
        this.empleadosRiesgoAlto = Math.round(data.totalEmpleados * 0.08);
        this.feedbackPendientes = Math.round(data.totalEmpleados * 0.15);
        
        this.ranking = [
          { nombre: 'María González', puntaje: 98 },
          { nombre: 'Carlos Pérez', puntaje: 95 },
          { nombre: 'Ana Martínez', puntaje: 92 },
          { nombre: 'Luis Rodríguez', puntaje: 88 },
          { nombre: 'Laura Sánchez', puntaje: 85 },
        ];
      },
      error: (err) => {
        console.error('❌ Error cargando dashboard:', err);
        this.usuariosSistema = 0;
        this.faltasHoy = 0;
        this.puntualidad = 0;
        this.reportesDiarios = 0;
        this.incidentes = 0;
        this.empleadosRiesgoAlto = 0;
        this.feedbackPendientes = 0;
        this.ranking = [];
      },
    });
  }

  verTodasLasAsistencias(): void {
    this.router.navigate(['/asistencias']);
  }

  // ✅ CORREGIDO - Acepta estado opcional
  getEstadoClass(estado?: string): string {
    if (!estado) return 'badge-default';
    const clases: Record<string, string> = {
      PRESENTE: 'badge-presente',
      TARDANZA: 'badge-tardanza',
      AUSENTE: 'badge-ausente',
    };
    return clases[estado] || 'badge-default';
  }

  getTipoClass(tipo: string): string {
    const clases: Record<string, string> = {
      ENTRADA: 'badge-entrada',
      SALIDA: 'badge-salida',
      INICIO_RECESO: 'badge-receso',
      FIN_RECESO: 'badge-receso',
    };
    return clases[tipo] || 'badge-default';
  }

  logout(): void {
    this.tokenService.logout();
    this.router.navigate(['/login']);
  }
}