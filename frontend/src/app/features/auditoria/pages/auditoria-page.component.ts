import { Component } from '@angular/core';
import { WorkspacePageComponent } from '../../workspace/workspace-page.component';

@Component({
  selector: 'app-auditoria-page',
  standalone: true,
  imports: [WorkspacePageComponent],
  templateUrl: './auditoria-page.component.html',
  styleUrls: ['./auditoria-page.component.css'],
})
export class AuditoriaPageComponent {
  readonly moduleId = 'auditoria';
}
