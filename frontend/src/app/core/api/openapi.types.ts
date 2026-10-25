// Archivo generado desde backend/docs/openapi.json. No editar manualmente.

export type JsonValue = string | number | boolean | null | JsonValue[] | { [key: string]: JsonValue };
export interface ApiPage<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number; first?: boolean; last?: boolean; }
export interface ApiProblem { status?: number; code?: string; message?: string; fieldErrors?: Record<string, string>; traceId?: string; errores?: Array<{ fila?: number; motivo?: string }>; }
export interface ActividadTablaDTO {
  cantidad?: number;
  tabla?: string;
}

export interface ActividadUsuarioDTO {
  cantidad?: number;
  email?: string;
  nombre?: string;
  userId?: string;
}

export interface AjusteStockRequestDTO {
  almacenId: string;
  cantidad: number;
  justificacion?: string;
  motivo: string;
  productoId: string;
}

export interface AjusteStockResponseDTO {
  almacenId?: string;
  almacenNombre?: string;
  cantidad?: number;
  codigo?: string;
  codigoSku?: string;
  costoUnitario?: number;
  createdAt?: string;
  id?: string;
  justificacion?: string;
  kardexMovimientoId?: string;
  motivo?: string;
  productoId?: string;
  productoNombre?: string;
  sedeId?: string;
  sedeNombre?: string;
  stockAnterior?: number;
  stockResultante?: number;
  tipoMovimiento?: "ENTRADA" | "SALIDA";
  unidad?: string;
  userId?: string;
  userNombre?: string;
  valor?: number;
}

export interface Almacen {
  activo?: boolean;
  createdAt?: string;
  descripcion?: string;
  id?: string;
  nombre?: string;
  sede?: Sede;
}

export interface AlmacenRequestDTO {
  activo: boolean;
  descripcion?: string;
  nombre: string;
  sede: Sede;
}

export interface AlmacenResponseDTO {
  activo?: boolean;
  createdAt?: string;
  descripcion?: string;
  id?: string;
  nombre?: string;
  sede?: SedeResponseDTO;
}

export interface AlmacenResumenDTO {
  almacenId?: string;
  almacenNombre?: string;
  sedeId?: string;
  sedeNombre?: string;
}

export interface AnularCompraRequestDTO {
  motivo: string;
}

export interface AnularRetiroRequestDTO {
  motivo: string;
}

export interface AnularVentaRequestDTO {
  motivo: string;
}

export interface AperturaCajaRequestDTO {
  montoInicial: number;
  observaciones?: string;
  sedeId: string;
}

export interface AprobarDevolucionRequestDTO {
  observacion?: string;
}

export interface ArqueoTurnoDTO {
  apertura?: string;
  cajero?: string;
  cierre?: string;
  declarado?: number;
  diferencia?: number;
  efectivoVentasAnuladas?: number;
  esperado?: number;
  estado?: "ABIERTO" | "CERRADO";
  montoInicial?: number;
  reembolsos?: number;
  resultado?: "ABIERTO" | "CUADRADO" | "FALTANTE" | "SOBRANTE" | "SIN_DECLARAR";
  retirosSocios?: number;
  sede?: string;
  turnoId?: string;
  ventasEfectivo?: number;
}

export interface ArqueosReporteDTO {
  desde?: string;
  hasta?: string;
  pagina?: number;
  resumen?: ResumenArqueosDTO;
  sedeId?: string;
  tamano?: number;
  totalElementos?: number;
  totalPaginas?: number;
  turnos?: Array<ArqueoTurnoDTO>;
  usuarioId?: string;
}

export interface AsignacionTurnoResponseDTO {
  creados?: number;
  turnos?: Array<TurnoTrabajoResponseDTO>;
}

export interface AsignarConductorRequestDTO {
  conductorId?: string;
}

export interface AsignarRepartidorRequestDTO {
  movilidadId?: string;
  responsableId: string;
}

export interface AsignarTurnoRequestDTO {
  diasSemana: Array<string>;
  horaFin: string;
  horaInicio: string;
  nombreTurno: string;
  sedeId?: string;
  toleranciaMinutos?: number;
  userIds: Array<string>;
}

export interface AsistenciaEstadoDTO {
  mensajeEntrada?: string;
  mensajeSalida?: string;
  yaMarcoEntrada?: boolean;
  yaMarcoSalida?: boolean;
}

export interface AsistenciaQrDTO {
  empleadoId?: string;
  expiraEnSegundos?: number;
  payload?: string;
}

export interface AuditoriaDashboardDTO {
  desde?: string;
  eventosDelSistema?: number;
  eventosPorAccion?: Record<string, unknown>;
  eventosPorDia?: Array<ConteoDiarioDTO>;
  hasta?: string;
  logsSinFecha?: number;
  topTablas?: Array<ActividadTablaDTO>;
  topUsuarios?: Array<ActividadUsuarioDTO>;
  totalEventos?: number;
}

export interface BoletaConceptoDTO {
  concepto?: string;
  monto?: number;
}

export interface BoletaPagoDTO {
  descuentos?: Array<BoletaConceptoDTO>;
  diasComputados?: number;
  empleadoEmail?: string;
  empleadoId?: string;
  empleadoNombre?: string;
  estado?: "PENDIENTE" | "APROBADA" | "PAGADA";
  fechaIngreso?: string;
  fechaPago?: string;
  ingresos?: Array<BoletaConceptoDTO>;
  netoAPagar?: number;
  nominaId?: string;
  periodo?: string;
  sedeNombre?: string;
  tipoContrato?: string;
  totalDescuentos?: number;
  totalIngresos?: number;
}

export interface CalcularComisionesRequestDTO {
  periodo: string;
  sedeId?: string;
}

export interface CalculoComisionesResponseDTO {
  actualizadas?: number;
  advertencias?: Array<string>;
  comisiones?: Array<ComisionResponseDTO>;
  nuevas?: number;
  omitidas?: number;
  periodo?: string;
  sedeId?: string;
  totalComisiones?: number;
}

export interface CambiarEstadoDespachoRequestDTO {
  estado: "PENDIENTE" | "EN_CAMINO" | "ENTREGADO" | "FALLIDO";
  fechaProgramada?: string;
  motivo?: string;
}

export interface CancelarPreformaRequestDTO {
  motivo?: string;
}

export interface CargaRepartidorDTO {
  enCamino?: number;
  nombre?: string;
  pendientes?: number;
  responsableId?: string;
}

export interface CatalogoFraccion {
  activo?: boolean;
  factorDescuentoKardex?: number;
  id?: string;
  nombreFraccion?: string;
  precioVenta?: number;
  producto?: Producto;
}

export interface CatalogoFraccionRequestDTO {
  activo?: boolean;
  factorDescuentoKardex: number;
  nombreFraccion: string;
  precioVenta?: number;
  productoId: string;
}

export interface CatalogoFraccionResponseDTO {
  activo?: boolean;
  factorDescuentoKardex?: number;
  id?: string;
  nombreFraccion?: string;
  precioVenta?: number;
  /** Añadido en el DTO de respuesta. Requiere regenerar el cliente OpenAPI. */
  productoId?: string;
  /** Añadido en el DTO de respuesta. Requiere regenerar el cliente OpenAPI. */
  productoNombre?: string;
}

export interface Categoria {
  activa?: boolean;
  id?: string;
  nombre?: string;
  parent?: Categoria;
  slug?: string;
}

export interface CategoriaRequestDTO {
  activa: boolean;
  nombre: string;
  parent?: Categoria;
  slug: string;
}

export interface CategoriaResponseDTO {
  id?: string;
  activa?: boolean;
  nombre?: string;
  parent?: Categoria;
  slug?: string;
}

export interface CategoriaSummaryDTO {
  id?: string;
  nombre?: string;
}

export interface CierreCajaRequestDTO {
  montoFinalDeclarado: number;
  observaciones?: string;
}

export interface ClienteFrecuenteDTO {
  clienteId?: string;
  compras?: number;
  devuelto?: number;
  diasDesdeUltimaCompra?: number;
  diasEntreCompras?: number;
  montoNeto?: number;
  montoTotal?: number;
  nombre?: string;
  numeroDocumento?: string;
  posicion?: number;
  primeraCompra?: string;
  ticketPromedio?: number;
  tipoDocumento?: string;
  ultimaCompra?: string;
}

export interface ClienteRequestDTO {
  celular?: string;
  direccion?: string;
  email?: string;
  nombre: string;
  numeroDocumento: string;
  tipoDocumento?: "DNI" | "RUC" | "CE" | "PASAPORTE";
}

export interface ClienteResponseDTO {
  celular?: string;
  createdAt?: string;
  direccion?: string;
  email?: string;
  id?: string;
  nombre?: string;
  numeroDocumento?: string;
  tipoDocumento?: "DNI" | "RUC" | "CE" | "PASAPORTE";
  activo?: boolean;
}

