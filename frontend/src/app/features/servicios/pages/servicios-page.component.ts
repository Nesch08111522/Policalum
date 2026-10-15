import { Component } from '@angular/core';
import { WorkspacePageComponent } from '../../workspace/workspace-page.component';

@Component({
  selector: 'app-servicios-page',
  standalone: true,
  imports: [WorkspacePageComponent],
  templateUrl: './servicios-page.component.html',
  styleUrls: ['./servicios-page.component.css'],
})
export class ServiciosPageComponent {
  readonly moduleId = 'servicios';
}
