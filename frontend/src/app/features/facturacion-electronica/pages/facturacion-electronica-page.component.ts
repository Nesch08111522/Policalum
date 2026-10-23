import { Component } from '@angular/core';
import { Routes } from '@angular/router';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { FacturacionSunatComponent } from '../components/facturacion-sunat.component';

@Component({
  selector: 'app-facturacion-electronica-page',
  standalone: true,
  imports: [PageHeaderComponent, FacturacionSunatComponent],
  template: `
    <section class="module-page">
      <app-page-header title="Facturación electrónica SUNAT" subtitle="Comprobantes locales de la empresa con RUC 20. La emisión externa espera la API."></app-page-header>
      <app-facturacion-sunat></app-facturacion-sunat>
    </section>
  `,
  styles: [':host { display: block; min-width: 0; }'],
})
export class FacturacionElectronicaPageComponent {}

export const FACTURACION_ELECTRONICA_ROUTES: Routes = [
  { path: '', component: FacturacionElectronicaPageComponent, title: 'Facturación SUNAT', data: { section: 'Finanzas', breadcrumb: 'Facturación SUNAT' } },
];