export interface ClientesFrecuentesDTO {
  clientesConCompras?: number;
  criterio?: "CANTIDAD" | "MONTO" | "FRECUENCIA";
  desde?: string;
  hasta?: string;
  items?: Array<ClienteFrecuenteDTO>;
  montoSinCliente?: number;
  sedeId?: string;
  top?: number;
  ventasSinCliente?: number;
}

export interface ComisionResponseDTO {
  aprobadoPorNombre?: string;
  cantidadVentas?: number;
  empleadoNombre?: string;
  estado?: "CALCULADA" | "APROBADA" | "LIQUIDADA";
  fechaAprobacion?: string;
  id?: string;
  montoComision?: number;
  periodo?: string;
  porcentajeComision?: number;
  sedeId?: string;
  sedeNombre?: string;
  totalVentasNetas?: number;
  updatedAt?: string;
  userId?: string;
}

export interface CompraAdjuntoResponseDTO {
  contentType?: string;
  createdAt?: string;
  id?: string;
  nombreOriginal?: string;
  subidoPorId?: string;
  subidoPorNombre?: string;
  tamanoBytes?: number;
  tipo?: string;
  urlArchivo?: string;
}

export interface CompraListadoDTO {
  createdAt?: string;
  estadoCompra?: "BORRADOR" | "RECIBIDA" | "ANULADA";
  estadoPago?: "PENDIENTE" | "PAGADO_PARCIAL" | "PAGADO";
  fechaEmision?: string;
  id?: string;
  moneda?: "PEN" | "USD" | "EUR";
  numeroFacturaProveedor?: string;
  numeroOrdenCompra?: string;
  proveedorNombre?: string;
  total?: number;
}

export interface CompraRequestDTO {
  detalles: Array<DetalleCompraCommandDTO>;
  fechaEmision: string;
  moneda?: "PEN" | "USD" | "EUR";
  numeroFacturaProveedor?: string;
  observaciones?: string;
  proveedorId: string;
  sedeId: string;
}

export interface CompraResponseDTO {
  adjuntos?: Array<CompraAdjuntoResponseDTO>;
  createdAt?: string;
  detalles?: Array<DetalleCompraResponseDTO>;
  estadoCompra?: "BORRADOR" | "RECIBIDA" | "ANULADA";
  estadoPago?: "PENDIENTE" | "PAGADO_PARCIAL" | "PAGADO";
  estadoRecepcion?: "PENDIENTE" | "RECIBIDO_PARCIAL" | "RECIBIDO";
  fechaAnulacion?: string;
  fechaEmision?: string;
  fechaVencimientoPago?: string;
  id?: string;
  moneda?: "PEN" | "USD" | "EUR";
  motivoAnulacion?: string;
  numeroFacturaProveedor?: string;
  numeroOrdenCompra?: string;
  observaciones?: string;
  proveedorDocumento?: string;
  proveedorId?: string;
  proveedorNombre?: string;
  sedeId?: string;
  sedeNombre?: string;
  subtotalNeto?: number;
  total?: number;
  totalIgv?: number;
  userId?: string;
  userNombre?: string;
}

export interface ConfiguracionPublicaDTO {
  logoUrl?: string;
  nombreComercial?: string;
}

export interface ConfirmacionPreformaDTO {
  preforma?: PreformaVentaResponseDTO;
  venta?: VentaProcesadaResponseDTO;
}

export interface ConfirmarPreformaRequestDTO {
  almacenId: string;
  canalVenta?: "LOCAL" | "ONLINE";
  pagos: Array<PagoVentaCommandDTO>;
  tipoComprobante: "BOLETA" | "FACTURA" | "TICKET";
}

export interface ConteoDiarioDTO {
  cantidad?: number;
  fecha?: string;
}

export interface ConvertirCotizacionRequestDTO {
  almacenId: string;
  canalVenta?: "LOCAL" | "ONLINE";
  lineas?: Array<DetalleCotizacionCommandDTO>;
  pagos: Array<PagoVentaCommandDTO>;
  tipoComprobante: "BOLETA" | "FACTURA" | "TICKET";
  turnoCajaId: string;
}

export interface CotizacionListadoDTO {
  clienteId?: string;
  clienteNombre?: string;
  createdAt?: string;
  estado?: "EMITIDA" | "ACEPTADA" | "RECHAZADA" | "VENCIDA" | "CONVERTIDA" | "NO_COMPRADO";
  fechaVigencia?: string;
  id?: string;
  sedeId?: string;
  total?: number;
}

export interface CotizacionRequestDTO {
  clienteId?: string;
  fechaVigencia?: string;
  lineas: Array<DetalleCotizacionCommandDTO>;
  observaciones?: string;
  sedeId: string;
}

export interface CotizacionResponseDTO {
  clienteId?: string;
  clienteNombre?: string;
  createdAt?: string;
  estado?: "EMITIDA" | "ACEPTADA" | "RECHAZADA" | "VENCIDA" | "CONVERTIDA" | "NO_COMPRADO";
  fechaVigencia?: string;
  id?: string;
  lineas?: Array<DetalleCotizacionResponseDTO>;
  observaciones?: string;
  sedeId?: string;
  sedeNombre?: string;
  total?: number;
  usuarioId?: string;
  usuarioNombre?: string;
  ventaId?: string;
}

export interface CrearDespachoRequestDTO {
  contactoNombre?: string;
  contactoTelefono?: string;
  direccionEntrega: string;
  fechaProgramada?: string;
  observaciones?: string;
  referenciaEntrega?: string;
  sedeId?: string;
  ventaId: string;
}

export interface CuentaFinancieraRequestDTO {
  entidad?: string;
  metodosPago?: Array<"EFECTIVO" | "YAPE" | "PLIN" | "SIP_AGORA" | "TARJETA" | "TRANSFERENCIA">;
  nombre: string;
  sedeId?: string;
  tipo: "EFECTIVO" | "BANCO" | "BILLETERA_DIGITAL";
  ultimosDigitos?: string;
}

export interface CuentaFinancieraResponseDTO {
  activa?: boolean;
  createdAt?: string;
  entidad?: string;
  id?: string;
  metodosPago?: Array<"EFECTIVO" | "YAPE" | "PLIN" | "SIP_AGORA" | "TARJETA" | "TRANSFERENCIA">;
  nombre?: string;
  sedeId?: string;
  sedeNombre?: string;
  tipo?: "EFECTIVO" | "BANCO" | "BILLETERA_DIGITAL";
  ultimoCorte?: string;
  ultimoSaldo?: number;
  ultimosDigitos?: string;
}

export interface CuentaPorPagarResponseDTO {
  compraId?: string;
  compraNumeroOC?: string;
  createdAt?: string;
  descripcion?: string;
  estado?: "PENDIENTE" | "PAGADO_PARCIAL" | "PAGADO";
  fechaPago?: string;
  fechaVencimiento?: string;
  id?: string;
  metodoPago?: string;
  montoPagado?: number;
  montoTotal?: number;
  pagadoPorId?: string;
  pagadoPorNombre?: string;
  proveedorDocumento?: string;
  proveedorId?: string;
  proveedorNombre?: string;
  saldoPendiente?: number;
  sedeId?: string;
  sedeNombre?: string;
  tipoReferencia?: "COMPRA" | "PLANILLA" | "SERVICIO" | "OTRO";
  vencida?: boolean;
}

export interface DashboardAsistenciaDTO {
  asistenciasRecientes?: Array<RrhhAsistenciaResponseDTO>;
  ausentesHoy?: number;
  presentesHoy?: number;
  tardanzasHoy?: number;
  totalEmpleados?: number;
}

export interface DespachoDashboardDTO {
  atrasados?: number;
  cargaPorRepartidor?: Array<CargaRepartidorDTO>;
  entregadosEnLaFecha?: number;
  fecha?: string;
  programadosPorEstado?: Record<string, unknown>;
  sedeId?: string;
  tasaEntregaPrimerIntento?: number;
}

export interface DetalleCompraCommandDTO {
  almacenId: string;
  cantidad: number;
  fraccionId?: string;
  precioUnitarioConIgv: number;
  productoId: string;
}

export interface DetalleCompraResponseDTO {
  almacenId?: string;
  almacenNombre?: string;
  cantidad?: number;
  cantidadRecibida?: number;
  factorFraccion?: number;
  fraccionId?: string;
  fraccionNombre?: string;
  id?: string;
  igvLinea?: number;
  precioUnitarioConIgv?: number;
  productoId?: string;
  productoNombre?: string;
  productoSku?: string;
  subtotal?: number;
  valorUnitarioNeto?: number;
}

export interface DetalleCotizacionCommandDTO {
  cantidad: number;
  fraccionId?: string;
  productoId?: string;
  servicioId?: string;
}

