import { Component } from '@angular/core';
import { WorkspacePageComponent } from '../../workspace/workspace-page.component';

@Component({
  selector: 'app-cotizaciones-page',
  standalone: true,
  imports: [WorkspacePageComponent],
  templateUrl: './cotizaciones-page.component.html',
  styleUrls: ['./cotizaciones-page.component.css'],
})
export class CotizacionesPageComponent {
  readonly moduleId = 'cotizaciones';
}
