export interface FraccionPublica {
  id: string;
  nombre: string;
  factor: number;
  precio: number;
  precioLista?: number;
}

export interface ProductoPublico {
  id: string;
  nombre: string;
  descripcion?: string;
  codigoSku?: string;
  unidad?: string;
  precioPublico: number;
  precioLista?: number;
  disponible: boolean;
  destacado: boolean;
  esFraccionable: boolean;
  categoriaId?: string;
  categoriaNombre?: string;
  imagenUrl?: string;
  oferta?: boolean;
  stock?: number;
  fracciones: FraccionPublica[];
}

export interface ServicioPublico {
  id: string;
  nombre: string;
  descripcion?: string;
  precio: number;
}

export interface CategoriaPublica {
  id: string;
  nombre: string;
  slug: string;
}

export interface SedePublica {
  id: string;
  nombre: string;
  ciudad?: string;
  direccion?: string;
  telefono?: string;
}

export interface PaginaPublica<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  enCache?: boolean;
}

export interface EmpresaPublica {
  nombreComercial: string;
  direccion?: string;
  telefono?: string;
  email?: string;
  whatsapp?: string;
  logoUrl?: string;
  ruc?: string;
  razonSocial?: string;
}

export interface ProyectoPublico {
  id: string;
  titulo: string;
  descripcion: string;
  imagenUrl?: string;
  orden: number;
  cliente?: string;
}

export interface LineaCarrito {
  productoId: string;
  nombre: string;
  fraccionId?: string;
  fraccionNombre?: string;
  precio: number;
  cantidad: number;
  imagenUrl?: string;
  unidad: string;
}

export interface LineaCotizacion {
  productoNombre?: string;
  fraccionNombre?: string;
  servicioNombre?: string;
  cantidad: number;
  precioUnitarioConIgv: number;
  subtotal: number;
}

export interface CargosCotizacion {
  distrito?: string;
  cantidadPiezas?: number;
  subtotalMateriales?: number;
  utilidadPorcentaje?: number;
  utilidadMonto?: number;
  movilidadMonto?: number;
  otrosMonto?: number;
  otrosDetalle?: string;
  modificada?: boolean;
  notaCambio?: string;
}

export interface FormatoCotizacion {
  utilidadPorcentaje: number;
  distritos: { distrito: string; precioBase: number; unidadesIncluidas: number; precioUnidadExtra: number; activo?: boolean }[];
  conceptos: { nombre: string; tipo: 'MONTO' | 'PORCENTAJE'; valor: number; activo?: boolean }[];
}

export interface VistaCotizacion {
  total: number;
  cargos: CargosCotizacion;
  lineas: LineaCotizacion[];
}

export interface CotizacionCreada {
  id: string;
  clienteNombre?: string;
  estado: string;
  origen?: string;
  total: number;
  fechaVigencia?: string;
  lineas: LineaCotizacion[];
  cargos?: CargosCotizacion;
}

export interface ContactoCreado {
  id: string;
  estado: string;
}
