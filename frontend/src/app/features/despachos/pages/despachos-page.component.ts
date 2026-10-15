import { Component } from '@angular/core';
import { WorkspacePageComponent } from '../../workspace/workspace-page.component';

@Component({
  selector: 'app-despachos-page',
  standalone: true,
  imports: [WorkspacePageComponent],
  templateUrl: './despachos-page.component.html',
  styleUrls: ['./despachos-page.component.css'],
})
export class DespachosPageComponent {
  readonly moduleId = 'despachos';
}
