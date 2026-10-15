// src/app/components/qr-publico/qr-publico.component.ts
import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AsistenciaService } from '../../services/asistencia.service';
import { TokenService } from '../../services/token.service';

declare var QRCode: any;

@Component({
  selector: 'app-qr-publico',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './qr-publico.component.html',
  styleUrls: ['./qr-publico.component.css'],
})
export class QrPublicoComponent implements OnInit, OnDestroy, AfterViewInit {
  qrData: any = null;
  segundosRestantes: number = 60;
  isLoading: boolean = true;
  error: string | null = null;
  codigoManual: string = '';
  mostrandoInputManual: boolean = false;
  adminNombre: string = '';

  mensajeFlotante: string | null = null;
  tipoMensaje: 'success' | 'error' = 'success';
  mostrarMensajeFlotante: boolean = false;

  private timerInterval: any;
  private messageTimeout: any;

  constructor(
    private asistenciaService: AsistenciaService,
    private tokenService: TokenService,
    private cdr: ChangeDetectorRef,
  ) {
    const user = this.tokenService.getUser();
    this.adminNombre = user?.nombre || 'Administrador';
  }

  ngOnInit(): void {
  }

  // ✅ USAR NGAFTERVIEWINIT PARA CARGAR EL QR
  ngAfterViewInit(): void {
    setTimeout(() => {
      this.cargarQR();
      this.iniciarActualizacion();
      this.cdr.detectChanges();
    }, 300);
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
    if (this.messageTimeout) {
      clearTimeout(this.messageTimeout);
    }
  }

  cargarQR(): void {
    this.isLoading = true;
    this.error = null;

    const token = this.tokenService.getToken();
    if (!token) {
      console.warn('⚠️ No hay token, reintentando QR...');
      setTimeout(() => this.cargarQR(), 500);
      return;
    }

    this.asistenciaService.obtenerQr().subscribe({
      next: (data) => {
        this.qrData = data;
        this.segundosRestantes = data.segundosRestantes || 60; // ← REINICIAR CONTADOR
        this.isLoading = false;

        setTimeout(() => {
          this.generarQRCode(data.payload);
          this.cdr.detectChanges();
        }, 100);
      },
      error: (err) => {
        console.error('❌ Error cargando QR:', err);
        this.error = 'Error al cargar el QR';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  generarQRCode(texto: string): void {
    const container = document.getElementById('qrcode');

    if (!container) {
      console.warn('⚠️ Contenedor QR no encontrado, reintentando...');
      setTimeout(() => this.generarQRCode(texto), 300);
      return;
    }

    container.innerHTML = '';
    try {
      new QRCode(container, {
        text: texto,
        width: 220,
        height: 220,
        colorDark: '#1F2937',
        colorLight: '#FFFFFF',
        correctLevel: QRCode.CorrectLevel.H,
      });
      this.cdr.detectChanges();
    } catch (error) {
      console.error('❌ Error generando QR:', error);
    }
  }

  // src/app/components/qr-publico/qr-publico.component.ts

  iniciarActualizacion(): void {
    // ✅ ACTUALIZAR QR CADA 60 SEGUNDOS
    this.timerInterval = setInterval(() => {
      // ✅ Cuando llegue a 0, recargar el QR
      if (this.segundosRestantes <= 0) {
        this.cargarQR();
      } else {
        this.segundosRestantes--;
        this.cdr.detectChanges();
      }
    }, 1000);
  }

  refrescarQR(): void {
    this.cargarQR();
  }

  mostrarEscaneoManual(): void {
    this.mostrandoInputManual = !this.mostrandoInputManual;
  }

  marcarConCodigoManual(): void {
    if (!this.codigoManual || this.codigoManual.trim() === '') {
      this.mostrarMensaje('Ingresa el código QR escaneado', 'error');
      return;
    }

    this.isLoading = true;
    this.asistenciaService.marcarAsistenciaQr(this.codigoManual.trim()).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.codigoManual = '';
        this.mostrandoInputManual = false;
        const nombre = response.empleadoNombreCompleto || 'Empleado';
        this.mostrarMensaje(
          `✅ ${response.tipoMarcacion} registrada para ${nombre} a las ${new Date(response.fechaHora).toLocaleTimeString()}`,
          'success',
        );
        this.cargarQR();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isLoading = false;
        this.mostrarMensaje(err.error?.message || 'Error al registrar asistencia', 'error');
        this.cdr.detectChanges();
      },
    });
  }

  mostrarMensaje(mensaje: string, tipo: 'success' | 'error'): void {
    this.mensajeFlotante = mensaje;
    this.tipoMensaje = tipo;
    this.mostrarMensajeFlotante = true;
    this.cdr.detectChanges();

    if (this.messageTimeout) {
      clearTimeout(this.messageTimeout);
    }
    this.messageTimeout = setTimeout(() => {
      this.mostrarMensajeFlotante = false;
      this.mensajeFlotante = null;
      this.cdr.detectChanges();
    }, 5000);
  }

  forzarQR(): void {
    const testText = 'POLICALUM|PUBLICO|' + Date.now() + '|123456';
    this.qrData = {
      payload: testText,
      empleadoNombre: 'Test Manual',
      segundosRestantes: 60,
    };
    this.isLoading = false;
    this.generarQRCode(testText);
    this.cdr.detectChanges();
  }

  getPorcentajeProgreso(): number {
    return (this.segundosRestantes / 60) * 100;
  }

  getColorBarra(): string {
    if (this.segundosRestantes <= 10) return 'bg-danger';
    if (this.segundosRestantes <= 20) return 'bg-warning';
    return 'bg-success';
  }
}
