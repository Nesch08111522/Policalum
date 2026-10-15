// src/app/components/asistencia/asistencia.ts
import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AsistenciaService } from '../../services/asistencia.service';
import { TokenService } from '../../services/token.service';
import { QrPublicoComponent } from '../qr-publico/qr-publico.component';
import { ListadoAsistenciasComponent } from './listado-asistencias.component';

@Component({
  selector: 'app-asistencia',
  standalone: true,
  imports: [CommonModule, FormsModule, QrPublicoComponent, ListadoAsistenciasComponent],
  templateUrl: './asistencia.component.html',
  styleUrls: ['./asistencia.css'],
})
export class AsistenciaComponent implements OnInit, AfterViewInit {
  // ============================================
  // ✅ DASHBOARD
  // ============================================
  totalEmpleados: number = 0;
  presentesHoy: number = 0;
  tardanzasHoy: number = 0;
  ausentesHoy: number = 0;
  isAdmin: boolean = false;
  adminName: string = '';

  // ✅ Inyectar ChangeDetectorRef
  constructor(
    private asistenciaService: AsistenciaService,
    private tokenService: TokenService,
    private cdr: ChangeDetectorRef, // ← Inyectar ChangeDetectorRef
  ) {
    const user = this.tokenService.getUser();
    this.isAdmin = user?.rol === 'ROLE_ADMIN';
    this.adminName = user?.nombre || 'Administrador';
    console.log('👤 Usuario:', user?.email, 'Rol:', user?.rol);
  }

  ngOnInit(): void {
    console.log('🔄 ngOnInit - Iniciando AsistenciaComponent');
  }

  // ✅ USAR NGAFTERVIEWINIT (después de que la vista esté lista)
  ngAfterViewInit(): void {
    console.log('🔄 ngAfterViewInit - Vista lista, cargando datos...');

    // ✅ Dar tiempo para que el token esté disponible
    setTimeout(() => {
      this.cargarDashboard();
      // ✅ Forzar actualización después de cargar
      this.cdr.detectChanges();
    }, 300);
  }

  // ============================================
  // ✅ DASHBOARD CON FORZADO DE ACTUALIZACIÓN
  // ============================================
  cargarDashboard(): void {
    console.log('📡 Cargando dashboard...');

    // ✅ Verificar token antes de hacer la petición
    const token = this.tokenService.getToken();
    if (!token) {
      console.warn('⚠️ No hay token, reintentando...');
      setTimeout(() => this.cargarDashboard(), 500);
      return;
    }

    this.asistenciaService.obtenerDashboard().subscribe({
      next: (data) => {
        console.log('✅ Dashboard recibido:', data);
        this.totalEmpleados = data.totalEmpleados || 0;
        this.presentesHoy = data.presentesHoy || 0;
        this.tardanzasHoy = data.tardanzasHoy || 0;
        this.ausentesHoy = data.ausentesHoy || 0;

        // ✅ FORZAR ACTUALIZACIÓN
        this.cdr.detectChanges();
        console.log('✅ Dashboard actualizado en UI');
      },
      error: (err) => {
        console.error('❌ Error cargando dashboard:', err);
        this.cdr.detectChanges();
      },
    });
  }
}
