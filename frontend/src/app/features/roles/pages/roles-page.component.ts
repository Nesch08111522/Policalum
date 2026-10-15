import { Component } from '@angular/core';
import { WorkspacePageComponent } from '../../workspace/workspace-page.component';

@Component({
  selector: 'app-roles-page',
  standalone: true,
  imports: [WorkspacePageComponent],
  templateUrl: './roles-page.component.html',
  styleUrls: ['./roles-page.component.css'],
})
export class RolesPageComponent {
  readonly moduleId = 'roles';
}