export interface DetalleCotizacionResponseDTO {
  cantidad?: number;
  fraccionId?: string;
  fraccionNombre?: string;
  id?: string;
  igvLinea?: number;
  porcentajeIgv?: number;
  precioUnitarioConIgv?: number;
  productoId?: string;
  productoNombre?: string;
  servicioId?: string;
  servicioNombre?: string;
  subtotal?: number;
  valorUnitarioNeto?: number;
}

export interface DetalleDevolucionRequestDTO {
  cantidad: number;
  detalleVentaId: string;
  estadoProducto: "BUENO" | "DAÑADO" | "USADO";
  observacion?: string;
}

export interface DetalleDevolucionResponseDTO {
  almacenNombre?: string;
  cantidadDevuelta?: number;
  cantidadStock?: number;
  detalleVentaId?: string;
  estadoProducto?: "BUENO" | "DAÑADO" | "USADO";
  fraccionNombre?: string;
  id?: string;
  observacion?: string;
  precioUnitarioRef?: number;
  productoId?: string;
  productoNombre?: string;
  subtotal?: number;
}

export interface DetallePreformaRequestDTO {
  cantidad: number;
  fraccionId?: string;
  productoId?: string;
  servicioId?: string;
}

export interface DetallePreformaResponseDTO {
  cantidad?: number;
  fraccionId?: string;
  fraccionNombre?: string;
  id?: string;
  igvLinea?: number;
  precioUnitarioConIgv?: number;
  productoId?: string;
  productoNombre?: string;
  servicioId?: string;
  servicioNombre?: string;
  subtotal?: number;
  valorUnitarioNeto?: number;
}

export interface DevolucionDashboardDTO {
  desde?: string;
  devolucionesPorEstado?: Record<string, unknown>;
  hasta?: string;
  montoAprobado?: number;
  montoReembolsado?: number;
  montoReembolsosPendientes?: number;
  reembolsosPendientes?: number;
  sedeId?: string;
  tasaDevolucionPorcentaje?: number;
  unidadesMerma?: number;
  unidadesReingresadas?: number;
  ventasDelPeriodo?: number;
}

export interface DevolucionListadoDTO {
  clienteNombre?: string;
  codigo?: string;
  estado?: "PENDIENTE" | "APROBADA" | "RECHAZADA";
  fechaSolicitud?: string;
  id?: string;
  montoTotal?: number;
  sedeNombre?: string;
  ventaId?: string;
  ventaSerie?: string;
}

export interface DevolucionRequestDTO {
  lineas: Array<DetalleDevolucionRequestDTO>;
  motivo: string;
  ventaId: string;
}

export interface DevolucionResponseDTO {
  clienteId?: string;
  clienteNombre?: string;
  codigo?: string;
  detalles?: Array<DetalleDevolucionResponseDTO>;
  estado?: "PENDIENTE" | "APROBADA" | "RECHAZADA";
  fechaResolucion?: string;
  fechaSolicitud?: string;
  id?: string;
  montoTotal?: number;
  motivo?: string;
  observacionAdmin?: string;
  reembolsoEstado?: "PENDIENTE" | "PROCESADO";
  reembolsoId?: string;
  resueltoPorNombre?: string;
  sedeId?: string;
  sedeNombre?: string;
  solicitadoPorNombre?: string;
  ventaId?: string;
  ventaSerie?: string;
}

export interface EmpresaRequestDTO {
  direccionFiscal: string;
  email?: string;
  logoUrl?: string;
  nombreComercial?: string;
  razonSocial: string;
  ruc: string;
  telefono?: string;
  ubigeo: string;
}

export interface EmpresaResponseDTO {
  actualizadoPor?: string;
  configurada?: boolean;
  direccionFiscal?: string;
  email?: string;
  logoUrl?: string;
  nombreComercial?: string;
  razonSocial?: string;
  ruc?: string;
  telefono?: string;
  ubigeo?: string;
  updatedAt?: string;
}

export interface EvolucionMensualResponseDTO {
  meses?: number;
  sedeId?: string;
  serie?: Array<MesEvolucionDTO>;
}

export interface FlujoCajaRealResponseDTO {
  advertencias?: Array<string>;
  consolidado?: FlujoCajaResumenDTO;
  desde?: string;
  hasta?: string;
  porSede?: Array<FlujoCajaSedeDTO>;
  sedeId?: string;
}

export interface FlujoCajaResumenDTO {
  cobrosPorMetodo?: Record<string, unknown>;
  cobrosVentas?: MontoPorMedioDTO;
  flujoNeto?: MontoPorMedioDTO;
  flujoOperativo?: MontoPorMedioDTO;
  otrosPagos?: MontoPorMedioDTO;
  pagosNominas?: MontoPorMedioDTO;
  pagosProveedores?: MontoPorMedioDTO;
  reembolsosClientes?: MontoPorMedioDTO;
  retirosSocios?: MontoPorMedioDTO;
  totalEgresosOperativos?: MontoPorMedioDTO;
}

export interface FlujoCajaSedeDTO {
  resumen?: FlujoCajaResumenDTO;
  sedeId?: string;
  sedeNombre?: string;
}

export interface FlujoDevengadoResponseDTO {
  consolidado?: UtilidadNetaResponseDTO;
  desde?: string;
  hasta?: string;
  porSede?: Array<UtilidadSedeDTO>;
  sedeId?: string;
}

export interface GeneracionNominaResponseDTO {
  advertencias?: Array<string>;
  generadas?: number;
  nominas?: Array<NominaListadoDTO>;
  omitidas?: number;
  periodo?: string;
  sedeId?: string;
  totalNetoGenerado?: number;
}

export interface GenerarNominaRequestDTO {
  periodo: string;
  sedeId: string;
}

export interface HistorialProductoDTO {
  almacenId?: string;
  codigoSku?: string;
  desde?: string;
  hasta?: string;
  movimientos?: Array<KardexMovimientoDetalleDTO>;
  pagina?: number;
  productoId?: string;
  productoNombre?: string;
  saldosPorAlmacen?: Array<SaldoKardexDTO>;
  stockActual?: number;
  tamanoPagina?: number;
  totalElementos?: number;
  totalEntradas?: number;
  totalPaginas?: number;
  totalSalidas?: number;
  unidad?: string;
}

export interface KardexMovimientoDetalleDTO {
  almacenId?: string;
  almacenNombre?: string;
  cantidadMovida?: number;
  codigoSku?: string;
  createdAt?: string;
  id?: string;
  observacion?: string;
  productoId?: string;
  productoNombre?: string;
  referenciaId?: string;
  sedeId?: string;
  sedeNombre?: string;
  stockAnterior?: number;
  stockResultante?: number;
  tipoMovimiento?: "ENTRADA" | "SALIDA";
  tipoReferencia?: "VENTA" | "COMPRA" | "AJUSTE" | "MERMA" | "TRANSFERENCIA" | "DEVOLUCION";
  unidad?: string;
  userId?: string;
  userNombre?: string;
  costoUnitario?: number;
  precioVenta?: number;
  margenUnitario?: number;
}

export interface KardexMovimientoRequestDTO {
  almacen?: Almacen;
  cantidadMovida?: number;
  observacion?: string;
  producto?: Producto;
  referenciaId?: string;
  stockResultante?: number;
  tipoMovimiento?: "ENTRADA" | "SALIDA";
  tipoReferencia?: "VENTA" | "COMPRA" | "AJUSTE" | "MERMA" | "TRANSFERENCIA" | "DEVOLUCION";
  user?: User;
}

export interface KardexMovimientoResponseDTO {
  almacen?: Almacen;
  cantidadMovida?: number;
  id?: string;
  observacion?: string;
  producto?: Producto;
  referenciaId?: string;
  stockResultante?: number;
  tipoMovimiento?: "ENTRADA" | "SALIDA";
  tipoReferencia?: "VENTA" | "COMPRA" | "AJUSTE" | "MERMA" | "TRANSFERENCIA" | "DEVOLUCION";
  user?: User;
}

export interface KpisFinancierosDTO {
  cantidadVentas?: number;
  conversionPreformasPorcentaje?: number;
  cuentasPorPagarVencidas?: number;
  flujoNeto?: number;
  flujoOperativo?: number;
  margenNetoPorcentaje?: number;
  montoPreformasPerdido?: number;
  retirosSobreUtilidadPorcentaje?: number;
  retirosSocios?: number;
  tasaDevolucionPorcentaje?: number;
  ticketPromedio?: number;
  utilidadNeta?: number;
  valorMermas?: number;
  variacionUtilidadPorcentaje?: number;
  variacionVentasPorcentaje?: number;
  ventasNetasReales?: number;
}

export interface KpisFinancierosResponseDTO {
  consolidado?: KpisFinancierosDTO;
  desde?: string;
  hasta?: string;
  porSede?: Array<KpisSedeDTO>;
  sedeId?: string;
}

export interface KpisSedeDTO {
  kpis?: KpisFinancierosDTO;
  sedeId?: string;
  sedeNombre?: string;
}

