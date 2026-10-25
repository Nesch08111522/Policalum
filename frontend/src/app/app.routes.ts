import { Routes } from '@angular/router';
import { AsistenciaComponent } from './components/asistencia/asistencia';
import { LoginComponent } from './components/login/login.component';
import { PosComponent } from './features/ventas/pos.component';
import { ErrorPageComponent } from './features/errores/error-page.component';
import { AuthGuard } from './guards/auth.guard';
import { RoleGuard } from './guards/role.guard';
import { AuthenticatedLayoutComponent } from './layout/authenticated-layout.component';
import { MODULE_ROUTES } from './module.routes';

const operacion = ['ROLE_ADMIN', 'ROLE_MULTIFUNCIONAL'];

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: '/login' },
  { path: 'login', component: LoginComponent },
  { path: 'recuperar-password', redirectTo: '/login', pathMatch: 'full' },
  {
    path: '',
    component: AuthenticatedLayoutComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      { path: 'admin', pathMatch: 'full', redirectTo: 'inicio' },
      { path: 'empleado', pathMatch: 'full', redirectTo: 'inicio' },
      { path: 'dashboard', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'asistencias',
        component: AsistenciaComponent,
        canActivate: [RoleGuard],
        data: { expectedRoles: operacion },
      },
      {
        path: 'cuenta/perfil',
        loadChildren: () => import('./features/perfil/pages/perfil-page.component').then((m) => m.PERFIL_ROUTES),
      },
      {
        path: 'ventas/pos',
        component: PosComponent,
        canActivate: [RoleGuard],
        data: { expectedRoles: operacion },
      },
      { path: 'workspace/:module', redirectTo: ({ params }) => `/modules/${params['module']}` },
      ...MODULE_ROUTES,
      { path: 'ventas/cotizaciones', pathMatch: 'full', redirectTo: 'modules/cotizaciones' },
      { path: 'ventas/clientes', pathMatch: 'full', redirectTo: 'modules/clientes' },
      { path: 'inventario/productos', pathMatch: 'full', redirectTo: 'modules/productos' },
      { path: 'inventario/servicios', pathMatch: 'full', redirectTo: 'modules/servicios' },
      { path: 'inventario/stock', pathMatch: 'full', redirectTo: 'modules/stock' },
      { path: 'inventario/kardex', pathMatch: 'full', redirectTo: 'modules/kardex' },
      { path: 'compras/ordenes', pathMatch: 'full', redirectTo: 'modules/compras' },
      { path: 'compras/proveedores', pathMatch: 'full', redirectTo: 'modules/proveedores' },
      { path: 'compras/despachos', pathMatch: 'full', redirectTo: 'modules/despachos' },
      { path: 'compras/movilidades', pathMatch: 'full', redirectTo: 'modules/movilidades' },
      { path: 'finanzas/turnos', pathMatch: 'full', redirectTo: 'modules/turnos-caja' },
      { path: 'finanzas/cuentas-pagar', pathMatch: 'full', redirectTo: 'modules/cuentas-por-pagar' },
      { path: 'finanzas/pagos', pathMatch: 'full', redirectTo: 'modules/finanzas' },
      { path: 'finanzas/retiro-socios', pathMatch: 'full', redirectTo: 'modules/finanzas' },
      { path: 'rrhh/legajo', pathMatch: 'full', redirectTo: 'modules/rrhh' },
      { path: 'rrhh/nominas', pathMatch: 'full', redirectTo: 'modules/rrhh' },
      { path: 'reportes/ventas', pathMatch: 'full', redirectTo: 'modules/reportes' },
      { path: 'reportes/financieros', pathMatch: 'full', redirectTo: 'modules/finanzas' },
      { path: 'config/sedes', pathMatch: 'full', redirectTo: 'modules/sedes' },
      { path: 'config/usuarios', pathMatch: 'full', redirectTo: 'modules/users' },
      { path: 'config/parametros', pathMatch: 'full', redirectTo: 'modules/configuracion' },
      { path: 'config/auditoria', pathMatch: 'full', redirectTo: 'modules/auditoria' },
    ],
  },
  { path: 'error/404', component: ErrorPageComponent, title: 'Página no encontrada', data: { codigo: '404' } },
  { path: 'error/500', component: ErrorPageComponent, title: 'Error del servidor', data: { codigo: '500' } },
  { path: 'error/permisos', component: ErrorPageComponent, title: 'Error de permisos', data: { codigo: 'permisos' } },
  { path: '**', component: ErrorPageComponent, title: 'Página no encontrada', data: { codigo: '404' } },
];
