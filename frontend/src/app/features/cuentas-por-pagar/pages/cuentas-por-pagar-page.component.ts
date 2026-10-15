import { Component } from '@angular/core';
import { WorkspacePageComponent } from '../../workspace/workspace-page.component';

@Component({
  selector: 'app-cuentas-por-pagar-page',
  standalone: true,
  imports: [WorkspacePageComponent],
  templateUrl: './cuentas-por-pagar-page.component.html',
  styleUrls: ['./cuentas-por-pagar-page.component.css'],
})
export class CuentasPorPagarPageComponent {
  readonly moduleId = 'cuentas-por-pagar';
}
