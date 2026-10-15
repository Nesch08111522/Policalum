import { ENDPOINTS, EndpointDefinition } from '../../core/api/api-endpoints.generated';

export interface WorkspaceModule {
  id: string;
  label: string;
  description: string;
  icon: string;
  roles?: string[];
}

export const WORKSPACE_MODULES: WorkspaceModule[] = [
  { id: 'ventas', label: 'Ventas', description: 'Ventas, POS, anulaciones y comprobantes.', icon: '🛒' },
  { id: 'clientes', label: 'Clientes', description: 'CRM, clientes frecuentes y validaciones.', icon: '👥' },
  { id: 'cotizaciones', label: 'Cotizaciones', description: 'Cotizaciones y conversión a venta.', icon: '🧾' },
  { id: 'productos', label: 'Productos', description: 'Catálogo, precios, SKU y fracciones.', icon: '📦' },
  { id: 'servicios', label: 'Servicios', description: 'Servicios facturables y precios.', icon: '🛠️' },
  { id: 'categorias', label: 'Categorías', description: 'Árbol de categorías del catálogo.', icon: '🗂️' },
  { id: 'catalogo-fracciones', label: 'Fracciones', description: 'Unidades y precios por fracción.', icon: '◫' },
  { id: 'stock', label: 'Stock', description: 'Stock, ajustes, transferencias y mínimos.', icon: '🏷️' },
  { id: 'kardex', label: 'Kardex', description: 'Movimientos, saldos y valorización.', icon: '📊' },
  { id: 'almacenes', label: 'Almacenes', description: 'Almacenes y existencias por sede.', icon: '🏭' },
  { id: 'compras', label: 'Compras', description: 'Compras, recepción, adjuntos y anulaciones.', icon: '🚚' },
  { id: 'proveedores', label: 'Proveedores', description: 'Maestro y validación de proveedores.', icon: '🏢' },
  { id: 'despachos', label: 'Despachos', description: 'Entregas, estados y evidencias.', icon: '📍' },
  { id: 'movilidades', label: 'Movilidades', description: 'Vehículos, conductores e historial.', icon: '🚗' },
  { id: 'turnos-caja', label: 'Caja', description: 'Apertura, cierre y arqueo de caja.', icon: '💵' },
  { id: 'cuentas-por-pagar', label: 'Cuentas por pagar', description: 'Deudas, vencimientos y pagos.', icon: '💳' },
  { id: 'finanzas', label: 'Finanzas', description: 'Cuentas, saldos, flujo y retiros.', icon: '📈', roles: ['ROLE_ADMIN'] },
  { id: 'rrhh', label: 'Recursos humanos', description: 'Asistencia, legajos, turnos, nóminas y comisiones.', icon: '🧑‍💼' },
  { id: 'reportes', label: 'Reportes', description: 'Ventas, rentabilidad, caja y rankings.', icon: '📑' },
  { id: 'auditoria', label: 'Auditoría', description: 'Logs, trazabilidad y exportaciones.', icon: '🔎', roles: ['ROLE_ADMIN'] },
  { id: 'configuracion', label: 'Configuración', description: 'Empresa, parámetros y series.', icon: '⚙️', roles: ['ROLE_ADMIN'] },
  { id: 'users', label: 'Usuarios', description: 'Usuarios y roles de acceso.', icon: '🔐', roles: ['ROLE_ADMIN'] },
  { id: 'roles', label: 'Roles', description: 'Roles disponibles del sistema.', icon: '🛡️', roles: ['ROLE_ADMIN'] },
  { id: 'sedes', label: 'Sedes', description: 'Sedes y estados operativos.', icon: '📍', roles: ['ROLE_ADMIN'] },
];

export function endpointsForModule(moduleId: string): EndpointDefinition[] {
  return ENDPOINTS.filter((endpoint) => {
    if (moduleId === 'rrhh') return endpoint.path.startsWith('/api/v1/rrhh/');
    if (moduleId === 'finanzas') return endpoint.path.startsWith('/api/v1/finanzas/');
    if (moduleId === 'reportes') return endpoint.path.startsWith('/api/v1/reportes/');
    return endpoint.module === moduleId;
  });
}

export function moduleById(moduleId: string): WorkspaceModule {
  return WORKSPACE_MODULES.find((item) => item.id === moduleId) ?? {
    id: moduleId,
    label: moduleId,
    description: 'Operaciones disponibles del backend.',
    icon: '▦',
  };
}
