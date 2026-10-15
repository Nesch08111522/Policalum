// src/app/components/asistencia/listado-asistencias.component.ts
import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
    AsistenciaFilterDTO,
    RrhhAsistenciaRequestDTO,
    RrhhAsistenciaResponseDTO,
} from '../../models/asistencia.model';
import { UserInfoDTO } from '../../models/user.model';
import { AsistenciaService } from '../../services/asistencia.service';
import { TokenService } from '../../services/token.service';

@Component({
  selector: 'app-listado-asistencias',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './listado-asistencias.component.html',
  styleUrls: ['./listado-asistencias.component.css'],
})
export class ListadoAsistenciasComponent implements OnInit, AfterViewInit {
  asistencias: RrhhAsistenciaResponseDTO[] = [];
  isLoading: boolean = true;
  error: string | null = null;

  page: number = 0;
  size: number = 10;
  totalElements: number = 0;
  totalPages: number = 0;

  filtro: AsistenciaFilterDTO = {};
  tipos = ['ENTRADA', 'SALIDA', 'INICIO_RECESO', 'FIN_RECESO'];
  estados = ['PRESENTE', 'TARDANZA', 'AUSENTE'];
  isAdmin: boolean = false;
  empleadoId: string = '';

  empleados: UserInfoDTO[] = [];
  empleadosCargados: boolean = false;

  mostrarModalManual: boolean = false;
  manualData: RrhhAsistenciaRequestDTO = {
    empleadoId: '',
    fechaHora: '',
    tipoMarcacion: 'ENTRADA',
    dispositivoInfo: 'Registro Manual por Admin',
  };
  manualErrors: { [key: string]: string } = {};
  mensajeExito: string | null = null;

  constructor(
    private asistenciaService: AsistenciaService,
    private tokenService: TokenService,
    private cdr: ChangeDetectorRef,
  ) {
    const user = this.tokenService.getUser();
    this.isAdmin = user?.rol === 'ROLE_ADMIN';
    this.empleadoId = user?.id || '';
    console.log('👤 ListadoAsistencias - isAdmin:', this.isAdmin);
  }

  ngOnInit(): void {
    console.log('🔄 ngOnInit - ListadoAsistencias');
  }

  ngAfterViewInit(): void {
    console.log('🔄 ngAfterViewInit - ListadoAsistencias, cargando datos...');
    setTimeout(() => {
      this.cargarAsistencias();
      if (this.isAdmin) {
        this.cargarEmpleados();
      }
      this.cdr.detectChanges();
    }, 300);
  }

  cargarEmpleados(): void {
    if (this.empleadosCargados) return;

    console.log('🔄 Cargando empleados...');
    const token = this.tokenService.getToken();
    if (!token) {
      console.warn('⚠️ No hay token para empleados');
      setTimeout(() => this.cargarEmpleados(), 500);
      return;
    }

    this.asistenciaService.obtenerEmpleados().subscribe({
      next: (data) => {
        this.empleados = data;
        this.empleadosCargados = true;
        console.log('✅ Empleados cargados:', this.empleados.length);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('❌ Error cargando empleados:', err);
        this.empleados = [];
        this.cdr.detectChanges();
      },
    });
  }

