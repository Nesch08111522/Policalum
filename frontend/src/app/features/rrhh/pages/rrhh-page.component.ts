import { Component } from '@angular/core';
import { WorkspacePageComponent } from '../../workspace/workspace-page.component';

@Component({
  selector: 'app-rrhh-page',
  standalone: true,
  imports: [WorkspacePageComponent],
  templateUrl: './rrhh-page.component.html',
  styleUrls: ['./rrhh-page.component.css'],
})
export class RrhhPageComponent {
  readonly moduleId = 'rrhh';
}
