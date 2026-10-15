import { Component } from '@angular/core';
import { WorkspacePageComponent } from '../../workspace/workspace-page.component';

@Component({
  selector: 'app-configuracion-page',
  standalone: true,
  imports: [WorkspacePageComponent],
  templateUrl: './configuracion-page.component.html',
  styleUrls: ['./configuracion-page.component.css'],
})
export class ConfiguracionPageComponent {
  readonly moduleId = 'configuracion';
}