export interface LegajoDashboardDTO {
  activos?: number;
  activosPorSistemaPension?: Record<string, unknown>;
  activosPorTipoContrato?: Record<string, unknown>;
  activosSinContrato?: number;
  activosSinDni?: number;
  activosSinSistemaPension?: number;
  inactivos?: number;
  planillaMensualBase?: number;
  sedeId?: string;
  sueldoPromedio?: number;
  totalDocumentos?: number;
  totalLegajos?: number;
  usuariosActivosSinLegajo?: number;
}

export interface LegajoDocumentoResponseDTO {
  contentType?: string;
  createdAt?: string;
  descripcion?: string;
  id?: string;
  legajoId?: string;
  nombreOriginal?: string;
  subidoPorNombre?: string;
  tamanoBytes?: number;
  tipo?: "CONTRATO" | "DNI" | "CV" | "CERTIFICADO_ESTUDIOS" | "CERTIFICADO_TRABAJO" | "ANTECEDENTES_POLICIALES" | "OTRO";
}

export interface LegajoListadoDTO {
  activo?: boolean;
  empleadoId?: string;
  empleadoNombre?: string;
  fechaIngreso?: string;
  id?: string;
  sedeNombre?: string;
  sistemaPension?: "ONP" | "AFP" | "NINGUNO";
  sueldoBase?: number;
  tipoContrato?: string;
}

export interface LegajoRequestDTO {
  activo?: boolean;
  diaCobro?: number;
  empleadoId: string;
  fechaIngreso: string;
  porcentajeComision?: number;
  sistemaPension: "ONP" | "AFP" | "NINGUNO";
  sueldoBase: number;
  tasaPension?: number;
  tipoContrato: string;
}

export interface LegajoResponseDTO {
  activo?: boolean;
  createdAt?: string;
  diaCobro?: number;
  documentos?: Array<LegajoDocumentoResponseDTO>;
  empleadoEmail?: string;
  empleadoId?: string;
  empleadoNombre?: string;
  fechaIngreso?: string;
  id?: string;
  porcentajeComision?: number;
  sedeId?: string;
  sedeNombre?: string;
  sistemaPension?: "ONP" | "AFP" | "NINGUNO";
  sueldoBase?: number;
  tasaPension?: number;
  tipoContrato?: string;
}

export interface LineaVentaCommandDTO {
  cantidad: number;
  fraccionId?: string;
  productoId?: string;
  servicioId?: string;
}

export interface LineaVentaResponseDTO {
  cantidad?: number;
  fraccionId?: string;
  fraccionNombre?: string;
  id?: string;
  precioUnitarioConIgv?: number;
  productoId?: string;
  productoNombre?: string;
  servicioId?: string;
  servicioNombre?: string;
  subtotal?: number;
}

export interface LogAuditoriaResponseDTO {
  accion?: "INSERT" | "UPDATE" | "DELETE" | "LOGIN" | "LOGOUT" | "ACCESS";
  createdAt?: string;
  datosJson?: Record<string, unknown>;
  id?: string;
  registroId?: string;
  tabla?: string;
  userAgent?: string;
  userEmail?: string;
  userId?: string;
  userNombre?: string;
}

export interface LoginRequestDTO {
  email: string;
  password: string;
}

export interface LoginResponseDTO {
  email?: string;
  nombre?: string;
  rol?: string;
  token?: string;
  totpConfigurado?: boolean;
  usuarioId?: string;
}

export interface MermaKardexDTO {
  almacenId?: string;
  almacenNombre?: string;
  cantidad?: number;
  codigoSku?: string;
  costoUnitario?: number;
  fecha?: string;
  movimientoId?: string;
  observacion?: string;
  productoId?: string;
  productoNombre?: string;
  referenciaId?: string;
  unidad?: string;
  userNombre?: string;
  valor?: number;
}

export interface MermaPorProductoDTO {
  cantidad?: number;
  codigoSku?: string;
  movimientos?: number;
  productoId?: string;
  productoNombre?: string;
  unidad?: string;
  valor?: number;
}

export interface MesEvolucionDTO {
  cobros?: number;
  comprasNetas?: number;
  devolucionesNetas?: number;
  egresosOperativos?: number;
  flujoNeto?: number;
  flujoOperativo?: number;
  margenNetoPorcentaje?: number;
  nominas?: number;
  parcial?: boolean;
  periodo?: string;
  retirosSocios?: number;
  utilidadNeta?: number;
  ventasNetasReales?: number;
}

export interface MesProyectadoDTO {
  flujoNeto?: number;
  flujoOperativo?: number;
  fraccionDelMes?: number;
  ingresosEstimados?: number;
  otrosPagos?: MontoProyectadoDTO;
  pagosNominas?: MontoProyectadoDTO;
  pagosProveedores?: MontoProyectadoDTO;
  parcial?: boolean;
  periodo?: string;
  reembolsosClientes?: MontoProyectadoDTO;
  retirosEstimados?: number;
  saldoFinalEstimado?: number;
  totalEgresos?: number;
}

export interface MetodoPagoMontoDTO {
  metodo?: "EFECTIVO" | "YAPE" | "PLIN" | "SIP_AGORA" | "TARJETA" | "TRANSFERENCIA";
  pagos?: number;
  porcentaje?: number;
  total?: number;
}

export interface MontoPorMedioDTO {
  digital?: number;
  efectivo?: number;
  total?: number;
}

export interface MontoProyectadoDTO {
  comprometido?: number;
  estimado?: number;
  tendencia?: number;
}

export interface MovilidadDashboardDTO {
  activos?: number;
  activosPorTipo?: Record<string, unknown>;
  activosSinConductor?: number;
  enRuta?: number;
  entregasHoy?: number;
  inactivos?: number;
  sedeId?: string;
  total?: number;
}

export interface MovilidadListadoDTO {
  activo?: boolean;
  conductorNombre?: string;
  id?: string;
  marca?: string;
  modelo?: string;
  placa?: string;
  sedeNombre?: string;
  tipo?: string;
}

export interface MovilidadRequestDTO {
  activo?: boolean;
  marca?: string;
  modelo?: string;
  placa: string;
  sedeId: string;
  tipo: "MOTO" | "AUTO" | "CAMIONETA" | "CAMION";
}

export interface MovilidadResponseDTO {
  activo?: boolean;
  conductorId?: string;
  conductorNombre?: string;
  createdAt?: string;
  id?: string;
  marca?: string;
  modelo?: string;
  ordenesActivas?: number;
  placa?: string;
  sedeId?: string;
  sedeNombre?: string;
  tipo?: string;
}

export interface NominaDashboardDTO {
  cantidadAprobadas?: number;
  cantidadPagadas?: number;
  cantidadPendientes?: number;
  periodo?: string;
  sedeId?: string;
  totalComisiones?: number;
  totalDescuentos?: number;
  totalNeto?: number;
  totalPagado?: number;
  totalPorPagar?: number;
  totalSueldos?: number;
}

export interface NominaListadoDTO {
  descuentos?: number;
  empleadoNombre?: string;
  estado?: "PENDIENTE" | "APROBADA" | "PAGADA";
  id?: string;
  periodo?: string;
  sedeNombre?: string;
  sueldoBaseCalculado?: number;
  totalComisiones?: number;
  totalNeto?: number;
  userId?: string;
}

export interface NominaResponseDTO {
  aprobadoPorNombre?: string;
  comisionId?: string;
  createdAt?: string;
  descuentoPension?: number;
  descuentos?: number;
  diasComputados?: number;
  empleadoNombre?: string;
  estado?: "PENDIENTE" | "APROBADA" | "PAGADA";
  fechaAprobacion?: string;
  fechaPago?: string;
  generadoPorNombre?: string;
  id?: string;
  metodoPago?: "EFECTIVO" | "YAPE" | "PLIN" | "SIP_AGORA" | "TARJETA" | "TRANSFERENCIA";
  observaciones?: string;
  otrosDescuentos?: number;
  pagadoPorNombre?: string;
  periodo?: string;
  porcentajeComision?: number;
  referenciaPago?: string;
  sedeId?: string;
  sedeNombre?: string;
  sistemaPension?: "ONP" | "AFP" | "NINGUNO";
  sueldoBaseCalculado?: number;
  tasaPension?: number;
  tipoContrato?: string;
  totalComisiones?: number;
  totalNeto?: number;
  userId?: string;
  ventasBaseComision?: number;
}

export interface OrdenDespachoResponseDTO {
  atrasado?: boolean;
  clienteNombre?: string;
  codigoSeguimiento?: string;
  contactoNombre?: string;
  contactoTelefono?: string;
  createdAt?: string;
  direccionEntrega?: string;
  estado?: "PENDIENTE" | "EN_CAMINO" | "ENTREGADO" | "FALLIDO";
  fechaAsignacion?: string;
  fechaEntrega?: string;
  fechaProgramada?: string;
  fechaSalida?: string;
  id?: string;
  intentosFallidos?: number;
  latitudEntrega?: number;
  longitudEntrega?: number;
  motivoFallo?: string;
  movilidadId?: string;
  movilidadPlaca?: string;
  observaciones?: string;
  recibidoPorDocumento?: string;
  recibidoPorNombre?: string;
  referenciaEntrega?: string;
  responsableId?: string;
  responsableNombre?: string;
  sedeId?: string;
  sedeNombre?: string;
  tieneEvidencia?: boolean;
  ventaId?: string;
  ventaSerie?: string;
}

