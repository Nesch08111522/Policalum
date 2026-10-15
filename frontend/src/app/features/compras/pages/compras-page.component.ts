import { Component } from '@angular/core';
import { WorkspacePageComponent } from '../../workspace/workspace-page.component';

@Component({
  selector: 'app-compras-page',
  standalone: true,
  imports: [WorkspacePageComponent],
  templateUrl: './compras-page.component.html',
  styleUrls: ['./compras-page.component.css'],
})
export class ComprasPageComponent {
  readonly moduleId = 'compras';
}
