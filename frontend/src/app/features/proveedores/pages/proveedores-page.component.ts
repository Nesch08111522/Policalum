import { Component } from '@angular/core';
import { WorkspacePageComponent } from '../../workspace/workspace-page.component';

@Component({
  selector: 'app-proveedores-page',
  standalone: true,
  imports: [WorkspacePageComponent],
  templateUrl: './proveedores-page.component.html',
  styleUrls: ['./proveedores-page.component.css'],
})
export class ProveedoresPageComponent {
  readonly moduleId = 'proveedores';
}
