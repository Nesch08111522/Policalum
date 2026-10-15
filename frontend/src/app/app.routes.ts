// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { AdminDashboardComponent } from './components/admin/admin-dashboard.component';
import { AsistenciaComponent } from './components/asistencia/asistencia'; // ✅ Correcto
import { EmpleadoDashboardComponent } from './components/empleado/empleado-dashboard.component';
import { LoginComponent } from './components/login/login.component';
import { AuthGuard } from './guards/auth.guard';
import { RoleGuard } from './guards/role.guard';
import { AuthenticatedLayoutComponent } from './layout/authenticated-layout.component';
import { DashboardRedirectComponent } from './shared/components/dashboard-redirect/dashboard-redirect';
import { PosComponent } from './features/ventas/pos.component';
import { WorkspacePageComponent } from './features/workspace/workspace-page.component';
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
      {
        path: 'admin',
        component: AdminDashboardComponent,
        canActivate: [RoleGuard],
        data: { expectedRoles: ['ROLE_ADMIN'] },
      },
      {
        path: 'empleado',
        component: EmpleadoDashboardComponent,
        canActivate: [RoleGuard],
        data: { expectedRoles: ['ROLE_MULTIFUNCIONAL', 'ROLE_ADMIN'] },
      },
      {
        path: 'asistencias', // ✅ Ruta simplificada
        component: AsistenciaComponent,
        canActivate: [RoleGuard],
        data: { expectedRoles: ['ROLE_ADMIN', 'ROLE_MULTIFUNCIONAL'] },
      },
      {
        path: 'ventas/pos',
        component: PosComponent,
        canActivate: [RoleGuard],
        data: { expectedRoles: ['ROLE_ADMIN', 'ROLE_MULTIFUNCIONAL'] },
      },
      {
        path: 'workspace/:module',
        component: WorkspacePageComponent,
        canActivate: [RoleGuard],
        data: { expectedRoles: ['ROLE_ADMIN', 'ROLE_MULTIFUNCIONAL'] },
      },
      ...WORKSPACE_ROUTES,
      { path: 'dashboard', redirectTo: 'workspace/ventas', pathMatch: 'full' },
      { path: 'ventas/cotizaciones', redirectTo: 'workspace/cotizaciones', pathMatch: 'full' },
      { path: 'ventas/clientes', redirectTo: 'workspace/clientes', pathMatch: 'full' },
      { path: 'inventario/productos', redirectTo: 'workspace/productos', pathMatch: 'full' },
      { path: 'inventario/servicios', redirectTo: 'workspace/servicios', pathMatch: 'full' },
      { path: 'inventario/stock', redirectTo: 'workspace/stock', pathMatch: 'full' },
      { path: 'inventario/kardex', redirectTo: 'workspace/kardex', pathMatch: 'full' },
      { path: 'compras/ordenes', redirectTo: 'workspace/compras', pathMatch: 'full' },
      { path: 'compras/proveedores', redirectTo: 'workspace/proveedores', pathMatch: 'full' },
      { path: 'compras/despachos', redirectTo: 'workspace/despachos', pathMatch: 'full' },
      { path: 'compras/movilidades', redirectTo: 'workspace/movilidades', pathMatch: 'full' },
      { path: 'finanzas/turnos', redirectTo: 'workspace/turnos-caja', pathMatch: 'full' },
      { path: 'finanzas/cuentas-pagar', redirectTo: 'workspace/cuentas-por-pagar', pathMatch: 'full' },
      { path: 'finanzas/pagos', redirectTo: 'workspace/finanzas', pathMatch: 'full' },
      { path: 'finanzas/retiro-socios', redirectTo: 'workspace/finanzas', pathMatch: 'full' },
      { path: 'rrhh/legajo', redirectTo: 'workspace/rrhh', pathMatch: 'full' },
      { path: 'rrhh/nominas', redirectTo: 'workspace/rrhh', pathMatch: 'full' },
      { path: 'reportes/ventas', redirectTo: 'workspace/reportes', pathMatch: 'full' },
      { path: 'reportes/financieros', redirectTo: 'workspace/finanzas', pathMatch: 'full' },
      { path: 'config/sedes', redirectTo: 'workspace/sedes', pathMatch: 'full' },
      { path: 'config/usuarios', redirectTo: 'workspace/users', pathMatch: 'full' },
      { path: 'config/parametros', redirectTo: 'workspace/configuracion', pathMatch: 'full' },
      { path: 'config/auditoria', redirectTo: 'workspace/auditoria', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: '/login' },
];
