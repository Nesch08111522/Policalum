import { Component } from '@angular/core';
import { WorkspacePageComponent } from '../../workspace/workspace-page.component';

@Component({
  selector: 'app-clientes-page',
  standalone: true,
  imports: [WorkspacePageComponent],
  templateUrl: './clientes-page.component.html',
  styleUrls: ['./clientes-page.component.css'],
})
export class ClientesPageComponent {
  readonly moduleId = 'clientes';
}
