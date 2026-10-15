import { Component } from '@angular/core';
import { WorkspacePageComponent } from '../../workspace/workspace-page.component';

@Component({
  selector: 'app-ventas-page',
  standalone: true,
  imports: [WorkspacePageComponent],
  templateUrl: './ventas-page.component.html',
  styleUrls: ['./ventas-page.component.css'],
})
export class VentasPageComponent {
  readonly moduleId = 'ventas';
}
