// src/app/components/empleado/empleado-dashboard.component.ts
import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  AsistenciaEstadoDTO,
  AsistenciaFilterDTO,
  RrhhAsistenciaResponseDTO,
} from '../../models/asistencia.model';
import { AsistenciaService } from '../../services/asistencia.service';
import { TokenService } from '../../services/token.service';

declare var Html5Qrcode: any;

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-empleado-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './empleado-dashboard.component.html',
  styleUrls: ['./empleado-dashboard.component.css'],
})
export class EmpleadoDashboardComponent implements OnInit, OnDestroy {
  // ============================================
  // DATOS DEL EMPLEADO
  // ============================================
  empleadoNombre: string = '';
  empleadoId: string = '';

  // ============================================
  // ESTADO DEL DÍA
  // ============================================
  estadoDia: AsistenciaEstadoDTO | null = null;

  // ============================================
  // ESCÁNER QR
  // ============================================
  escanerActivo: boolean = false;
  escanerIniciado: boolean = false;
  html5QrCode: any;
  mensajeEscaneo: string | null = null;
  mensajeErrorEscaneo: string | null = null;
  isLoadingEscaneo: boolean = false;
  codigoManual: string = '';
  mostrandoInputManual: boolean = false;

  // ============================================
  // MIS ASISTENCIAS (SOLO LAS PROPIAS)
  // ============================================
  asistencias: RrhhAsistenciaResponseDTO[] = [];
  isLoadingAsistencias: boolean = false;
  errorAsistencias: string | null = null;

  page: number = 0;
  size: number = 10;
  totalElements: number = 0;
  totalPages: number = 0;

  filtro: AsistenciaFilterDTO = {};
  tipos = ['ENTRADA', 'SALIDA', 'INICIO_RECESO', 'FIN_RECESO'];
  estados = ['PRESENTE', 'TARDANZA', 'AUSENTE'];

  constructor(
    private tokenService: TokenService,
    private asistenciaService: AsistenciaService,
    private router: Router,
  ) {
    const user = this.tokenService.getUser();
    this.empleadoNombre = user?.nombre || 'Empleado';
    this.empleadoId = user?.id || '';
  }

  ngOnInit(): void {
    this.cargarEstadoDia();
    this.cargarMisAsistencias();
    setTimeout(() => {
      this.iniciarEscaner();
    }, 1000);
  }

  ngOnDestroy(): void {
    this.detenerEscaner();
  }

  // ============================================
  // ✅ ESCÁNER QR
  // ============================================

  iniciarEscaner(): void {
    if (this.escanerIniciado) return;

    this.escanerActivo = true;
    this.escanerIniciado = true;
    this.mensajeEscaneo = null;
    this.mensajeErrorEscaneo = null;

    const element = document.getElementById('reader');
    if (!element) {
      this.mensajeErrorEscaneo = 'Error al iniciar la cámara';
      return;
    }

    try {
      this.html5QrCode = new Html5Qrcode('reader');

      const config = {
        fps: 10,
        qrbox: { width: 250, height: 250 },
        aspectRatio: 1.0,
      };

      this.html5QrCode.start(
        { facingMode: 'environment' },
        config,
        (decodedText: string) => {
          this.marcarAsistencia(decodedText);
        },
        (errorMessage: any) => {
          // Ignorar errores de escaneo
        },
      );
    } catch (error) {
      console.error('Error iniciando escáner:', error);
      this.mensajeErrorEscaneo = 'Error al acceder a la cámara. Verifica los permisos.';
      this.escanerActivo = false;
      this.escanerIniciado = false;
    }
  }

  detenerEscaner(): void {
    if (this.html5QrCode && this.escanerIniciado) {
      try {
        this.html5QrCode.stop();
        this.html5QrCode.clear();
      } catch (error) {
      }
    }
    this.escanerActivo = false;
    this.escanerIniciado = false;
  }

  marcarAsistencia(payload: string): void {
    this.isLoadingEscaneo = true;
    this.mensajeEscaneo = null;
    this.mensajeErrorEscaneo = null;

    this.detenerEscaner();

    this.asistenciaService.marcarAsistenciaQr(payload).subscribe({
      next: (response) => {
        this.isLoadingEscaneo = false;
        this.mensajeEscaneo = `✅ ${response.tipoMarcacion} registrada a las ${new Date(response.fechaHora).toLocaleTimeString()}`;

        this.cargarEstadoDia();
        this.cargarMisAsistencias();

        setTimeout(() => {
          this.mensajeEscaneo = null;
          if (!this.mostrandoInputManual) {
            this.iniciarEscaner();
          }
        }, 3000);
      },
      error: (err) => {
        console.error('❌ Error registrando asistencia:', err);
        this.isLoadingEscaneo = false;
        this.mensajeErrorEscaneo =
          err.error?.message || 'Error al registrar asistencia. Intenta nuevamente.';

        setTimeout(() => {
          this.mensajeErrorEscaneo = null;
          if (!this.mostrandoInputManual) {
            this.iniciarEscaner();
          }
        }, 3000);
      },
    });
  }

  toggleManualInput(): void {
    this.mostrandoInputManual = !this.mostrandoInputManual;
    if (this.mostrandoInputManual) {
      this.detenerEscaner();
    } else {
      this.iniciarEscaner();
    }
  }

  marcarManual(): void {
    if (!this.codigoManual || this.codigoManual.trim() === '') {
      this.mensajeErrorEscaneo = 'Ingresa el código QR escaneado';
      return;
    }
    this.detenerEscaner();
    this.marcarAsistencia(this.codigoManual.trim());
  }

  // ============================================
  // ✅ ESTADO DEL DÍA
  // ============================================

  cargarEstadoDia(): void {
    this.asistenciaService.obtenerEstadoDelDia().subscribe({
      next: (data) => {
        this.estadoDia = data;
      },
      error: (err) => {
        console.error('Error cargando estado del día:', err);
      },
    });
  }

  // ============================================
  // ✅ MIS ASISTENCIAS (SOLO LAS PROPIAS)
  // ============================================

  cargarMisAsistencias(): void {
    this.isLoadingAsistencias = true;
    this.errorAsistencias = null;

    // ✅ Forzar el ID del empleado autenticado
    this.filtro.empleadoId = this.empleadoId;

    this.asistenciaService.misAsistencias(this.filtro, this.page, this.size).subscribe({
      next: (response) => {
        this.asistencias = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.totalPages = response.totalPages || 0;
        this.isLoadingAsistencias = false;
      },
      error: (err) => {
        console.error('Error cargando mis asistencias:', err);
        this.errorAsistencias = 'Error al cargar tus asistencias';
        this.isLoadingAsistencias = false;
        this.asistencias = [];
      },
    });
  }

  aplicarFiltros(): void {
    this.page = 0;
    this.cargarMisAsistencias();
  }

  limpiarFiltros(): void {
    this.filtro = {};
    this.page = 0;
    this.cargarMisAsistencias();
  }

  cambiarPagina(pagina: number): void {
    if (pagina >= 0 && pagina < this.totalPages) {
      this.page = pagina;
      this.cargarMisAsistencias();
    }
  }

  getEstadoClass(estado: string): string {
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
    this.detenerEscaner();
    this.tokenService.logout();
    this.router.navigate(['/login']);
  }
}