  cargarAsistencias(): void {
    console.log('🔍 Cargando asistencias...');
    this.isLoading = true;
    this.error = null;

    const token = this.tokenService.getToken();
    if (!token) {
      console.warn('⚠️ No hay token, reintentando...');
      setTimeout(() => this.cargarAsistencias(), 500);
      return;
    }

    // ✅ CREAR UNA COPIA DEL FILTRO
    let filtroEnvio: AsistenciaFilterDTO = { ...this.filtro };

    // ✅ ELIMINAR FILTROS DE FECHA PARA VER TODAS LAS ASISTENCIAS
    delete filtroEnvio.fechaInicio;
    delete filtroEnvio.fechaFin;

    // ✅ SI EL EMPLEADOID ESTÁ VACÍO O ES "TODOS", ELIMINARLO
    if (!filtroEnvio.empleadoId || filtroEnvio.empleadoId === '') {
      delete filtroEnvio.empleadoId;
    }

    // ✅ SI NO ES ADMIN, FORZAR SU ID
    if (!this.isAdmin) {
      filtroEnvio.empleadoId = this.empleadoId;
    }

    console.log('📋 Filtro a enviar:', filtroEnvio);

    const metodo = this.isAdmin
      ? this.asistenciaService.listarAsistencias(filtroEnvio, this.page, this.size)
      : this.asistenciaService.misAsistencias(filtroEnvio, this.page, this.size);

    metodo.subscribe({
      next: (response) => {
        console.log('✅ Asistencias recibidas:', response);
        this.asistencias = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.totalPages = response.totalPages || 0;
        this.isLoading = false;
        this.cdr.detectChanges();
        console.log('✅ Asistencias actualizadas en UI:', this.asistencias.length);
      },
      error: (err) => {
        console.error('❌ Error cargando asistencias:', err);
        this.error = 'Error al cargar las asistencias';
        this.isLoading = false;
        this.asistencias = [];
        this.cdr.detectChanges();
      },
    });
  }

  onEmpleadoChange(): void {
    console.log('🔄 Empleado seleccionado:', this.filtro.empleadoId);
    this.page = 0;
    this.cargarAsistencias();
  }

  aplicarFiltros(): void {
    console.log('🔄 Aplicando filtros...');
    this.page = 0;
    this.cargarAsistencias();
  }

  limpiarFiltros(): void {
    console.log('🔄 Limpiando filtros...');
    this.filtro = {};
    this.page = 0;
    this.cargarAsistencias();
  }

  cambiarPagina(pagina: number): void {
    if (pagina >= 0 && pagina < this.totalPages) {
      this.page = pagina;
      this.cargarAsistencias();
    }
  }

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

  // ============================================
  // ✅ REGISTRO MANUAL (SOLO ADMIN)
  // ============================================

  abrirModalManual(): void {
    console.log('🔄 Abriendo modal de registro manual...');
    this.mostrarModalManual = true;
    this.manualData = {
      empleadoId: '',
      fechaHora: new Date().toISOString().slice(0, 16),
      tipoMarcacion: 'ENTRADA',
      dispositivoInfo: 'Registro Manual por Admin',
    };
    this.manualErrors = {};
    this.mensajeExito = null;
    this.cdr.detectChanges();
  }

  cerrarModalManual(): void {
    this.mostrarModalManual = false;
    this.manualErrors = {};
    this.mensajeExito = null;
    this.cdr.detectChanges();
  }

  validarManual(): boolean {
    this.manualErrors = {};
    let valido = true;

    if (!this.manualData.empleadoId) {
      this.manualErrors['empleado'] = 'Selecciona un empleado';
      valido = false;
    }

    if (!this.manualData.fechaHora) {
      this.manualErrors['fecha'] = 'Selecciona una fecha y hora';
      valido = false;
    } else {
      const fechaSeleccionada = new Date(this.manualData.fechaHora);
      const ahora = new Date();
      if (fechaSeleccionada > ahora) {
        this.manualErrors['fecha'] = 'No se puede registrar en el futuro';
        valido = false;
      }
    }

    if (!this.manualData.tipoMarcacion) {
      this.manualErrors['tipo'] = 'Selecciona un tipo';
      valido = false;
    }

    this.cdr.detectChanges();
    return valido;
  }

  registrarManual(): void {
    if (!this.validarManual()) {
      return;
    }

    console.log('📝 Registrando asistencia manual:', this.manualData);
    this.isLoading = true;
    this.mensajeExito = null;
    this.error = null;

    this.asistenciaService.registrarManual(this.manualData).subscribe({
      next: (response) => {
        console.log('✅ Asistencia manual registrada:', response);
        this.mensajeExito = '✅ Asistencia registrada correctamente';
        this.isLoading = false;
        this.cdr.detectChanges();

        setTimeout(() => {
          this.cerrarModalManual();
          this.cargarAsistencias();
          this.cdr.detectChanges();
        }, 1500);
      },
      error: (err) => {
        console.error('❌ Error registrando asistencia manual:', err);
        this.error = err.error?.message || 'Error al registrar asistencia';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }
}