export interface PagarNominaRequestDTO {
  metodoPago: "EFECTIVO" | "YAPE" | "PLIN" | "SIP_AGORA" | "TARJETA" | "TRANSFERENCIA";
  referencia?: string;
}

export interface PageAjusteStockResponseDTO {
  content?: Array<AjusteStockResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PerfilActualizarRequestDTO {
  apellido: string;
  biografia?: string;
  celular: string;
  diaCobro?: number;
  documento?: string;
  email: string;
  foto?: string;
  nombre: string;
  sedeId?: string;
  sueldoBase?: number;
}

export interface PerfilContrasenaRequestDTO {
  actual: string;
  confirmacion: string;
  nueva: string;
}

export interface PerfilResponseDTO {
  activo?: boolean;
  apellido?: string;
  biografia?: string;
  celular?: string;
  diaCobro?: number;
  documento?: string;
  email?: string;
  fechaIngreso?: string;
  foto?: string;
  id?: string;
  legajoId?: string;
  nombre?: string;
  nombreUsuario?: string;
  puedeEditarLaboral?: boolean;
  rol?: string;
  sedeId?: string;
  sedeNombre?: string;
  sistemaPension?: string;
  sueldoBase?: number;
  tipoContrato?: string;
  refreshToken?: string;
  token?: string;
  totpEnabled?: boolean;
  totpSecreto?: string;
  totpUrl?: string;
  ultimoIp?: string;
  ultimoLogin?: string;
}

export interface PerfilTotpRequestDTO {
  activo: boolean;
}

export interface PageAlmacenResponseDTO {
  content?: Array<AlmacenResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageCatalogoFraccionResponseDTO {
  content?: Array<CatalogoFraccionResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageCategoriaResponseDTO {
  content?: Array<CategoriaResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageClienteResponseDTO {
  content?: Array<ClienteResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageComisionResponseDTO {
  content?: Array<ComisionResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageCompraListadoDTO {
  content?: Array<CompraListadoDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageCotizacionListadoDTO {
  content?: Array<CotizacionListadoDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageCuentaPorPagarResponseDTO {
  content?: Array<CuentaPorPagarResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageDevolucionListadoDTO {
  content?: Array<DevolucionListadoDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageKardexMovimientoDetalleDTO {
  content?: Array<KardexMovimientoDetalleDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageKardexMovimientoResponseDTO {
  content?: Array<KardexMovimientoResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageLegajoListadoDTO {
  content?: Array<LegajoListadoDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageLogAuditoriaResponseDTO {
  content?: Array<LogAuditoriaResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageMovilidadListadoDTO {
  content?: Array<MovilidadListadoDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageNominaListadoDTO {
  content?: Array<NominaListadoDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageOrdenDespachoResponseDTO {
  content?: Array<OrdenDespachoResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PagePreformaListadoDTO {
  content?: Array<PreformaListadoDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageProductoResponseDTO {
  content?: Array<ProductoResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageProveedorResponseDTO {
  content?: Array<ProveedorResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageReembolsoResponseDTO {
  content?: Array<ReembolsoResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageRetiroSocioListadoDTO {
  content?: Array<RetiroSocioListadoDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageRrhhAsistenciaResponseDTO {
  content?: Array<RrhhAsistenciaResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageSaldoCuentaResponseDTO {
  content?: Array<SaldoCuentaResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageSaldoKardexDTO {
  content?: Array<SaldoKardexDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageSedeResponseDTO {
  content?: Array<SedeResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageServicioResponseDTO {
  content?: Array<ServicioResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageStockConsolidadoDTO {
  content?: Array<StockConsolidadoDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageStockResponseDTO {
  content?: Array<StockResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageTransferenciaStockResponseDTO {
  content?: Array<TransferenciaStockResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageTurnoTrabajoResponseDTO {
  content?: Array<TurnoTrabajoResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageVentaListadoResponseDTO {
  content?: Array<VentaListadoResponseDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageVentaReporteDTO {
  content?: Array<VentaReporteDTO>;
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  number?: number;
  numberOfElements?: number;
  pageable?: PageableObject;
  size?: number;
  sort?: SortObject;
  totalElements?: number;
  totalPages?: number;
}

export interface PageableObject {
  offset?: number;
  pageNumber?: number;
  pageSize?: number;
  paged?: boolean;
  sort?: SortObject;
  unpaged?: boolean;
}

export interface PagoCxPRequestDTO {
  metodoPago: "EFECTIVO" | "YAPE" | "PLIN" | "SIP_AGORA" | "TARJETA" | "TRANSFERENCIA";
  monto: number;
  observaciones?: string;
  referencia?: string;
}

export interface PagoVentaCommandDTO {
  metodoPago: "EFECTIVO" | "YAPE" | "PLIN" | "SIP_AGORA" | "TARJETA" | "TRANSFERENCIA";
  monto: number;
  referencia?: string;
}

export interface PagoVentaDetalleResponseDTO {
  id?: string;
  metodoPago?: "EFECTIVO" | "YAPE" | "PLIN" | "SIP_AGORA" | "TARJETA" | "TRANSFERENCIA";
  monto?: number;
  referencia?: string;
}

export interface ParametrosFinanzasDTO {
  mesesHistoricosProyeccion?: number;
  mesesProyeccionDefault?: number;
  mesesProyeccionMax?: number;
}

export interface ParametrosFinanzasRequestDTO {
  mesesHistoricosProyeccion: number;
  mesesProyeccionDefault: number;
  mesesProyeccionMax: number;
}

export interface ParametrosInventarioDTO {
  minCaracteresJustificacion?: number;
  umbralJustificacionAjuste?: number;
}

export interface ParametrosInventarioRequestDTO {
  minCaracteresJustificacion: number;
  umbralJustificacionAjuste: number;
}

export interface ParametrosReportesDTO {
  maxFilasExportacion?: number;
}

export interface ParametrosReportesRequestDTO {
  maxFilasExportacion: number;
}

export interface ParametrosRequestDTO {
  finanzas?: ParametrosFinanzasRequestDTO;
  inventario?: ParametrosInventarioRequestDTO;
  reportes?: ParametrosReportesRequestDTO;
  ventas?: ParametrosVentasRequestDTO;
}

export interface ParametrosResponseDTO {
  finanzas?: ParametrosFinanzasDTO;
  inventario?: ParametrosInventarioDTO;
  reportes?: ParametrosReportesDTO;
  ventas?: ParametrosVentasDTO;
}

export interface ParametrosVentasDTO {
  preformaMinutosDefault?: number;
  preformaMinutosMax?: number;
  preformaMinutosMin?: number;
  vigenciaCotizacionDias?: number;
}

export interface ParametrosVentasRequestDTO {
  preformaMinutosDefault: number;
  preformaMinutosMax: number;
  preformaMinutosMin: number;
  vigenciaCotizacionDias: number;
}

export interface PatrimonioResponseDTO {
  advertencias?: Array<string>;
  calculadoEn?: string;
  cuentas?: Array<SaldoCuentaEstimadoDTO>;
  cuentasPorPagar?: number;
  cuentasPorPagarVencidas?: number;
  disponible?: number;
  inventarioValorizado?: number;
  nominasPorPagar?: number;
  patrimonioNeto?: number;
  porSede?: Array<PatrimonioSedeDTO>;
  productosSinCosto?: number;
  reembolsosPorPagar?: number;
  sedeId?: string;
  separacion?: SeparacionPatrimonialDTO;
  totalActivos?: number;
  totalPasivos?: number;
}

export interface PatrimonioSedeDTO {
  disponible?: number;
  inventarioValorizado?: number;
  patrimonioNeto?: number;
  sedeId?: string;
  sedeNombre?: string;
  totalActivos?: number;
  totalPasivos?: number;
}

export interface PeriodoVentasDTO {
  cantidad?: number;
  desde?: string;
  etiqueta?: string;
  hasta?: string;
  igv?: number;
  neto?: number;
  ticketPromedio?: number;
  total?: number;
}

export interface PreformaDashboardDTO {
  abiertasAhora?: number;
  desde?: string;
  hasta?: string;
  montoAbiertoAhora?: number;
  montoConfirmado?: number;
  montoPerdido?: number;
  preformasPorEstado?: Record<string, unknown>;
  sedeId?: string;
  tasaConversionPorcentaje?: number;
}

export interface PreformaListadoDTO {
  clienteNombre?: string;
  codigo?: string;
  createdAt?: string;
  estado?: "ABIERTA" | "CONFIRMADA" | "CANCELADA" | "EXPIRADA";
  fechaExpiracion?: string;
  id?: string;
  totalEstimado?: number;
  vendedorNombre?: string;
}

export interface PreformaVentaRequestDTO {
  clienteId?: string;
  lineas: Array<DetallePreformaRequestDTO>;
  minutosVigencia?: number;
  observaciones?: string;
  sedeId: string;
}

export interface PreformaVentaResponseDTO {
  canceladoPorNombre?: string;
  clienteId?: string;
  clienteNombre?: string;
  codigo?: string;
  confirmadoPorNombre?: string;
  createdAt?: string;
  estado?: "ABIERTA" | "CONFIRMADA" | "CANCELADA" | "EXPIRADA";
  fechaCancelacion?: string;
  fechaConfirmacion?: string;
  fechaExpiracion?: string;
  id?: string;
  lineas?: Array<DetallePreformaResponseDTO>;
  minutosRestantes?: number;
  motivoCancelacion?: string;
  observaciones?: string;
  sedeId?: string;
  sedeNombre?: string;
  subtotalNetoEstimado?: number;
  totalEstimado?: number;
  totalIgvEstimado?: number;
  vendedorId?: string;
  vendedorNombre?: string;
  ventaId?: string;
  ventaSerie?: string;
}

export interface ProcesarReembolsoRequestDTO {
  observaciones?: string;
  referencia?: string;
}

export interface Producto {
  activo?: boolean;
  categoria?: Categoria;
  codigoSku?: string;
  createdAt?: string;
  descripcion?: string;
  esFraccionable?: boolean;
  fracciones?: Array<CatalogoFraccion>;
  id?: string;
  incluyeIgv?: boolean;
  nombre?: string;
  precioCompra?: number;
  precioVentaBase?: number;
  proveedor?: Proveedor;
  tipo?: "FISICO" | "SERVICIO";
  tipoAfectacion?: string;
  unidad?: string;
}

export interface ProductoRequestDTO {
  activo: boolean;
  categoriaId?: string;
  codigoSku?: string;
  descripcion?: string;
  esFraccionable: boolean;
  fracciones?: Array<CatalogoFraccionRequestDTO>;
  incluyeIgv?: boolean;
  nombre: string;
  precioCompra: number;
  precioVentaBase: number;
  proveedorId?: string;
  tipo: "FISICO" | "SERVICIO";
  tipoAfectacion?: string;
  unidad?: string;
  imagen?: string;
}

export interface ProductoResponseDTO {
  activo?: boolean;
  categoria?: CategoriaSummaryDTO;
  codigoSku?: string;
  createdAt?: string;
  descripcion?: string;
  esFraccionable?: boolean;
  fracciones?: Array<CatalogoFraccionResponseDTO>;
  id?: string;
  incluyeIgv?: boolean;
  nombre?: string;
  precioCompra?: number;
  precioVentaBase?: number;
  proveedor?: ProveedorSummaryDTO;
  tipo?: "FISICO" | "SERVICIO";
  tipoAfectacion?: string;
  unidad?: string;
  imagen?: string;
}

export interface ProductosEstrellaDTO {
  criterio?: "CANTIDAD" | "INGRESO" | "MARGEN";
  desde?: string;
  hasta?: string;
  items?: Array<RentabilidadItemDTO>;
  sedeId?: string;
  top?: number;
}

export interface Proveedor {
  activo?: boolean;
  banco?: string;
  celular?: string;
  condicionesPago?: string;
  createdAt?: string;
  cuentaBancaria?: string;
  diasCredito?: number;
  direccion?: string;
  email?: string;
  id?: string;
  nombreRazon?: string;
  numeroDocumento?: string;
  tipoDocumento?: string;
}

export interface ProveedorRankingDTO {
  anuladas?: number;
  compras?: number;
  diasEntregaMaximo?: number;
  diasEntregaPromedio?: number;
  montoRecibido?: number;
  nombre?: string;
  numeroDocumento?: string;
  pendientes?: number;
  posicion?: number;
  proveedorId?: string;
  recibidas?: number;
  ticketPromedio?: number;
  ultimaCompra?: string;
}

export interface ProveedorRequestDTO {
  activo: boolean;
  banco?: string;
  celular?: string;
  condicionesPago?: string;
  cuentaBancaria?: string;
  diasCredito?: number;
  direccion?: string;
  email?: string;
  nombreRazon: string;
  numeroDocumento: string;
  tipoDocumento: string;
}

export interface ProveedorResponseDTO {
  activo?: boolean;
  banco?: string;
  celular?: string;
  condicionesPago?: string;
  cuentaBancaria?: string;
  diasCredito?: number;
  direccion?: string;
  email?: string;
  id?: string;
  nombreRazon?: string;
  numeroDocumento?: string;
  tipoDocumento?: string;
}

export interface ProveedorSummaryDTO {
  id?: string;
  razonSocial?: string;
}

export interface ProveedoresRankingDTO {
  advertencias?: Array<string>;
  comprasOtraMoneda?: number;
  criterio?: "VOLUMEN" | "FRECUENCIA" | "ENTREGA";
  desde?: string;
  hasta?: string;
  items?: Array<ProveedorRankingDTO>;
  montosVisibles?: boolean;
  sedeId?: string;
  top?: number;
}

export interface ProyeccionFlujoResponseDTO {
  advertencias?: Array<string>;
  fechaBase?: string;
  ingresosPotenciales?: number;
  meses?: Array<MesProyectadoDTO>;
  mesesHistoricosUsados?: number;
  promedioEgresos?: number;
  promedioIngresos?: number;
  promedioRetiros?: number;
  saldoInicial?: number;
  sedeId?: string;
}

export interface QrPublicoDTO {
  empleadoNombre?: string;
  expiraEnEpoch?: number;
  payload?: string;
  segundosRestantes?: number;
}

export interface RecepcionCompraRequestDTO {
  numeroFacturaProveedor?: string;
  observaciones?: string;
}

export interface RechazarCotizacionRequestDTO {
  motivo: string;
}

export interface RechazarDevolucionRequestDTO {
  motivo: string;
}

export interface ReembolsoRequestDTO {
  devolucionId: string;
  metodoReembolso: "EFECTIVO" | "YAPE" | "PLIN" | "TARJETA" | "TRANSFERENCIA";
  monto?: number;
  motivoAjuste?: string;
  observaciones?: string;
  referencia?: string;
}

export interface ReembolsoResponseDTO {
  clienteNombre?: string;
  creadoPorNombre?: string;
  createdAt?: string;
  devolucionCodigo?: string;
  devolucionId?: string;
  estado?: "PENDIENTE" | "PROCESADO";
  fechaProcesado?: string;
  id?: string;
  metodoReembolso?: "EFECTIVO" | "YAPE" | "PLIN" | "TARJETA" | "TRANSFERENCIA";
  montoMaximo?: number;
  montoTotalReembolso?: number;
  motivoAjuste?: string;
  observaciones?: string;
  procesadoPorNombre?: string;
  referencia?: string;
  turnoCajaId?: string;
  ventaId?: string;
}

export interface RegistrarSaldoCuentaRequestDTO {
  fechaCorte?: string;
  observacion?: string;
  saldo: number;
}

export interface RegistrarVentaRequestDTO {
  almacenId: string;
  canalVenta?: "LOCAL" | "ONLINE";
  clienteId?: string;
  cotizacionId?: string;
  lineas: Array<LineaVentaCommandDTO>;
  pagos: Array<PagoVentaCommandDTO>;
  preformaId?: string;
  referenciaPedidoOnline?: string;
  sedeId: string;
  tipoComprobante: "BOLETA" | "FACTURA" | "TICKET";
  turnoCajaId: string;
}

export interface RentabilidadItemDTO {
  cantidadDevuelta?: number;
  cantidadNeta?: number;
  cantidadVendida?: number;
  categoria?: string;
  costoEstimado?: number;
  costoUnitario?: number;
  devolucionesNeto?: number;
  id?: string;
  ingresoNeto?: number;
  ingresoNetoReal?: number;
  margen?: number;
  margenPorcentaje?: number;
  nombre?: string;
  participacionPorcentaje?: number;
  sku?: string;
  tipo?: string;
  unidad?: string;
  ventas?: number;
}

export interface RentabilidadReporteDTO {
  advertencias?: Array<string>;
  categoriaId?: string;
  costoEstimado?: number;
  costosVisibles?: boolean;
  desde?: string;
  hasta?: string;
  ingresoNetoReal?: number;
  items?: Array<RentabilidadItemDTO>;
  margen?: number;
  margenPorcentaje?: number;
  sedeId?: string;
}

export interface ReporteMermasDTO {
  almacenId?: string;
  desde?: string;
  detalle?: Array<MermaKardexDTO>;
  hasta?: string;
  movimientos?: number;
  pagina?: number;
  porProducto?: Array<MermaPorProductoDTO>;
  productoId?: string;
  tamanoPagina?: number;
  totalElementos?: number;
  totalPaginas?: number;
  valorTotal?: number;
}

export interface ReporteUsoMovilidadDTO {
  desde?: string;
  despachosSinMovilidad?: number;
  hasta?: string;
  sedeId?: string;
  totalEntregados?: number;
  totalFallidos?: number;
  vehiculos?: Array<UsoMovilidadDTO>;
}

export interface ResumenArqueosDTO {
  abiertos?: number;
  cerrados?: number;
  conFaltante?: number;
  conSobrante?: number;
  cuadrados?: number;
  diferenciaNeta?: number;
  efectivoVentasAnuladas?: number;
  totalFaltante?: number;
  totalSobrante?: number;
  turnos?: number;
}

export interface ResumenCierreCajaDTO {
  declarado?: number;
  diferencia?: number;
  esperado?: number;
  montoInicial?: number;
  reembolsosEfectivo?: number;
  retirosSocios?: number;
  turnoId?: string;
  ventasEfectivo?: number;
}

export interface ResumenCxPDashboardDTO {
  montoTotalPagado?: number;
  montoTotalPorPagar?: number;
  montoTotalVencido?: number;
  pagadas?: number;
  pagadasParcial?: number;
  pendientes?: number;
  totalCuentas?: number;
  vencidas?: number;
}

export interface RetiroPorAdminDTO {
  adminId?: string;
  adminNombre?: string;
  cantidad?: number;
  total?: number;
}

export interface RetiroSocioDashboardDTO {
  cantidadAnulados?: number;
  cantidadRetiros?: number;
  desde?: string;
  hasta?: string;
  porAdmin?: Array<RetiroPorAdminDTO>;
  sedeId?: string;
  totalCajaFisica?: number;
  totalCuentaBancaria?: number;
  totalRetirado?: number;
}

export interface RetiroSocioListadoDTO {
  adminNombre?: string;
  estado?: "REGISTRADO" | "ANULADO";
  fechaRetiro?: string;
  id?: string;
  montoRetirado?: number;
  origenFondos?: "CAJA_FISICA" | "CUENTA_BANCARIA";
  sedeNombre?: string;
}

export interface RetiroSocioRequestDTO {
  descripcion?: string;
  montoRetirado: number;
  origenFondos: "CAJA_FISICA" | "CUENTA_BANCARIA";
  sedeId?: string;
}

export interface RetiroSocioResponseDTO {
  adminId?: string;
  adminNombre?: string;
  anuladoPorId?: string;
  anuladoPorNombre?: string;
  createdAt?: string;
  descripcion?: string;
  estado?: "REGISTRADO" | "ANULADO";
  fechaAnulacion?: string;
  fechaRetiro?: string;
  id?: string;
  impactoCajaRevertido?: boolean;
  montoRetirado?: number;
  motivoAnulacion?: string;
  origenFondos?: "CAJA_FISICA" | "CUENTA_BANCARIA";
  sedeId?: string;
  sedeNombre?: string;
  turnoCajaId?: string;
}

export interface RetiroSocioResumenDTO {
  cantidad?: number;
  socioId?: string;
  socioNombre?: string;
  total?: number;
}

export interface Rol {
  createdAt?: string;
  id?: string;
  nombre?: "ROLE_ADMIN" | "ROLE_MULTIFUNCIONAL";
  permisosJson?: Record<string, unknown>;
}

export interface RolRequestDTO {
  nombre: "ROLE_ADMIN" | "ROLE_MULTIFUNCIONAL";
  permisosJson: Record<string, unknown>;
}

export interface RolResponseDTO {
  id?: string;
  nombre?: string;
  permisosJson?: Record<string, unknown>;
}

export interface RrhhAsistenciaRequestDTO {
  dispositivoInfo?: string;
  empleadoId?: string;
  fechaHora?: string;
  tipoMarcacion?: "ENTRADA" | "SALIDA" | "INICIO_RECESO" | "FIN_RECESO";
  turnoCajaId?: string;
}

export interface RrhhAsistenciaResponseDTO {
  dispositivoInfo?: string;
  empleadoId?: string;
  empleadoNombreCompleto?: string;
  fechaHora?: string;
  id?: string;
  tipoMarcacion?: "ENTRADA" | "SALIDA" | "INICIO_RECESO" | "FIN_RECESO";
  turnoCajaId?: string;
}

export interface SaldoCuentaEstimadoDTO {
  cuentaId?: string;
  diasDesdeCorte?: number;
  fechaCorte?: string;
  metodosPago?: Array<"EFECTIVO" | "YAPE" | "PLIN" | "SIP_AGORA" | "TARJETA" | "TRANSFERENCIA">;
  movimientosPosteriores?: number;
  nombre?: string;
  saldoCorte?: number;
  saldoEstimado?: number;
  sedeId?: string;
  sedeNombre?: string;
  tipo?: "EFECTIVO" | "BANCO" | "BILLETERA_DIGITAL";
}

export interface SaldoCuentaResponseDTO {
  createdAt?: string;
  cuentaId?: string;
  fechaCorte?: string;
  id?: string;
  observacion?: string;
  registradoPorNombre?: string;
  saldo?: number;
}

export interface SaldoKardexDTO {
  almacenId?: string;
  almacenNombre?: string;
  bajoMinimo?: boolean;
  cantidad?: number;
  codigoSku?: string;
  productoId?: string;
  productoNombre?: string;
  sedeId?: string;
  sedeNombre?: string;
  stockMinimo?: number;
  ultimoMovimiento?: string;
  unidad?: string;
}

export interface SaldosProductoDTO {
  almacenes?: Array<SaldoKardexDTO>;
  codigoSku?: string;
  productoId?: string;
  productoNombre?: string;
  stockTotal?: number;
  unidad?: string;
}

export interface Sede {
  activa?: boolean;
  celularSede?: string;
  ciudad?: string;
  createdAt?: string;
  direccion?: string;
  id?: string;
  moneda?: string;
  nombre?: string;
  pais?: string;
}

export interface SedeRequestDTO {
  activa?: boolean;
  celularSede?: string;
  ciudad?: string;
  direccion?: string;
  imagen?: string;
  latitud?: number;
  longitud?: number;
  moneda?: string;
  nombre: string;
  pais?: string;
}

export interface SedeResponseDTO {
  activa?: boolean;
  celularSede?: string;
  ciudad?: string;
  createdAt?: string;
  direccion?: string;
  id?: string;
  imagen?: string;
  latitud?: number;
  longitud?: number;
  moneda?: string;
  nombre?: string;
  pais?: string;
}

export interface SeparacionPatrimonialDTO {
  anio?: number;
  retirosPorSocio?: Array<RetiroSocioResumenDTO>;
  retirosSociosAnio?: number;
  retirosSociosMesActual?: number;
  utilidadNetaAnio?: number;
  utilidadRetenidaAnio?: number;
}

export interface SerieComprobanteRequestDTO {
  activa?: boolean;
  sedeId: string;
  serie: string;
  tipoComprobante: "BOLETA" | "FACTURA" | "TICKET";
}

export interface SerieComprobanteResponseDTO {
  activa?: boolean;
  id?: string;
  sedeId?: string;
  sedeNombre?: string;
  serie?: string;
  tipoComprobante?: "BOLETA" | "FACTURA" | "TICKET";
  updatedAt?: string;
}

export interface SeriesRequestDTO {
  series: Array<SerieComprobanteRequestDTO>;
}

export interface ServicioColaboradorRequestDTO {
  participacion?: 'INSTALO' | 'AYUDO' | string;
  userId: string;
}

export interface ServicioColaboradorResponseDTO {
  apellido?: string;
  email?: string;
  nombre?: string;
  participacion?: string;
  userId?: string;
}

export interface ServicioRequestDTO {
  activo: boolean;
  colaboradores?: Array<ServicioColaboradorRequestDTO>;
  descripcion?: string;
  nombre: string;
  precio: number;
}

export interface ServicioResponseDTO {
  activo?: boolean;
  colaboradores?: Array<ServicioColaboradorResponseDTO>;
  createdAt?: string;
  descripcion?: string;
  id?: string;
  nombre?: string;
  precio?: number;
}

export interface SortObject {
  empty?: boolean;
  sorted?: boolean;
  unsorted?: boolean;
}

export interface StockConsolidadoDTO {
  almacenes?: number;
  almacenesBajoMinimo?: number;
  codigoSku?: string;
  productoId?: string;
  productoNombre?: string;
  stockMinimoTotal?: number;
  stockTotal?: number;
  unidad?: string;
}

export interface StockDashboardDTO {
  agotados?: number;
  ajustesHoy?: number;
  alertasBajoMinimo?: number;
  entradasHoy?: number;
  fecha?: string;
  movimientosHoy?: number;
  primerasAlertas?: Array<SaldoKardexDTO>;
  productosConStock?: number;
  salidasHoy?: number;
  sedeId?: string;
  transferenciasHoy?: number;
  valorAjustesHoy?: number;
  valorInventario?: number;
}

export interface StockRequestDTO {
  almacen: Almacen;
  cantidad: number;
  producto: Producto;
  stockMinimo?: number;
  updatedAt?: string;
}

export interface StockResponseDTO {
  almacen?: Almacen;
  cantidad?: number;
  producto?: Producto;
  stockMinimo?: number;
  updatedAt?: string;
}

export interface TotpConfigDTO {
  empleadoId?: string;
  mensaje?: string;
  qrCodeUrl?: string;
  secret?: string;
}

export interface TransferenciaRequestDTO {
  almacenDestinoId: string;
  almacenOrigenId: string;
  cantidad: number;
  motivo?: string;
  productoId: string;
  referenciaDocumento?: string;
}

export interface TransferenciaStockRequestDTO {
  almacenDestinoId: string;
  almacenOrigenId: string;
  cantidad: number;
  motivo?: string;
  productoId: string;
  referenciaDocumento?: string;
}

export interface TransferenciaStockResponseDTO {
  cantidad?: number;
  codigo?: string;
  codigoSku?: string;
  createdAt?: string;
  destino?: AlmacenResumenDTO;
  entrada?: KardexMovimientoDetalleDTO;
  entreSedes?: boolean;
  id?: string;
  motivo?: string;
  origen?: AlmacenResumenDTO;
  productoId?: string;
  productoNombre?: string;
  referenciaDocumento?: string;
  salida?: KardexMovimientoDetalleDTO;
  unidad?: string;
  userId?: string;
  userNombre?: string;
}

export interface TurnoCajaResponseDTO {
  estado?: "ABIERTO" | "CERRADO";
  fechaApertura?: string;
  fechaCierre?: string;
  id?: string;
  montoFinalDeclarado?: number;
  montoInicial?: number;
  observaciones?: string;
  sede?: Sede;
  user?: User;
}

export interface TurnoDashboardDTO {
  coberturaPorDia?: Record<string, unknown>;
  empleadosConTurno?: number;
  empleadosSinTurno?: number;
  empleadosSobre48Horas?: Array<string>;
  horasSemanalesProgramadas?: number;
  sedeId?: string;
  turnosActivos?: number;
  turnosPorNombre?: Record<string, unknown>;
}

export interface TurnoTrabajoRequestDTO {
  activo?: boolean;
  diasSemana: Array<string>;
  horaFin: string;
  horaInicio: string;
  nombreTurno: string;
  sedeId?: string;
  toleranciaMinutos?: number;
  userId: string;
}

export interface TurnoTrabajoResponseDTO {
  activo?: boolean;
  createdAt?: string;
  diasSemana?: Array<string>;
  duracionHoras?: number;
  empleadoNombre?: string;
  horaFin?: string;
  horaInicio?: string;
  horasSemanales?: number;
  id?: string;
  nocturno?: boolean;
  nombreTurno?: string;
  sedeId?: string;
  sedeNombre?: string;
  toleranciaMinutos?: number;
  userId?: string;
}

export interface User {
  activo?: boolean;
  apellido?: string;
  celular?: string;
  createdAt?: string;
  email?: string;
  id?: string;
  nombre?: string;
  nombreUsuario?: string;
  passwordHash?: string;
  rol?: Rol;
  sede?: Sede;
  totpEnabled?: boolean;
  totpSecret?: string;
  ultimoLogin?: string;
}

export interface UserInfoDTO {
  apellido?: string;
  email?: string;
  id?: string;
  nombre?: string;
  nombreCompleto?: string;
}

export interface UserRequestDTO {
  activo?: boolean;
  apellido: string;
  celular: string;
  email: string;
  nombre: string;
  nombreUsuario: string;
  passwordHash: string;
  rol: Rol;
  sede: Sede;
}

export interface UserResponseDTO {
  /** Añadido en el DTO de respuesta. Requiere regenerar el cliente OpenAPI. */
  id?: string;
  activo?: boolean;
  apellido?: string;
  celular?: string;
  createdAt?: string;
  email?: string;
  nombre?: string;
  nombreUsuario?: string;
  rol?: Rol;
  sede?: Sede;
  ultimoLogin?: string;
}

export interface UsoMovilidadDTO {
  activo?: boolean;
  conductorNombre?: string;
  despachos?: number;
  entregados?: number;
  fallidos?: number;
  intentosFallidos?: number;
  movilidadId?: string;
  pendientes?: number;
  placa?: string;
  tasaExitoPorcentaje?: number;
  tipo?: string;
}

export interface UtilidadNetaResponseDTO {
  cantidadCompras?: number;
  cantidadDevoluciones?: number;
  cantidadNominasPagadas?: number;
  cantidadVentas?: number;
  comprasNetas?: number;
  comprasOtraMonedaExcluidas?: number;
  comprasTotales?: number;
  desde?: string;
  devolucionesNetas?: number;
  devolucionesTotales?: number;
  gastosOperativos?: number;
  hasta?: string;
  igvVentas?: number;
  margenNetoPorcentaje?: number;
  retirosSocios?: number;
  saldoTrasRetiros?: number;
  sedeId?: string;
  utilidadBruta?: number;
  utilidadNetaReal?: number;
  utilidadOperativa?: number;
  gastosEmpresariales?: number;
  cantidadGastosEmpresariales?: number;
  valorMermas?: number;
  ventasNetas?: number;
  ventasNetasReales?: number;
  ventasTotales?: number;
}

export interface UtilidadSedeDTO {
  sedeId?: string;
  sedeNombre?: string;
  utilidad?: UtilidadNetaResponseDTO;
}

export interface ValorizacionKardexDTO {
  almacenId?: string;
  almacenNombre?: string;
  calculadoEn?: string;
  detalle?: Array<ValorizacionProductoDTO>;
  productos?: number;
  productosSinCosto?: number;
  sedeId?: string;
  valorTotal?: number;
}

export interface ValorizacionProductoDTO {
  almacenes?: number;
  cantidad?: number;
  codigoSku?: string;
  costoUnitario?: number;
  porcentajeDelTotal?: number;
  productoId?: string;
  productoNombre?: string;
  unidad?: string;
  valorTotal?: number;
  precioVenta?: number;
  gananciaEstimada?: number;
}

export interface VentaDetalleResponseDTO {
  canalVenta?: "LOCAL" | "ONLINE";
  clienteId?: string;
  clienteNombre?: string;
  createdAt?: string;
  estado?: "COMPLETADA" | "ANULADA";
  estadoSunat?: "PENDIENTE" | "ACEPTADO" | "RECHAZADO" | "ANULADO";
  id?: string;
  lineas?: Array<LineaVentaResponseDTO>;
  pagos?: Array<PagoVentaDetalleResponseDTO>;
  sedeId?: string;
  serieCorrelativo?: string;
  subtotalNeto?: number;
  tipoComprobante?: "BOLETA" | "FACTURA" | "TICKET";
  total?: number;
  totalIgv?: number;
  turnoCajaId?: string;
  usuarioId?: string;
  usuarioNombre?: string;
}

export interface VentaListadoResponseDTO {
  canalVenta?: "LOCAL" | "ONLINE";
  clienteId?: string;
  clienteNombre?: string;
  createdAt?: string;
  estado?: "COMPLETADA" | "ANULADA";
  id?: string;
  serieCorrelativo?: string;
  tipoComprobante?: "BOLETA" | "FACTURA" | "TICKET";
  total?: number;
  usuarioId?: string;
}

export interface VentaProcesadaResponseDTO {
  estado?: string;
  id?: string;
  serieCorrelativo?: string;
  subtotalNeto?: number;
  total?: number;
  totalIgv?: number;
}

export interface VentaReporteDTO {
  canal?: "LOCAL" | "ONLINE";
  cliente?: string;
  clienteDocumento?: string;
  estado?: "COMPLETADA" | "ANULADA";
  fecha?: string;
  id?: string;
  igv?: number;
  metodosPago?: string;
  neto?: number;
  sede?: string;
  serieCorrelativo?: string;
  tipoComprobante?: "BOLETA" | "FACTURA" | "TICKET";
  total?: number;
  vendedor?: string;
}

export interface VentasResumenDTO {
  advertencias?: Array<string>;
  agrupacion?: "DIA" | "SEMANA" | "MES";
  cantidad?: number;
  cantidadDevoluciones?: number;
  desde?: string;
  devoluciones?: number;
  estado?: "COMPLETADA" | "ANULADA";
  hasta?: string;
  igv?: number;
  neto?: number;
  periodos?: Array<PeriodoVentasDTO>;
  porMetodoPago?: Array<MetodoPagoMontoDTO>;
  sedeId?: string;
  ticketPromedio?: number;
  total?: number;
  totalMenosDevoluciones?: number;
}

