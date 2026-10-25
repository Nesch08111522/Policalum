export interface NavItem {
  id: string;
  label: string;
  path: string;
  icon: string;
  roles?: string[];
}

export interface NavSection {
  id: string;
  label: string;
  items: NavItem[];
}

const admin = ['ROLE_ADMIN'];

export const NAV_SECTIONS: NavSection[] = [
  { id: 'inicio', label: 'Inicio', items: [
    { id: 'dashboard', label: 'Panel', path: '/inicio', icon: 'home' },
    { id: 'notificaciones', label: 'Notificaciones', path: '/modules/notificaciones', icon: 'bell' },
  ] },
  {
    id: 'ventas',
    label: 'Ventas',
    items: [
      { id: 'ventas', label: 'Ventas', path: '/modules/ventas', icon: 'cart' },
      { id: 'pos', label: 'Punto de venta', path: '/ventas/pos', icon: 'cash' },
      { id: 'clientes', label: 'Clientes', path: '/modules/clientes', icon: 'users' },
      { id: 'cotizaciones', label: 'Cotizaciones', path: '/modules/cotizaciones', icon: 'file' },
      { id: 'preformas', label: 'Preformas', path: '/modules/preformas', icon: 'file' },
      { id: 'devoluciones', label: 'Devoluciones', path: '/modules/devoluciones', icon: 'refresh' },
      { id: 'reembolsos', label: 'Reembolsos', path: '/modules/reembolsos', icon: 'cash' },
      { id: 'turnos-caja', label: 'Caja', path: '/modules/turnos-caja', icon: 'cash' },
    ],
  },
  {
    id: 'inventario',
    label: 'Inventario',
    items: [
      { id: 'productos', label: 'Productos', path: '/modules/productos', icon: 'box' },
      { id: 'servicios', label: 'Servicios', path: '/modules/servicios', icon: 'wrench' },
      { id: 'categorias', label: 'Categorías', path: '/modules/categorias', icon: 'folder' },
      { id: 'catalogo-fracciones', label: 'Fracciones', path: '/modules/catalogo-fracciones', icon: 'grid' },
      { id: 'stock', label: 'Stock', path: '/modules/stock', icon: 'layers' },
      { id: 'kardex', label: 'Kardex', path: '/modules/kardex', icon: 'chart' },
      { id: 'almacenes', label: 'Almacenes', path: '/modules/almacenes', icon: 'warehouse' },
    ],
  },
  {
    id: 'compras',
    label: 'Compras',
    items: [
      { id: 'compras', label: 'Compras', path: '/modules/compras', icon: 'truck' },
      { id: 'proveedores', label: 'Proveedores', path: '/modules/proveedores', icon: 'building' },
      { id: 'despachos', label: 'Despachos', path: '/modules/despachos', icon: 'pin' },
      { id: 'movilidades', label: 'Movilidades', path: '/modules/movilidades', icon: 'car' },
    ],
  },
  {
    id: 'finanzas',
    label: 'Finanzas',
    items: [
      { id: 'cuentas-por-pagar', label: 'Cuentas por pagar', path: '/modules/cuentas-por-pagar', icon: 'card' },
      { id: 'facturacion-electronica', label: 'Facturación SUNAT', path: '/modules/facturacion-electronica', icon: 'file' },
      { id: 'finanzas', label: 'Finanzas', path: '/modules/finanzas', icon: 'chart', roles: admin },
    ],
  },
  {
    id: 'rrhh',
    label: 'Recursos humanos',
    items: [
      { id: 'rrhh', label: 'Personas', path: '/modules/rrhh', icon: 'people' },
      { id: 'asistencias', label: 'Asistencias', path: '/asistencias', icon: 'check', roles: ['ROLE_ADMIN', 'ROLE_MULTIFUNCIONAL'] },
    ],
  },
  {
    id: 'analisis',
    label: 'Análisis',
    items: [
      { id: 'reportes', label: 'Reportes', path: '/modules/reportes', icon: 'report' },
      { id: 'auditoria', label: 'Auditoría', path: '/modules/auditoria', icon: 'search', roles: admin },
    ],
  },
  {
    id: 'config',
    label: 'Configuración',
    items: [
      { id: 'perfil', label: 'Mi perfil', path: '/cuenta/perfil', icon: 'users' },
      { id: 'users', label: 'Usuarios', path: '/modules/users', icon: 'shield', roles: admin },
      { id: 'roles', label: 'Roles', path: '/modules/roles', icon: 'shield', roles: admin },
      { id: 'sedes', label: 'Sedes', path: '/modules/sedes', icon: 'map', roles: admin },
      { id: 'configuracion', label: 'Configuración', path: '/modules/configuracion', icon: 'settings', roles: admin },
    ],
  },
];

export const QUICK_LINKS = [
  { label: 'Panel', path: '/inicio' },
  { label: 'Punto de venta', path: '/ventas/pos' },
  { label: 'Stock', path: '/modules/stock' },
  { label: 'Ventas', path: '/modules/ventas' },
  { label: 'Asistencia', path: '/asistencias' },
];
