// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { AsistenciaComponent } from './components/asistencia/asistencia';
import { LoginComponent } from './components/login/login.component';
import { AuthGuard } from './guards/auth.guard';
import { RoleGuard } from './guards/role.guard';
import { AuthenticatedLayoutComponent } from './layout/authenticated-layout.component';
import { DashboardRedirectComponent } from './shared/components/dashboard-redirect/dashboard-redirect';
import { PosComponent } from './features/ventas/pos.component';
import { WORKSPACE_ROUTES } from './features/workspace/workspace.routes';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: AuthenticatedLayoutComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', component: DashboardRedirectComponent },
      { path: 'admin', redirectTo: 'inicio', pathMatch: 'full' },
      { path: 'empleado', redirectTo: 'inicio', pathMatch: 'full' },
      {
        path: 'asistencias', // ✅ Ruta simplificada
        component: AsistenciaComponent,
        canActivate: [RoleGuard],
        data: { expectedRoles: ['ROLE_ADMIN', 'ROLE_MULTIFUNCIONAL'] },
      },
      {
        path: 'cuenta/perfil',
        loadChildren: () => import('./features/perfil/pages/perfil-page.component').then((m) => m.PERFIL_ROUTES),
      },
      {
        path: 'ventas/pos',
        component: PosComponent,
        canActivate: [RoleGuard],
        data: { expectedRoles: ['ROLE_ADMIN', 'ROLE_MULTIFUNCIONAL'] },
      },
      { path: 'workspace/:module', redirectTo: ({ params }) => `/modules/${params['module']}` },
      ...WORKSPACE_ROUTES,
      { path: 'dashboard', redirectTo: 'inicio', pathMatch: 'full' },
      { path: 'ventas/cotizaciones', redirectTo: 'modules/cotizaciones', pathMatch: 'full' },
      { path: 'ventas/clientes', redirectTo: 'modules/clientes', pathMatch: 'full' },
      { path: 'inventario/productos', redirectTo: 'modules/productos', pathMatch: 'full' },
      { path: 'inventario/servicios', redirectTo: 'modules/servicios', pathMatch: 'full' },
      { path: 'inventario/stock', redirectTo: 'modules/stock', pathMatch: 'full' },
      { path: 'inventario/kardex', redirectTo: 'modules/kardex', pathMatch: 'full' },
      { path: 'compras/ordenes', redirectTo: 'modules/compras', pathMatch: 'full' },
      { path: 'compras/proveedores', redirectTo: 'modules/proveedores', pathMatch: 'full' },
      { path: 'compras/despachos', redirectTo: 'modules/despachos', pathMatch: 'full' },
      { path: 'compras/movilidades', redirectTo: 'modules/movilidades', pathMatch: 'full' },
      { path: 'finanzas/turnos', redirectTo: 'modules/turnos-caja', pathMatch: 'full' },
      { path: 'finanzas/cuentas-pagar', redirectTo: 'modules/cuentas-por-pagar', pathMatch: 'full' },
      { path: 'finanzas/pagos', redirectTo: 'modules/finanzas', pathMatch: 'full' },
      { path: 'finanzas/retiro-socios', redirectTo: 'modules/finanzas', pathMatch: 'full' },
      { path: 'rrhh/legajo', redirectTo: 'modules/rrhh', pathMatch: 'full' },
      { path: 'rrhh/nominas', redirectTo: 'modules/rrhh', pathMatch: 'full' },
      { path: 'reportes/ventas', redirectTo: 'modules/reportes', pathMatch: 'full' },
      { path: 'reportes/financieros', redirectTo: 'modules/finanzas', pathMatch: 'full' },
      { path: 'config/sedes', redirectTo: 'modules/sedes', pathMatch: 'full' },
      { path: 'config/usuarios', redirectTo: 'modules/users', pathMatch: 'full' },
      { path: 'config/parametros', redirectTo: 'modules/configuracion', pathMatch: 'full' },
      { path: 'config/auditoria', redirectTo: 'modules/auditoria', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: '/login' },
];
