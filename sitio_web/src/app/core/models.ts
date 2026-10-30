export interface FraccionPublica {
  id: string;
  nombre: string;
  factor: number;
  precio: number;
}

export interface ProductoPublico {
  id: string;
  nombre: string;
  descripcion?: string;
  codigoSku?: string;
  unidad?: string;
  precioPublico: number;
  disponible: boolean;
  destacado: boolean;
  esFraccionable: boolean;
  categoriaId?: string;
  categoriaNombre?: string;
  imagenUrl?: string;
  fracciones: FraccionPublica[];
}

export interface CategoriaPublica {
  id: string;
  nombre: string;
  slug: string;
}

export interface PaginaPublica<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface EmpresaPublica {
  nombreComercial: string;
  direccion?: string;
  telefono?: string;
  email?: string;
  whatsapp?: string;
  logoUrl?: string;
}

export interface ProyectoPublico {
  id: string;
  titulo: string;
  descripcion: string;
  imagenUrl?: string;
  orden: number;
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

export interface CotizacionCreada {
  id: string;
  clienteNombre?: string;
  estado: string;
  origen?: string;
  total: number;
  fechaVigencia?: string;
  lineas: LineaCotizacion[];
}

export interface ContactoCreado {
  id: string;
  estado: string;
}
