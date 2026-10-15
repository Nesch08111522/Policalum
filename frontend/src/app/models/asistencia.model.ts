// src/app/models/asistencia.model.ts

// ✅ Usa los mismos nombres que en el backend
export interface RrhhAsistenciaResponseDTO {
  id: string;
  empleadoId: string;
  empleadoNombreCompleto: string;
  fechaHora: string;
  tipoMarcacion: 'ENTRADA' | 'SALIDA' | 'INICIO_RECESO' | 'FIN_RECESO';
  estado?: 'PRESENTE' | 'TARDANZA' | 'AUSENTE';
  dispositivoInfo: string;
  turnoCajaId?: string;
}

// ✅ Para registrar manualmente (coincide con RrhhAsistenciaRequestDTO del backend)
export interface RrhhAsistenciaRequestDTO {
  empleadoId: string;
  fechaHora: string;
  tipoMarcacion: 'ENTRADA' | 'SALIDA' | 'INICIO_RECESO' | 'FIN_RECESO';
  dispositivoInfo?: string;
  turnoCajaId?: string;
}

// ✅ Para filtros
export interface AsistenciaFilterDTO {
  empleadoId?: string;
  fechaInicio?: string;
  fechaFin?: string;
  tipoMarcacion?: string;
  estado?: string;
}

// ✅ Dashboard
export interface DashboardAsistenciaDTO {
  totalEmpleados: number;
  presentesHoy: number;
  ausentesHoy: number;
  tardanzasHoy: number;
  asistenciasRecientes: RrhhAsistenciaResponseDTO[];
}

// ✅ Estado del día
export interface AsistenciaEstadoDTO {
  yaMarcoEntrada: boolean;
  yaMarcoSalida: boolean;
  mensajeEntrada: string;
  mensajeSalida: string;
}

// ✅ QR (ya lo tienes, pero lo incluimos)
export interface AsistenciaQrDTO {
  payload: string;
  empleadoId: string;
  empleadoNombre: string;
  segundosRestantes: number;
  expiraEnEpoch: number;
}
